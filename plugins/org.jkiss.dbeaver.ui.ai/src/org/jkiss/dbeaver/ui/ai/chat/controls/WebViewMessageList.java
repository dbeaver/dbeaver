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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.registry.AISettingsEventListener;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.file.FileTypeHandlerDescriptor;
import org.jkiss.dbeaver.model.file.FileTypeHandlerRegistry;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.AIChatUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessages;
import org.jkiss.dbeaver.ui.ai.internal.AIUIFeatures;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

final class WebViewMessageList extends Composite implements AISettingsEventListener {

    private static final Log log = Log.getLog(WebViewMessageList.class);
    public static final String ATTACHMENT_CREATE_CONNECTION = "attachmentCreateConnection";
    public static final String ATTACHMENT_EXPORT_TO_TABLE = "attachmentExportToTable";
    public static final String ATTACHMENT_OPEN_FILE_IN_EXPLORER = "openFileInExplorer";

    private final Browser browser;
    private final AISettingsManager settingsManager;
    private final AIFunctionAllowMenu functionAllowMenu;
    private final Map<Integer, PendingConfirmation> pendingConfirmations = new HashMap<>();
    private final AIMessageChunkBuffer messageChunkBuffer;
    private final WebViewMessageRenderer renderer;
    private AIChatConversation conversation;
    private volatile boolean isInitWaiting = true;

