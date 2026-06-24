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
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.prompt.AIPromptGenerateSql;
import org.jkiss.dbeaver.model.ai.qm.AIChatStorage;
import org.jkiss.dbeaver.model.ai.quota.UserTokenQuotaService;
import org.jkiss.dbeaver.model.ai.registry.AIAssistantRegistry;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.AIUIUtils;
import org.jkiss.dbeaver.ui.ai.chat.AIChatController;
import org.jkiss.dbeaver.ui.ai.chat.AIChatUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessages;
import org.jkiss.dbeaver.ui.ai.internal.AIUIFeatures;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AIChatControl extends Composite implements AIChatContextProvider {
    private static final Log log = Log.getLog(AIChatControl.class);

    private final AIChatSession chatSession;
    private final AIChatController controller;
    private final PromptComposite promptComposite;
    private volatile boolean waitingForResponse = false;
    private volatile boolean isDataSourceInitialized = false;

    private AIContextSettings activeCompletionSettings;
    private AIChatConversation activeConversation;

    public AIChatControl(@NotNull Composite parent, @NotNull AIChatController controller) {
        super(parent, SWT.NONE);
        this.controller = controller;

        DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        AIAssistant assistant = AIAssistantRegistry.getInstance().getAssistant(workspace);
        AIChatSession.SessionIdProvider chatSessionProvider = assistant.getChatSessionProvider();
        AIChatStorage chatStorage = assistant.createChatStorage();

        this.chatSession = new AIChatSession(
            workspace,
            this,
            chatStorage,
            chatSessionProvider,
            new UserTokenQuotaService(Clock.systemUTC(), chatStorage)
        );

        setBackgroundMode(SWT.INHERIT_FORCE);
        setLayout(new GridLayout());

        {
            DBCExecutionContext executionContext = controller.getExecutionContext();
            activeConversation = createEmptyConversation(
                executionContext == null ? null : executionContext.getDataSource().getContainer());
        }
        enableDragAndDrop(this);

        int chatFeatures = controller.getChatFeatures();

        if (CommonUtils.isBitSet(chatFeatures, AIChatController.FEATURE_CONTEXT_VIEW)) {
            createContextControl(this);
            UIUtils.createLineSeparator(this, SWT.HORIZONTAL);
        }
        Control messageListComposite = createMessageListControl(this);
        UIUtils.createLineSeparator(this, SWT.HORIZONTAL);

        if (CommonUtils.isBitSet(chatFeatures, AIChatController.FEATURE_PROMPT_VIEW)) {
            promptComposite = createPromptControl(this);
            // Handle mouse click on empty area
            messageListComposite.addMouseListener(
                MouseListener.mouseDownAdapter(mouseEvent -> promptComposite.setFocusOnPrompt()));
        } else {
            promptComposite = null;
        }

        WidgetElement.applyStyles(this, true);

        addDisposeListener(e -> chatSession.closeSession());
    }

    public boolean isWaitingForResponse() {
        return waitingForResponse;
    }

    public void setWaitingForResponse(boolean waitingForResponse) {
        this.waitingForResponse = waitingForResponse;
    }

    public void sendPrompt() {
        if (promptComposite != null) {
            promptComposite.submitPrompt();
        }
    }

    @NotNull
    public AIChatSession getChatSession() {
        return chatSession;
    }

    /**
     * Makes the provided {@code conversation} the current conversation,
     * updating the prompt and message history in the UI accordingly.
     * <p>
     * If there was an ongoing conversation, it will be saved and the {@code conversation} will be selected instead.
     * <p>
     * If the conversation is not yet recorded, it will be added to the list of conversations.
     * It can be later removed using {@link #removeActiveConversation()}.
     *
     * @param conversation the conversation to select
     * @see AIChatListener#conversationChanged(AIChatConversation)
     */
    public void selectConversation(@NotNull AIChatConversation conversation) throws DBException {
        if (conversation == activeConversation) {
            return;
        }

        if (activeConversation != null) {
            chatSession.addConversation(activeConversation);
        }

        updateActiveConversation(conversation);

        AIContextSettings settings;
        if (conversation.getDataSource() != null) {
            settings = chatSession.getConversationSettings(conversation);
        } else {
            settings = null;
        }

        setCompletionSettings(settings);
        //promptComposite.setPromptText(conversation.getInitialPrompt());

        chatSession.notifyListeners(AIChatListener::conversationChanged, conversation);
    }

    @NotNull
    public AIChatConversation getActiveConversation() {
        return activeConversation;
    }

    public void updateActiveConversation(@NotNull AIChatConversation conversation) throws DBException {
        chatSession.addConversation(conversation);
        activeConversation = conversation;
    }

    /**
     * Removes the active conversation from the list of conversations,
     * selecting the last conversation in the list if available.
     * <p>
     * If no conversations are left, a new one will be created.
     */
    public void removeActiveConversation() throws DBException {
        if (activeConversation == null) {
            return;
        }

        chatSession.removeConversation(activeConversation);
        activeConversation = null;

        List<AIChatConversation> conversations = listConversations();

        if (conversations.isEmpty()) {
            selectConversation(createEmptyConversation(getDataSourceContainer()));
        } else {
            selectConversation(conversations.getLast());
        }
    }

    @NotNull
    AIChatConversation createEmptyConversation(@NotNull String name, @Nullable DBPDataSourceContainer container) {
        AIPromptGenerator aiPromptGenerator = new AIPromptGenerateSql();

        return new AIChatConversation(
            name,
            aiPromptGenerator,
            container
        );
    }

    @NotNull
    AIChatConversation createEmptyConversation(@Nullable DBPDataSourceContainer container) {
        return createEmptyConversation(AIChatMessages.ai_chat_default_conversation_name, container);
    }

    /**
     * Returns the list of conversations that are associated with the current data source container.
     */
    @NotNull
    public List<AIChatConversation> listConversations() throws DBException {
        DBPDataSourceContainer container = getDataSourceContainer();
        return chatSession.getAllConversations(container);
    }

    public void cancelPrompt() {
        if (activeConversation != null) {
            activeConversation.cancelConversation();
            activeConversation.addMessage(AIMessage.warningMessage(AIChatMessages.ai_chat_conversation_cancelled));
            chatSession.notifyMessageAdd(activeConversation, activeConversation.getMessages().getLast());
        }
    }

    public void submitPrompt(@NotNull String prompt) {
        AIMessage promptMessage = AIMessage.userMessage(prompt.trim());
        if (promptMessage.getContent().isEmpty()) {
            return;
        }
        Map<String, Object> additionalParameters = new LinkedHashMap<>();
        additionalParameters.put(AIBaseFeatures.PROMPT_TYPE, activeConversation.getPromptGenerator().generatorId());
        AIUIFeatures.SEND_PROMPT_EVENT.use(AIUIFeatures.buildFeatureParameters(getDataSourceContainer(), additionalParameters));
        submitPrompt(promptMessage);
    }

    public void submitPrompt(@NotNull AIMessage promptMessage) {

        AIContextSettings customSettings = activeConversation.getCustomSettings();
        AIContextSettings settings = customSettings != null ? customSettings : getCompletionSettings();
        if (settings != null && !AIUIUtils.confirmMetaTransfer(settings)) {
            return;
        }

        if (!checkConfiguration()) {
            return;
        }
        waitingForResponse = true;

        AIChatMessage chatMessage = activeConversation.addMessage(promptMessage);
        chatSession.notifyMessageAdd(activeConversation, chatMessage);

        chatSession.setBusy(true);

        new AbstractJob("Execute prompt") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    CompletableFuture<AIChatConversation> future = chatSession.submitConversation(
                        monitor,
                        settings,
                        activeConversation,
                        null
                    );
                    future.whenComplete((conversation, throwable) -> {
                        waitingForResponse = false;
                        chatSession.setBusy(false);
                    });
                    return Status.OK_STATUS;
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
            }
        }.schedule();
    }

    public boolean checkConfiguration() {
        try {
            if (!AIUtils.hasValidConfiguration()) {
                AIUIUtils.showPreferences(getShell(), true);
                return false;
            }
        } catch (DBException e) {
            log.error(e);
        }
        return true;
    }

    @NotNull
    public AIAssistant getAssistant() {
        return chatSession.getAssistant();
    }

    public void setFocusOnPrompt() {
        promptComposite.setFocusOnPrompt();
    }

    public void renameConversation(@NotNull AIChatConversation conversation, @NotNull String newName) {
        conversation.setCaption(
            AIChatUtils.normalizeConversationCaption(newName)
        );
        chatSession.notifyConversationRenamed(conversation, newName);
    }

    @Nullable
    public DBPDataSourceContainer getDataSourceContainer() {
        return activeCompletionSettings != null ? activeCompletionSettings.getDataSourceContainer() : null;
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext(@NotNull DBPDataSourceContainer dataSourceContainer) {
        DBCExecutionContext executionContext = controller.getExecutionContext();
        if (executionContext != null && executionContext.getDataSource() == dataSourceContainer.getDataSource()) {
            return executionContext;
        }
        return DBUtils.getDefaultContext(dataSourceContainer, false);
    }

    @Nullable
    public AIContextSettings getCompletionSettings() {
        return activeCompletionSettings;
    }

    public void setCompletionSettings(@Nullable AIContextSettings completionSettings) {
        if (this.activeCompletionSettings != completionSettings) {
            this.activeCompletionSettings = completionSettings;
            chatSession.notifyListeners(AIChatListener::settingsChanged, completionSettings);
        }
    }

    public boolean isBusy() {
        return chatSession.isBusy();
    }

    public void setDataSource(@Nullable DBPDataSourceContainer container) throws DBException {
        if (container == getDataSourceContainer() && isDataSourceInitialized) {
            return;
        }
        isDataSourceInitialized = true;
        AIChatConversation conversation = chatSession.getLastConversation(container);
        this.setCompletionSettings(container == null || conversation == null ? null : chatSession.getConversationSettings(conversation));
        this.selectConversation(
            conversation != null ?
                conversation :
                createEmptyConversation(container));
    }

    public boolean hasMessages() {
        return !activeConversation.getMessages().isEmpty();
    }

    @NotNull
    public AIChatController getController() {
        return controller;
    }

    public void attachFiles(@NotNull List<Path> files) {
        if (files.isEmpty()) {
            return;
        }
        AIChatMessage message = activeConversation.addMessage(new AIMessageFiles(files));
        chatSession.notifyMessageAdd(activeConversation, message);
    }

    /**
     * Finds the AI chat conversation associated with the given SQL query in the specified editor
     * or creates a new conversation if none exists.
     *
     * @param editor          the SQL editor containing the query
     * @param query           the SQL script element to find or associate a conversation with
     * @param newConversation the conversation to associate if none already exists
     * @return the found or newly associated AI chat conversation
     */
    @NotNull
    public AIChatConversation findOrCreateAssociatedConversation(
        @NotNull SQLEditor editor,
        @NotNull SQLScriptElement query,
        @NotNull AIChatConversation newConversation
    ) throws DBException {
        IAnnotationModel annotationModel = editor.getAnnotationModel();
        if (annotationModel == null) {
            return newConversation;
        }
        UUID associatedConversationId = getAssociatedConversationId(annotationModel, query);
        if (associatedConversationId != null) {
            AIChatConversation associatedConversation = chatSession.getConversation(associatedConversationId);
            if (associatedConversation != null) {
                return associatedConversation;
            }
        }

        annotationModel.addAnnotation(
            new AIChatAnnotation(newConversation.getId()),
            new Position(query.getOffset(), query.getLength())
        );
        return newConversation;
    }

    /**
     * Retrieves the conversation ID associated with the given SQL script element
     * from the provided annotation model, if present.
     *
     * @param annotationModel the annotation model to search
     * @param query           the SQL script element to match
     * @return the associated conversation ID, or {@code null} if none found
     */
    @Nullable
    private UUID getAssociatedConversationId(
        @NotNull IAnnotationModel annotationModel,
        @NotNull SQLScriptElement query
    ) {
        Iterator<Annotation> iterator = annotationModel.getAnnotationIterator();
        while (iterator.hasNext()) {
            Annotation annotation = iterator.next();
            Position position = annotationModel.getPosition(annotation);
            if (annotation instanceof AIChatAnnotation aiChatAnnotation &&
                position != null &&
                position.getOffset() == query.getOffset() &&
                position.getLength() == query.getLength()
            ) {
                return aiChatAnnotation.getConversationId();
            }
        }
        return null;
    }

    @NotNull
    private Control createMessageListControl(@NotNull Composite parent) {
        try {
            return new WebViewMessageList(this, parent);
        } catch (IOException e) {
            log.error("Error creating WebViewMessageList", e);
        }

        return UIUtils.createLabel(parent, "Internal error creating web chat. See logs.");
    }

    private void enableDragAndDrop(@NotNull Control control) {
        int operations = DND.DROP_COPY | DND.DROP_DEFAULT;
        DropTarget dropTarget = new DropTarget(control, operations);
        dropTarget.setTransfer(FileTransfer.getInstance());
        dropTarget.addDropListener(new DropTargetAdapter() {
            @Override
            public void dragEnter(@NotNull DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragOver(@NotNull DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void dragOperationChanged(@NotNull DropTargetEvent event) {
                handleDragEvent(event);
            }

            @Override
            public void drop(@NotNull DropTargetEvent event) {
                String[] filePaths = (String[]) event.data;
                if (filePaths != null && filePaths.length > 0) {
                    List<Path> files = new ArrayList<>();
                    for (String p : filePaths) {
                        if (p != null) {
                            files.add(Path.of(p));
                        }
                    }
                    attachFiles(files);
                }
            }

            private void handleDragEvent(@NotNull DropTargetEvent event) {
                if (!isDropSupported(event)) {
                    event.detail = DND.DROP_NONE;
                } else {
                    event.detail = DND.DROP_COPY;
                }
            }

            private boolean isDropSupported(@NotNull DropTargetEvent event) {
                return FileTransfer.getInstance().isSupportedType(event.currentDataType);
            }
        });
    }

    @NotNull
    private ContextComposite createContextControl(@NotNull Composite parent) {
        ContextComposite composite = new ContextComposite(this, parent);
        composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        return composite;
    }

    @NotNull
    private PromptComposite createPromptControl(@NotNull Composite parent) {
        PromptComposite composite = new PromptComposite(this, parent);
        composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));
        return composite;
    }
}
