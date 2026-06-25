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
package org.jkiss.dbeaver.ui.ai.chat;

import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnections;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.controls.AIChatControl;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatIcons;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessages;
import org.jkiss.dbeaver.ui.ai.controls.ScopeSelectorControl;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AIChatUtils {

    private static final Log log = Log.getLog(AIChatUtils.class);

    public static void chooseCustomScope(
        @NotNull Shell shell,
        @NotNull AIContextSettings settings,
        @NotNull DBPDataSourceContainer container,
        @NotNull AIChatContextProvider contextProvider,
        @Nullable AIChatConversation conversation
    ) {
        if (container.isConnected() && container.getDataSource() != null) {
            chooseContextCustomScope(shell, settings, container, contextProvider, conversation);
            return;
        }
        UIServiceConnections service = DBWorkbench.getService(UIServiceConnections.class);
        if (service == null) {
            DBWorkbench.getPlatformUI().showError("Can't connect to data source", "Try establishing the connection manually");
            return;
        }
        try {
            UIUtils.runWithMonitor(monitor ->
                DBUtils.initDataSource(monitor, container, e -> {
                    if (!e.isOK()) {
                        DBWorkbench.getPlatformUI().showError(
                            ModelMessages.dialog_connection_wizard_start_dialog_error_message,
                            null,
                            e
                        );
                    } else {
                        chooseContextCustomScope(shell, settings, container, contextProvider, conversation);
                    }
                }
            ));
        } catch (DBException e) {
            log.error(e);
        }
    }

    public static void chooseContextCustomScope(
        @NotNull Shell shell,
        @NotNull AIContextSettings settings,
        @NotNull DBPDataSourceContainer dataSourceContainer,
        @NotNull AIChatContextProvider contextProvider,
        @Nullable AIChatConversation conversation
    ) {
        DBCExecutionContext executionContext = contextProvider.getExecutionContext(dataSourceContainer);
        if (executionContext != null) {
            UIUtils.syncExec(() ->
                AIChatUtils.chooseCustomDataSourceScope(
                shell,
                settings,
                executionContext,
                conversation
            ));
        } else {
            DBWorkbench.getPlatformUI().showError(
                ModelMessages.dialog_connection_wizard_start_dialog_error_message,
                ModelMessages.error_not_connected_to_database
            );
        }
    }

    public static void chooseCustomDataSourceScope(
        @NotNull Shell shell,
        @NotNull AIContextSettings settings,
        @NotNull DBCExecutionContext executionContext,
        @Nullable AIChatConversation conversation
    ) {
        Set<String> passedObjects = ArrayUtils.isEmpty(settings.getCustomObjectIds()) ?
            Set.of() : Set.of(settings.getCustomObjectIds());
        List<String> ids = ScopeSelectorControl.chooseCustomEntities(
            shell,
            UIUtils.getDefaultRunnableContext(),
            executionContext,
            passedObjects
        );
        if (ids != null) {
            settings.setScope(AIDatabaseScope.CUSTOM);
            settings.setCustomObjectIds(ids.toArray(String[]::new));
            saveContextSettings(conversation, settings);
        }
    }

    public static void saveContextSettings(
        @Nullable AIChatConversation conversation,
        @NotNull AIContextSettings settings
    ) {
        try {
            if (conversation != null) {
                if (settings instanceof AIChatConversationSettings cs) {
                    conversation.setCustomSettings(cs);
                } else {
                    conversation.setCustomSettings(null);
                }
            }
            settings.saveSettings();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Settings save", "Settings save failed", e);
        }
    }

    /**
     * Copies the provided text to the system clipboard.
     *
     * @param display the display used for accessing the system clipboard; must not be null
     * @param text the textual content to be copied to the clipboard; must not be null
     */
    public static void copyToClipboard(@NotNull Display display, @NotNull String text) {
        UIUtils.setClipboardContents(display, TextTransfer.getInstance(), text);
    }

    /**
     * Opens the SQL code in an editor, optionally including a comment based on the conversation context.
     *
     * @param chat the AIChatControl instance responsible for managing the chat and controlling editor operations; must not be null
     * @param sqlCode the SQL code to be opened in the editor; must not be null
     * @param message the AIMessage instance representing the message in the conversation used for generating a description; must not be null
     */
    public static void openInEditor(@NotNull AIChatControl chat, @NotNull String sqlCode, @NotNull AIMessage message) {
        String contents = sqlCode;

        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AIConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT)) {
            var description = AIChatUtils.createConversationDescription(chat.getActiveConversation(), message);
            if (!description.isBlank()) {
                var container = chat.getDataSourceContainer();
                var comment = SQLUtils.generateCommentLine(container != null ? container.getDataSource() : null, description);
                contents = comment + contents;
            }
        }

        chat.getController().openInEditor(contents, chat.getActiveConversation());
    }

    private static String createConversationDescription(AIChatConversation conversation, AIMessage message) {
        List<AIChatMessage> messages = conversation.getMessages();
        if (messages.isEmpty() || messages.getFirst().message().isAutoGenerated()) {
            return ""; // it seems that it doesn't make sense to generate descriptions for autogenerated requests
        }
        return messages.stream()
            .takeWhile(m -> m.message() != message)
            .map(AIChatMessage::message)
            .filter(messaged -> messaged.getRole() == AIMessageType.USER)
            .map(AIMessage::getContent)
            .map(CommonUtils::capitalizeWord)
            .collect(Collectors.joining(". "));
    }

    /**
     * Executes the specified text in the editor using the provided AIChatController.
     *
     * @param chatController the AIChatController instance used to execute the text in the editor; must not be null
     * @param text the text to be executed in the editor; must not be null
     */
    public static void executeInEditor(@NotNull AIChatController chatController, @NotNull String text) {
        chatController.executeInEditor(text);
    }

    /**
     * Clears all messages in the active conversation from the provided message onward,
     * including the specified message, after user confirmation.
     *
     * @param chatSession the AIChatSession instance managing the current chat session; must not be null.
     * @param activeConversation the AIChatConversation instance representing the active conversation; must not be null.
     * @param message the AIChatMessage instance marking the message from which onward messages will be cleared; must not be null.
     */
    public static void clearToHere(
        @NotNull AIChatSession chatSession,
        @NotNull AIChatConversation activeConversation,
        @NotNull AIChatMessage message
    ) {
        if (DBWorkbench.getPlatformUI().confirmAction(
            AIChatMessages.ai_chat_clear_history_confirm_title,
            AIChatMessages.ai_chat_clear_history_partial_confirm_message
        )) {
            chatSession.notifyMessagesRemove(activeConversation, message);
            activeConversation.clearMessagesAfter(message);
        }
    }

    /**
     * Formats the message timestamp into a user-friendly string representation.
     * If the message was sent on the current day, it returns only the time in a short format (e.g., "10:30 AM").
     * Otherwise, it includes both the date and time in a medium format (e.g., "Jul 30, 2025, 10:30 AM").
     *
     * @param message the AIMessage instance from which the timestamp is extracted; must not be null.
     * @return a string representing the formatted timestamp of the message.
     */
    @NotNull
    public static String formatMessageTime(@NotNull AIMessage message) {
        LocalDateTime time = message.getTime();
        if (time.getDayOfYear() == LocalDateTime.now().getDayOfYear()) {
            return time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
        } else {
            return time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        }
    }

    /**
     * Retrieves the appropriate icon representing the role associated with the given AIMessage.
     *
     * @param message the AIMessage instance whose role icon is to be determined; must not be null
     * @return the corresponding DBPImage representing the role of the provided message
     */
    @NotNull
    public static DBPImage getRoleIcon(@NotNull AIMessage message) {
        return switch (message.getRole()) {
            case SYSTEM -> AIChatIcons.ROCKET;
            case USER -> message.isAutoGenerated() ? AIChatIcons.ROCKET : AIChatIcons.USER;
            case ASSISTANT, FUNCTION, CONFIRMATION -> AIChatIcons.ASSISTANT;
            case WARNING -> DBIcon.STATUS_WARNING;
            case ERROR -> DBIcon.STATUS_ERROR;
            case ATTACHMENT -> AIChatIcons.USER;
        };
    }

    @NotNull
    public static String normalizeConversationCaption(@NotNull String caption) {
        if (caption.contains("```")) {
            caption = caption.replaceAll("```", "");
        }
        if (caption.startsWith("\n")) {
            caption = caption.replaceFirst("\n", "");
        }
        if (caption.contains("\n")) {
            caption = caption.replaceAll("[\\n\\r\\t]", " ");
        }
        return caption;
    }

    public static void updateChatToggleContribution() {
        UIUtils.asyncExec(() -> ActionUtils.fireCommandRefresh(SQLEditorCommands.CMD_AI_CHAT_TOGGLE));
    }
}
