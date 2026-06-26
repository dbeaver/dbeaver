/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.jface.text.Document;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.browser.Browser;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.impl.MessageChunk;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.ai.chat.AIChatUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessages;
import org.jkiss.dbeaver.ui.editors.sql.convert.impl.HTMLSQLConverter;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLRuleScanner;
import org.jkiss.dbeaver.utils.DurationFormat;
import org.jkiss.dbeaver.utils.DurationFormatter;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class WebViewMessageRenderer {

    private static final Log log = Log.getLog(WebViewMessageRenderer.class);

    private final Browser browser;
    private final Supplier<DBPDataSource> dataSourceSupplier;
    private SQLSyntaxManager syntaxManager;
    private SQLRuleScanner ruleScanner;

    WebViewMessageRenderer(
        @NotNull Browser browser,
        @NotNull Supplier<DBPDataSource> dataSourceSupplier
    ) {
        this.browser = browser;
        this.dataSourceSupplier = dataSourceSupplier;
    }

    @Nullable
    private DBPDataSource getDataSource() {
        return dataSourceSupplier.get();
    }

    void showConversationMessages(@NotNull AIChatConversation conversation) {
        List<AIChatMessage> messages = conversation.getMessages();
        for (int leftIndex = 0; leftIndex < messages.size(); leftIndex++) {
            AIChatMessage current = messages.get(leftIndex);
            if (current.message().getConfirmation() instanceof AIFunctionCallConfirmation fcc) {
                AIChatMessage confirmationMessage = current;
                Set<UUID> approvedIds = new HashSet<>();
                int rightIndex = leftIndex + 1;
                // check for the next messages
                // if it is a function call, then we need to show them as executed, and collect their ids in the set
                while (rightIndex < messages.size()) {
                    current = messages.get(rightIndex);
                    if (current.message().getFunctionCall() == null) {
                        break;
                    }
                    replayFunctionExecutionStatus(current);
                    addMessage(current);
                    approvedIds.add(current.message().getFunctionCall().getId());
                    rightIndex++;
                }
                // we check all function calls from confirmation, and we show all function calls that are not in messages as declined
                for (AIFunctionCall fc : fcc.getFunctionCalls()) {
                    if (!approvedIds.contains(fc.getId())) {
                        addSavedFunctionStatus(confirmationMessage, fc, false);
                    }
                }
                // we made break when message is not a function, that's why we need to handle the message again
                leftIndex = rightIndex - 1;
            } else {
                replayFunctionExecutionStatus(current);
                addMessage(current);
            }
        }
    }

    boolean addMessage(@NotNull AIChatMessage message) {
        DBPImage icon = AIChatUtils.getRoleIcon(message.message());
        String iconPath = "";
        try {
            iconPath = RuntimeUtils.getPlatformFile(icon.getLocation()).toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("Error getting icon path for " + icon, e);
        }

        Map<String, Object> args = new HashMap<>();
        switch (message.message().getRole()) {
            case ATTACHMENT -> addAttachmentMessage(args, message);
            case FUNCTION -> {
                AIFunctionCall functionCall = message.message().getFunctionCall();
                if (functionCall != null) {
                    AIFunctionDescriptor function = functionCall.getFunction();
                    AIFunctionResult functionResult = message.message().getFunctionResult();
                    boolean hasException = functionResult != null && functionResult.getException() != null;
                    if (function == null || function.getType() == AIFunctionType.INFORMATION || hasException) {
                        return false;
                    }
                }
                addFunctionMessage(args, message);
            }
            default -> addUsualMessage(args, message);
        }

        args.put("id", message.id());
        args.put("role", message.message().getRole().name().toLowerCase(Locale.ROOT));
        args.put("icon", iconPath);
        args.put("meta", buildMessageMeta(message));
        execute("addMessage", args);

        return true;
    }

    @Nullable
    private static Map<String, String> buildMessageMeta(@NotNull AIChatMessage message) {
        var meta = message.message().getMeta();
        if (meta == null) {
            return null;
        }

        AIExtendedUsage extendedUsage = AIExtendedUsage.from(meta);

        return Map.of(
            "time", AIChatUtils.formatMessageTime(message.message()),
            "duration", DurationFormatter.format(extendedUsage.totalTime(), DurationFormat.SHORT),
            "usage", formatUsage(AIExtendedUsage.from(meta))
        );
    }

    @NotNull
    static String formatUsage(@NotNull AIExtendedUsage usage) {
        return NLS.bind(
            AIChatMessages.ai_chat_message_meta_tokens,
            usage.totalInputTokens(),
            usage.totalOutputTokens()
        );
    }

    private void addUsualMessage(@NotNull Map<String, Object> args, @NotNull AIChatMessage message) {
        StringBuilder sb = new StringBuilder();
        String messageContent = message.message().getDisplayMessage();
        MessageChunk[] chunks = AITextUtils.splitIntoChunks(messageContent, false);
        for (MessageChunk chunk : chunks) {
            if (chunk instanceof MessageChunk.Code(String text, String codeBlockTag)) {
                if (codeBlockTag.equals("sql")) {
                    sb.append("\n\n").append(convertSQLToHTML(text)).append("\n");
                } else {
                    sb.append('\n').append(chunk.toRawString()).append('\n');
                }
            } else {
                sb.append(chunk.toRawString());
            }
        }
        args.put("content", sb.toString());
    }

    void addMessageChunk(@NotNull AIChatMessage message, @NotNull String chunk) {
        execute("addMessageChunk", Map.of("id", message.id(), "chunk", chunk));
    }

    private void addFunctionMessage(@NotNull Map<String, Object> args, @NotNull AIChatMessage message) {
        String sb = "<a class='interactive-link' data-message-id='" + message.id() + "'>" + message.message().getDisplayMessage() + "</a>";
        args.put("content", sb);
    }

    private void replayFunctionExecutionStatus(@NotNull AIChatMessage message) {
        if (message.message().getRole() != AIMessageType.FUNCTION) {
            return;
        }

        AIFunctionCall functionCall = message.message().getFunctionCall();
        if (functionCall == null) {
            return;
        }

        addSavedFunctionStatus(message, functionCall, true);
        String result = CommonUtils.notEmpty(message.message().getDisplayMessage());
        Map<String, Object> arguments = functionCall.getArguments();
        AIFunctionDescriptor function = functionCall.getFunction();
        if (!result.isEmpty()) {
            Map<String, Object> resultArgs = new HashMap<>();
            resultArgs.put("messageId", message.id());
            resultArgs.put("functionName", functionCall.getFunctionName());
            resultArgs.put("result", result);
            resultArgs.put("resultLabel", AIChatMessages.ai_chat_confirm_result);
            AIFunctionResult functionResult = message.message().getFunctionResult();
            if (functionResult != null && functionResult.getException() != null) {
                resultArgs.put("hasException", true);
                String toolName = function != null ? function.getName() : functionCall.getFunctionName();
                resultArgs.put("text", NLS.bind(AIChatMessages.ai_chat_confirm_failed, toolName));
                resultArgs.put("paramsLabel", AIChatMessages.ai_chat_confirm_params);
                if (!arguments.isEmpty()) {
                    resultArgs.put("arguments", arguments);
                }
            }
            execute("updateFunctionResult", resultArgs);
        }
    }

    private void addSavedFunctionStatus(@NotNull AIChatMessage message, @NotNull AIFunctionCall functionCall, boolean confirmed) {
        AIFunctionDescriptor function = functionCall.getFunction();
        AIFunctionResult functionResult = message.message().getFunctionResult();
        boolean hasException = functionResult != null && functionResult.getException() != null;
        if (confirmed && function != null && function.getType() == AIFunctionType.ACTION && !hasException) {
            return;
        }
        String toolName = function != null ? function.getName() : functionCall.getFunctionName();
        Map<String, Object> args = new HashMap<>();
        args.put("messageId", message.id());
        args.put(
            "text",
            hasException ? NLS.bind(AIChatMessages.ai_chat_confirm_failed, toolName)
                : NLS.bind(AIChatMessages.ai_chat_confirm_auto_approved, toolName)
        );
        args.put("functionName", functionCall.getFunctionName());
        args.put("confirmed", confirmed);
        args.put("paramsLabel", AIChatMessages.ai_chat_confirm_params);
        args.put("resultLabel", AIChatMessages.ai_chat_confirm_result);
        if (hasException) {
            args.put("hasException", true);
        }

        Map<String, Object> arguments = functionCall.getArguments();
        if (!arguments.isEmpty()) {
            args.put("arguments", arguments);
        }

        execute("addAutoConfirmedStatus", args);
    }

    private void addAttachmentMessage(@NotNull Map<String, Object> args, @NotNull AIChatMessage message) {
        List<Map<String, Object>> attachments = new ArrayList<>();
        if (message.message() instanceof AIMessageFiles attachment) {
            List<Path> files = attachment.getAttachment();
            for (Path file : files) {
                Map<String, Object> attachmentData = new HashMap<>();
                attachmentData.put("path", file.toAbsolutePath().toString());
                attachmentData.put("name", file.getFileName().toString());
                String fileIconPath = null;
                try {
                    fileIconPath = RuntimeUtils.getPlatformFile(attachment.getIcon(file).getLocation()).toAbsolutePath().toString();
                } catch (IOException e) {
                    log.error("Error getting icon path for " + file.getFileName(), e);
                }
                if (fileIconPath != null) {
                    attachmentData.put("icon", fileIconPath);
                }
                attachments.add(attachmentData);
            }
            args.put("canCreateConnection", attachment.canCreateConnection());
            args.put("canImport", attachment.canImportData());
            args.put("isCreateConnectionWasExecuted", attachment.isCreateConnectionWasExecuted());
            args.put("isImportDataWasExecuted", attachment.isImportDataWasExecuted());
        }
        args.put("content", attachments);
    }

    void initChat() {
        Map<String, Object> icons = Map.of(
            "copy",
            getActionData(UIIcon.COPY, AIChatMessages.ai_chat_message_copy_to_clipboard_tip),
            "execute",
            getActionData(UIIcon.SQL_EXECUTE, AIChatMessages.ai_chat_message_execute_query_tip),
            "editor",
            getActionData(UIIcon.MOVE, AIChatMessages.ai_chat_message_copy_to_editor_tip),
            "info",
            getActionData(DBIcon.SMALL_INFO, ""),
            "clean",
            getActionData(UIIcon.CLEAN, AIChatMessages.ai_chat_message_clear_up_to_here_tip),
            "close",
            getActionData(UIIcon.CLOSE, AIChatMessages.ai_chat_message_delete_message_tip),
            "settings",
            Map.of(
                "showMessageTime", AIConstants.AI_CHAT_SHOW_MESSAGE_TIME,
                "showTimeSpent", AIConstants.AI_CHAT_SHOW_TIME_SPENT,
                "showTokensSpent", AIConstants.AI_CHAT_SHOW_TOKENS_SPENT,
                "showTotalTokensSpent", AIConstants.AI_CHAT_SHOW_TOTAL_TOKENS_SPENT
            )
        );

        execute("initChat", icons);
    }

    void updateChatMeta(@Nullable AIChatConversation conversation) {
        if (conversation != null) {
            updateChatMeta(formatUsage(conversation.computeUsage()));
        }
    }

    void updateChatMeta(@NotNull String text) {
        execute("updateChatMeta", text);
    }

    @NotNull
    private Map<String, String> getActionData(@NotNull DBIcon icon, @NotNull String tooltipMessage) {
        Map<String, String> iconData;
        try {
            iconData = Map.of("icon", RuntimeUtils.getPlatformFile(icon.getLocation()).toString(), "tooltip", tooltipMessage);
        } catch (IOException e) {
            log.error("Error getting icon path for " + icon, e);
            iconData = Map.of("icon", "", "tooltip", tooltipMessage);
        }
        return iconData;
    }

    void removeMessage(@NotNull AIChatMessage message) {
        execute("removeMessage", Map.of("id", message.id()));
    }

    void clearChat() {
        execute("clearChat", Map.of());
    }

    void showEmptyChat() {
        execute("showCenterText", Map.of("text", AIChatMessages.ai_chat_empty_hint));
    }

    void execute(@NotNull String function, @NotNull Object... args) {
        if (browser.isDisposed()) {
            return;
        }
        String merged = Stream.of(args)
            .map(JSONUtils.GSON::toJson)
            .collect(Collectors.joining(","));
        browser.execute(function + '(' + merged + ')');
    }

    @NotNull
    private String convertSQLToHTML(@NotNull String sql) {
        if (ruleScanner == null || syntaxManager == null) {
            // They should be initialized by now.
            // But in some odd case it fails (#7956)
            updateSQLParserContext();
        }
        DBPDataSource dataSource = getDataSource();
        HTMLSQLConverter converter = new HTMLSQLConverter();
        return converter.convertText(
            dataSource != null ? dataSource.getSQLDialect() : syntaxManager.getDialect(),
            syntaxManager,
            ruleScanner,
            new Document(sql),
            0,
            sql.length(),
            Map.of(HTMLSQLConverter.OPTION_ADD_CODE_BLOCK, true)
        );
    }

    void updateSQLParserContext() {
        syntaxManager = new SQLSyntaxManager();
        DBPDataSource dataSource = getDataSource();
        if (dataSource != null) {
            syntaxManager.init(dataSource.getSQLDialect(), dataSource.getContainer().getPreferenceStore());
        } else {
            syntaxManager.init(syntaxManager.getDialect(), syntaxManager.getPreferenceStore());
        }

        ruleScanner = new SQLRuleScanner();
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);

        if (dataSource != null) {
            ruleScanner.refreshRules(dataSource.getContainer(), ruleManager, null);
        } else {
            ruleScanner.refreshRules(null, ruleManager, null);
        }
    }

}