    WebViewMessageList(@NotNull AIChatControl chat, @NotNull Composite parent) throws IOException {
        super(parent, SWT.NONE);
        browser = new Browser(this, SWT.NONE);
        renderer = new WebViewMessageRenderer(browser, this::getDataSource);

        messageChunkBuffer = new AIMessageChunkBuffer(getDisplay(), renderer::addMessageChunk, this::isDisposed);
        chat.getChatSession().addListener(new AIChatListener() {
            @Override
            public void messageAdded(@NotNull AIChatConversation messageConversation, @NotNull AIChatMessage message) {
                if (messageConversation != conversation) {
                    return;
                }
                if (message.message().getConfirmation() instanceof AIFunctionCallConfirmation fcc) {
                    AIToolboxManager toolboxManager = chat.getChatSession().getAssistant().getToolboxManager();
                    List<AIFunctionCall> callsToConfirm = functionAllowMenu.getFunctionCallsToConfirm(fcc);
                    List<AIFunctionCall> autoApprovedCalls = functionAllowMenu.getAutoApprovedFunctionCalls(fcc, callsToConfirm);
                    if (!autoApprovedCalls.isEmpty() && (hasExternalFunctionCalls(autoApprovedCalls, chat)
                        || new AIFunctionCallConfirmation(autoApprovedCalls).hasInformationFunctions(toolboxManager))) {
                        UIUtils.syncExec(() -> showAutoConfirmedStatus(message, chat, new AIFunctionCallConfirmation(autoApprovedCalls)));
                    }
                    if (!callsToConfirm.isEmpty()) {
                        AIFunctionCallConfirmation pendingConfirmation = new AIFunctionCallConfirmation(callsToConfirm);
                        pendingConfirmations.put(message.id(), new PendingConfirmation(pendingConfirmation, autoApprovedCalls));
                        UIUtils.syncExec(() -> showFunctionCallConfirmation(chat, message, pendingConfirmation));
                    } else {
                        submitFunctionCallResponses(chat, messageConversation, fcc.getFunctionCalls(), List.of());
                    }
                    return;
                }
                if (message.message().getRole() == AIMessageType.FUNCTION) {
                    AIFunctionCall fc = message.message().getFunctionCall();
                    if (fc != null) {
                        String result = message.message().getDisplayMessage();
                        Map<String, Object> resultArgs = new HashMap<>();
                        resultArgs.put("messageId", message.id());
                        resultArgs.put("functionName", fc.getFunctionName());
                        resultArgs.put("result", CommonUtils.notEmpty(result));
                        resultArgs.put("resultLabel", AIChatMessages.ai_chat_confirm_result);
                        AIFunctionResult functionResult = message.message().getFunctionResult();
                        if (functionResult != null && functionResult.getException() != null) {
                            resultArgs.put("hasException", true);
                            resultArgs.put("text", NLS.bind(AIChatMessages.ai_chat_confirm_failed, fc.getFunctionDisplayName()));
                            resultArgs.put("paramsLabel", AIChatMessages.ai_chat_confirm_params);
                            Map<String, Object> arguments = fc.getArguments();
                            if (!arguments.isEmpty()) {
                                resultArgs.put("arguments", arguments);
                            }
                        }
                        UIUtils.syncExec(() -> renderer.execute("updateFunctionResult", resultArgs));
                    }
                }
                UIUtils.syncExec(() -> {
                    messageChunkBuffer.clear();
                    if (renderer.addMessage(message)) {
                        renderer.updateChatMeta(conversation);
                    }
                });
            }

            @Override
            public void messageRemoved(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message) {
                UIUtils.syncExec(() -> {
                    messageChunkBuffer.clear();
                    if (message.message().getConfirmation() instanceof AIFunctionCallConfirmation) {
                        pendingConfirmations.remove(message.id());
                    }
                    renderer.removeMessage(message);
                    renderer.updateChatMeta(WebViewMessageList.this.conversation);
                });
            }

            @Override
            public void conversationChanged(@NotNull AIChatConversation conversation) {
                UIUtils.syncExec(() -> {
                    messageChunkBuffer.clear();
                    setConversation(conversation, false);
                    renderer.updateChatMeta(WebViewMessageList.this.conversation);
                });
            }

            @Override
            public void busyChanged(boolean busy) {
                UIUtils.asyncExec(() -> renderer.execute("setBusy", Map.of("busy", busy)));
            }

            @Override
            public void messageChunkReceived(
                @NotNull AIChatConversation messageConversation,
                @NotNull AIChatMessage message,
                @NotNull String chunk
            ) {
                if (isDisposed() || messageConversation != conversation) {
                    return;
                }
                messageChunkBuffer.append(message, chunk);
            }
        });

        setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));


        browser.setJavascriptEnabled(true);
        functionAllowMenu = new AIFunctionAllowMenu(
            browser,
            chat.getChatSession().getAssistant().getToolboxManager(),
            messageId -> {
                PendingConfirmation pending = pendingConfirmations.get(messageId);
                if (pending == null) {
                    return null;
                }
                return new AIFunctionAllowMenu.PendingConfirmationState(
                    pending.fcc,
                    pending.approvedIndices,
                    pending.declinedIndices
                );
            },
            approvals -> {
                for (AIFunctionAllowMenu.PendingApproval approval : approvals) {
                    renderer.execute("approveFunctionConfirmation", approval.messageId(), approval.functionIndex());
                }
            }
        );

        browser.setVisible(false);
        WebCSSInitializer cssInitializer = new WebCSSInitializer();
        browser.setUrl(cssInitializer.getWebHtmlPath());

        browser.addProgressListener(ProgressListener.completedAdapter(e -> {
            browser.setVisible(true);
            isInitWaiting = false;
            renderer.initChat();
            if (conversation != null) {
                setConversation(conversation, true);
                renderer.updateChatMeta(conversation);
            } else {
                renderer.showEmptyChat();
            }
            renderer.execute("setBusy", Map.of("busy", chat.getChatSession().isBusy()));
        }));

        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(@NotNull LocationEvent event) {
                String url = event.location;
                if (shouldOpenExternally(url)) {
                    event.doit = false;
                    Program.launch(url);
                }
            }

            private boolean shouldOpenExternally(@NotNull String url) {
                return url.startsWith("http://") || url.startsWith("https://");
            }
        });

        settingsManager = AISettingsManager.getInstance();
        settingsManager.addChangedListener(this);
        addDisposeListener(e -> {
            messageChunkBuffer.clear();
            settingsManager.removeChangedListener(this);
        });
        new WebViewDndController(this, browser, chat, this::getDataSource);
        createFunctions(chat);
    }

    @Override
    public void onSettingsUpdate(@NotNull AISettingsManager registry) {
        UIUtils.asyncExec(() -> renderer.execute("settingsChanged"));
    }

    private void createFunctions(@NotNull AIChatControl chat) {
        createFunction("setClipboardContents", arguments -> {
            if (arguments.length < 1) {
                throw new IllegalArgumentException("setClipboardContents requires at least one argument");
            }
            AIChatUtils.copyToClipboard(getDisplay(), arguments[0].toString());
            return null;
        });

        createFunction("executeInEditor", arguments -> {
            if (arguments.length < 1) {
                throw new IllegalArgumentException("executeInEditor requires at least one argument");
            }
            AIChatUtils.executeInEditor(chat.getController(), arguments[0].toString());
            return null;
        });

        createFunction("openInEditor", arguments -> {
            if (arguments.length < 2) {
                throw new IllegalArgumentException("openInEditor requires at least one argument");
            }
            AIChatMessage message = getMessageById(arguments[1]);
            AIChatUtils.openInEditor(chat, arguments[0].toString(), message.message());
            return null;
        });

        createFunction("clearToHere", arguments -> {
            if (arguments.length < 1) {
                throw new IllegalArgumentException("clearToHere requires at least one argument");
            }
            AIChatMessage message = getMessageById(arguments[0]);
            UIUtils.asyncExec(() -> AIChatUtils.clearToHere(chat.getChatSession(), conversation, message));
            return null;
        });

        createFunction("deleteMessage", arguments -> {
            if (arguments.length < 1) {
                throw new IllegalArgumentException("deleteMessage requires at least one argument");
            }
            AIChatMessage message = getMessageById(arguments[0]);
            UIUtils.asyncExec(() ->
                chat.getChatSession().notifyMessageRemove(conversation, message));
            return null;
        });

        createFunction("executeFunction", arguments -> {
            if (arguments.length < 1) {
                throw new IllegalArgumentException("executeFunction requires at least one argument");
            }
            AIChatMessage message = getMessageById(arguments[0]);
            var functionResult = message.message().getFunctionResult();
            var functionCall = message.message().getFunctionCall();
            if ((functionResult == null || functionResult.getCallback() == null) &&
                (functionCall == null || functionCall.getFunction() == null)) {
                log.error("Message with id " + arguments[0] + " don't have function");
            } else {
                new AbstractJob("Execute function") {
                    @NotNull
                    @Override
                    protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                        try {
                            if (functionResult != null && functionResult.getCallback() != null) {
                                functionResult.getCallback().runTask(monitor);
                            } else if (functionCall != null && functionCall.getFunction() != null) {
                                AIDatabaseContext databaseContext = chat.getChatSession().createDatabaseContext(monitor, conversation);
                                functionCall.getFunction().getInstance().callFunction(
                                    new AIFunctionContext(monitor, databaseContext, conversation.getPromptGenerator()),
                                    functionCall.getArguments()
                                );
                            }
                        } catch (DBException e) {
                            return GeneralUtils.makeExceptionStatus(e);
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
            return null;
        });

        createFunction(ATTACHMENT_CREATE_CONNECTION, arguments -> {
            if (arguments.length < 1) {
                throw new IllegalArgumentException("attachmentCreateConnection requires at least one argument");
            }
            createAttachmentInteractionEvent(ATTACHMENT_CREATE_CONNECTION);
            AIMessageFiles attachment = getMessageFilesById(arguments[0]);
            attachment.createConnection();
            return null;
        });

        createFunction(ATTACHMENT_EXPORT_TO_TABLE, arguments -> {
            if (arguments.length < 1) {
                throw new IllegalArgumentException("attachmentExportToTable requires at least one argument");
            }
            createAttachmentInteractionEvent(ATTACHMENT_EXPORT_TO_TABLE);
            AIMessageFiles attachment = getMessageFilesById(arguments[0]);
            attachment.importData();
            return null;
        });

        createFunction(ATTACHMENT_OPEN_FILE_IN_EXPLORER, arguments -> {
            if (arguments.length < 1) {
                throw new IllegalArgumentException("openFileInExplorer requires at least one argument");
            }
            String filePath = String.valueOf(arguments[0]);
            createAttachmentInteractionEvent(ATTACHMENT_OPEN_FILE_IN_EXPLORER);
            UIUtils.asyncExec(() -> {
                try {
                    Path file = Path.of(filePath);
                    if (Files.exists(file)) {
                        ShellUtils.showInSystemExplorer(file.toFile());
                    } else {
                        DBWorkbench.getPlatformUI().showError("File not found", NLS.bind("The file does not exist: {0}", filePath));
                    }
                } catch (Exception e) {
                    log.error("Error opening file in explorer: " + filePath, e);
                    DBWorkbench.getPlatformUI().showError("Error", NLS.bind("Failed to open file in explorer: {0}", e.getMessage()));
                }
            });

            return null;
        });

        createFunction("getFileTypeHandlerExtensions", arguments -> {
            List<FileTypeHandlerDescriptor> handlers = FileTypeHandlerRegistry.getInstance().getHandlers();
            List<String> result = new ArrayList<>();
            for (FileTypeHandlerDescriptor d : handlers) {
                if (!d.isDatabaseHandler()) {
                    continue;
                }
                for (FileTypeHandlerDescriptor.Extension extension : d.getExtensions()) {
                    result.add(extension.getId());
                }
            }
            return result.toArray();
        });

        createFunction("setAttachmentFileType", arguments -> {
            if (arguments.length < 2) {
                return new Object[] {false, false};
            }
            String handlerId;
            try {
                handlerId = String.valueOf(arguments[1]);
            } catch (Exception e) {
                log.error("Invalid arguments in setAttachmentFileType", e);
                return new Object[] {false, false};
            }
            AIChatMessage message = getMessageById(arguments[0]);
            if (!(message.message() instanceof AIMessageFiles attachment)) {
                log.warn("Message with id " + arguments[0] + " is not an attachment");
                return new Object[] {false, false};
            }
            attachment.setExplicitHandlerForFile(handlerId);
            Map<String, Object> stringObjectMap = AIUIFeatures.buildFeatureParameters(
                getDataSource() == null ? null : getDataSource().getContainer(),
                Map.of(AIUIFeatures.ATTACHMENT_HANDLER_ID, handlerId)
            );
            AIUIFeatures.AI_CHAT_OPEN_AS_EVENT.use(stringObjectMap);
            boolean canCreate = attachment.canCreateConnection();
            boolean canImport = attachment.canImportData();
            return new Object[] {canCreate, canImport};
        });

        createFunction("getSetting", arguments -> {
            AISettings settings = settingsManager.getSettings();
            return switch (arguments.length) {
                case 1 -> settings.getProperty(String.valueOf(arguments[0]));
                case 2 -> settings.getProperty(String.valueOf(arguments[0]), arguments[1]);
                default -> throw new IllegalArgumentException("getSetting requires one or two arguments");
            };
        });

        createFunction("setSetting", arguments -> {
            if (arguments.length != 2) {
                throw new IllegalArgumentException("setSetting requires two arguments");
            }
            settingsManager.modifySettings(s -> s.setProperty(String.valueOf(arguments[0]), arguments[1]));
            return null;
        });

        createFunction("confirmFunctionCalls", arguments -> {
            if (arguments.length < 2) {
                throw new IllegalArgumentException("confirmFunctionCalls requires messageId and functionIndex");
            }
            int messageId = CommonUtils.toInt(arguments[0]);
            int functionIndex = CommonUtils.toInt(arguments[1]);
            PendingConfirmation pending = pendingConfirmations.get(messageId);
            if (pending == null) {
                log.warn("No pending confirmation found for message id " + messageId);
                return null;
            }
            pending.approvedIndices.add(functionIndex);
            checkAllDecisions(chat, messageId, pending);
            return null;
        });

        createFunction("denyFunctionCalls", arguments -> {
            if (arguments.length < 2) {
                throw new IllegalArgumentException("denyFunctionCalls requires messageId and functionIndex");
            }
            int messageId = CommonUtils.toInt(arguments[0]);
            int functionIndex = CommonUtils.toInt(arguments[1]);
            PendingConfirmation pending = pendingConfirmations.get(messageId);
            if (pending == null) {
                log.warn("No pending confirmation found for message id " + messageId);
                return null;
            }
            pending.declinedIndices.add(functionIndex);
            checkAllDecisions(chat, messageId, pending);
            return null;
        });
    }

    private void createAttachmentInteractionEvent(@NotNull String attachmentCreateConnection) {
        AIUIFeatures.AI_CHAT_ATTACHMENT_INTERACT.use(AIUIFeatures.buildFeatureParameters(
            getDataSource() == null ? null : getDataSource().getContainer(), Map.of(
                AIUIFeatures.ATTACHMENT_INTERACTION_NAME, attachmentCreateConnection
            )
        ));
    }

    private void createFunction(@NotNull String name, @NotNull Function<Object[], Object> handler) {
        new BrowserFunction(browser, name) {
            @Nullable
            @Override
            public Object function(@NotNull Object[] arguments) {
                super.function(arguments);
                return handler.apply(arguments);
            }
        };
    }

    @NotNull
    private AIMessageFiles getMessageFilesById(@NotNull Object id) {
        AIChatMessage message = getMessageById(id);
        if (!(message.message() instanceof AIMessageFiles attachment)) {
            throw new IllegalArgumentException("Message with id " + id + " is not an attachment");
        }
        return attachment;
    }

    @NotNull
    private AIChatMessage getMessageById(@NotNull Object id) {
        int messageId = CommonUtils.toInt(id);
        return conversation.getMessages().stream().filter(msg -> msg.id() == messageId).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Message with id " + id + " not found"));
    }

    @Nullable
    private DBPDataSource getDataSource() {
        return conversation == null || conversation.getDataSource() == null ? null : conversation.getDataSource().getDataSource();
    }

    private void setConversation(@NotNull AIChatConversation conversation, boolean forceUpdateContext) {
        AIChatConversation oldConversation = this.conversation;
        this.conversation = conversation;
        if (oldConversation != conversation) {
            pendingConfirmations.clear();
        }
        if (isInitWaiting) {
            return;
        }
        if (forceUpdateContext || oldConversation == null || oldConversation.getDataSource() != conversation.getDataSource()) {
            renderer.updateSQLParserContext();
        }
        renderer.clearChat();
        renderer.showConversationMessages(conversation);
        if (conversation.getMessages().isEmpty()) {
            renderer.showEmptyChat();
        }
    }

    private boolean hasExternalFunctionCalls(
        @NotNull List<AIFunctionCall> functionCalls,
        @NotNull AIChatControl chat
    ) {
        AIToolboxManager toolboxManager = chat.getChatSession().getAssistant().getToolboxManager();
        for (AIFunctionCall fc : functionCalls) {
            AIFunctionDescriptor function = fc.getOrResolveFunction(toolboxManager);
            if (function != null && !AIConstants.INTERNAL_TOOLBOX_ID.equals(function.getToolbox().getToolboxId())) {
                return true;
            }
        }
        return false;
    }

    private void showAutoConfirmedStatus(
        @NotNull AIChatMessage message,
        @NotNull AIChatControl chat,
        @NotNull AIFunctionCallConfirmation fcc
    ) {
        AIToolboxManager toolboxManager = chat.getChatSession().getAssistant().getToolboxManager();
        for (AIFunctionCall fc : fcc.getFunctionCalls()) {
            AIFunctionDescriptor descriptor = fc.getOrResolveFunction(toolboxManager);
            String toolName = descriptor != null ? descriptor.getName() : fc.getFunctionName();
            String functionName = fc.getFunctionName();
            String text = NLS.bind(AIChatMessages.ai_chat_confirm_auto_approved, toolName);

            Map<String, Object> args = new HashMap<>();
            args.put("messageId", message.id());
            args.put("text", text);
            args.put("functionName", functionName);
            args.put("confirmed", true);
            args.put("paramsLabel", AIChatMessages.ai_chat_confirm_params);
            args.put("resultLabel", AIChatMessages.ai_chat_confirm_result);

            Map<String, Object> arguments = fc.getArguments();
            if (!arguments.isEmpty()) {
                args.put("arguments", arguments);
            }

            renderer.execute("addAutoConfirmedStatus", args);
        }
    }

    private void showFunctionCallConfirmation(
        @NotNull AIChatControl chat,
        @NotNull AIChatMessage message,
        @NotNull AIFunctionCallConfirmation fcc
    ) {
        AIToolboxManager toolboxManager = chat.getChatSession().getAssistant().getToolboxManager();
        List<AIFunctionCall> functionCalls = fcc.getFunctionCalls();
        for (int i = 0; i < functionCalls.size(); i++) {
            AIFunctionCall fc = functionCalls.get(i);
            AIFunctionDescriptor descriptor = fc.getOrResolveFunction(toolboxManager);
            String agentName = descriptor != null ? descriptor.getToolbox().getDisplayName() : "";
            String toolName = descriptor != null ? descriptor.getName() : fc.getFunctionName();
            String title = NLS.bind(AIChatMessages.ai_chat_confirm_title, agentName, toolName);

            Map<String, Object> args = new HashMap<>();
            args.put("messageId", message.id());
            args.put("functionIndex", i);
            args.put("title", title);
            args.put("titlePrefix", AIChatMessages.ai_chat_confirm_title_prefix);
            args.put("titleMiddle", AIChatMessages.ai_chat_confirm_title_middle);
            args.put("agentName", agentName);
            args.put("toolName", toolName);
            args.put("functionName", fc.getFunctionName());
            args.put("allowText", AIChatMessages.ai_chat_confirm_approve);
            args.put("declineText", AIChatMessages.ai_chat_confirm_deny);
            args.put("approvedText", NLS.bind(AIChatMessages.ai_chat_confirm_approved, toolName));
            args.put("declinedText", NLS.bind(AIChatMessages.ai_chat_confirm_declined, toolName));
            args.put("paramsLabel", AIChatMessages.ai_chat_confirm_params);
            args.put("resultLabel", AIChatMessages.ai_chat_confirm_result);

            Map<String, Object> arguments = fc.getArguments();
            if (!arguments.isEmpty()) {
                args.put("arguments", arguments);
            }

            renderer.execute("addFunctionConfirmation", args);
        }
    }

    private void checkAllDecisions(
        @NotNull AIChatControl chat,
        int messageId,
        @NotNull PendingConfirmation pending
    ) {
        int totalDecisions = pending.approvedIndices.size() + pending.declinedIndices.size();
        if (totalDecisions < pending.fcc.getFunctionCalls().size()) {
            return;
        }
        pendingConfirmations.remove(messageId);

        List<AIFunctionCall> allCalls = pending.fcc.getFunctionCalls();
        List<AIFunctionCall> approvedCalls = new ArrayList<>(pending.autoApprovedCalls);
        List<AIFunctionCall> declinedCalls = new ArrayList<>();
        for (int idx : pending.approvedIndices) {
            if (idx >= 0 && idx < allCalls.size()) {
                approvedCalls.add(allCalls.get(idx));
            }
        }
        for (int idx : pending.declinedIndices) {
            if (idx >= 0 && idx < allCalls.size()) {
                declinedCalls.add(allCalls.get(idx));
            }
        }
        submitFunctionCallResponses(
            chat,
            conversation,
            approvedCalls,
            declinedCalls
        );
    }

    private void submitFunctionCallResponses(
        @NotNull AIChatControl chat,
        @NotNull AIChatConversation conversation,
        @NotNull List<AIFunctionCall> approvedCalls,
        @NotNull List<AIFunctionCall> declinedCalls
    ) {
        chat.getChatSession().setBusy(true);

        if (!declinedCalls.isEmpty()) {
            chat.getChatSession().declineFunctionCalls(conversation, declinedCalls);
        }

        CompletableFuture<AIChatConversation> submission;
        if (approvedCalls.isEmpty()) {
            submission = scheduleConversationSubmission(chat, conversation, null, "Continue AI conversation");
        } else {
            submission = scheduleConversationSubmission(
                chat,
                conversation,
                new AIFunctionCallConfirmation(approvedCalls),
                "Confirm function calls"
            );
            if (!declinedCalls.isEmpty() && !hasInformationFunctionCalls(chat, approvedCalls)) {
                submission = submission.thenCompose(ignored ->
                    scheduleConversationSubmission(chat, conversation, null, "Continue AI conversation"));
            }
        }

        submission.whenComplete((ignored, error) -> chat.getChatSession().setBusy(false));
    }

    @NotNull
    private CompletableFuture<AIChatConversation> scheduleConversationSubmission(
        @NotNull AIChatControl chat,
        @NotNull AIChatConversation conversation,
        @Nullable AIConfirmation confirmation,
        @NotNull String jobName
    ) {
        CompletableFuture<AIChatConversation> result = new CompletableFuture<>();
        RuntimeUtils.scheduleJob(jobName, monitor -> {
            try {
                chat.getChatSession().submitConversation(
                    monitor,
                    getSubmitSettings(chat, conversation),
                    conversation,
                    confirmation
                ).whenComplete((submittedConversation, error) -> {
                    if (error != null) {
                        result.completeExceptionally(error);
                    } else {
                        result.complete(submittedConversation);
                    }
                });
            } catch (DBException e) {
                conversation.promptProcessed(true);
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    private static boolean hasInformationFunctionCalls(
        @NotNull AIChatControl chat,
        @NotNull List<AIFunctionCall> functionCalls
    ) {
        AIToolboxManager toolboxManager = chat.getChatSession().getAssistant().getToolboxManager();
        return AIUtils.hasInformationFunctions(toolboxManager, functionCalls);
    }

    @Nullable
    private AIContextSettings getSubmitSettings(
        @NotNull AIChatControl chat,
        @NotNull AIChatConversation conversation
    ) {
        AIContextSettings customSettings = conversation.getCustomSettings();
        return customSettings != null ? customSettings : chat.getCompletionSettings();
    }

    private static class PendingConfirmation {
        @NotNull
        final AIFunctionCallConfirmation fcc;
        @NotNull
        final List<AIFunctionCall> autoApprovedCalls;
        final Set<Integer> approvedIndices = new HashSet<>();
        final Set<Integer> declinedIndices = new HashSet<>();

        PendingConfirmation(
            @NotNull AIFunctionCallConfirmation fcc,
            @NotNull List<AIFunctionCall> autoApprovedCalls
        ) {
            this.fcc = fcc;
            this.autoApprovedCalls = autoApprovedCalls;
        }
    }
}
