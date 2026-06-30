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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.qm.*;
import org.jkiss.dbeaver.model.ai.quota.QuotaStatus;
import org.jkiss.dbeaver.model.ai.quota.UserTokenQuotaService;
import org.jkiss.dbeaver.model.ai.registry.AIAssistantRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.zip.CRC32;

/**
 * Chat supports conversation and ability to submit new messages into conversations.
 */
public class AIChatSession {
    private static final Log log = Log.getLog(AIChatSession.class);
    private static final String DECLINED_FUNCTION_CALL_MESSAGE = "The user declined this tool call.";
    private static final DateTimeFormatter USER_DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault());

    private final Map<UUID, Long> conversationToContextChecksum = new WeakHashMap<>();
    private final List<AIChatListener> listeners = new ArrayList<>();
    private final DBPWorkspace workspace;
    private final AIChatContextProvider contextProvider;
    private final AIChatStorage storage;
    private final SessionIdProvider sessionIdProvider;
    private final UserTokenQuotaService quotaService;
    private Map<UUID, AIChatConversation> conversations;
    private boolean busy;
    private AIAssistant assistant;
    private volatile boolean initialized = false;
    private volatile boolean isClosed = false;

    public AIChatSession(
        @NotNull DBPWorkspace workspace,
        @NotNull AIChatContextProvider contextProvider,
        @NotNull AIChatStorage storage,
        @NotNull SessionIdProvider sessionIdProvider,
        @NotNull UserTokenQuotaService quotaService
    ) {
        this.workspace = workspace;
        this.contextProvider = contextProvider;
        this.storage = storage;
        this.sessionIdProvider = sessionIdProvider;
        this.quotaService = quotaService;

        this.addListener(quotaService);
        this.addListener(new ChatSessionListener(storage));
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void closeSession() {
        isClosed = true;
    }

    private void saveConversation(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIChatConversation conversation
    ) throws DBException {
        if (conversation.isTemporary()) {
            return;
        }

        AIContextSettings contextSettings = getConversationSettings(conversation);
        AIDatabaseContext databaseContext = null;
        try {
            if (contextSettings != null) {
                databaseContext = createDatabaseContext(monitor, conversation.getPromptGenerator(), contextSettings);
            }
        } catch (DBException e) {
            log.warn("Error creating database context for conversation save", e);
        }

        storage.saveConversation(
            sessionIdProvider.getSessionId(monitor),
            QMAIChatHistoryMapper.toQMAIChatHistory(conversation, contextSettings, databaseContext)
        );
    }

    @NotNull
    public AIChatStorage getStorage() {
        return storage;
    }

    @Nullable
    public AIDatabaseContext createDatabaseContext(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIChatConversation conversation
    ) throws DBException {
        AIContextSettings contextSettings = getConversationSettings(conversation);
        AIDatabaseContext databaseContext = null;
        if (contextSettings != null) {
            databaseContext = createDatabaseContext(monitor, conversation.getPromptGenerator(), contextSettings);
        }
        return databaseContext;
    }

    @Nullable
    public AIContextSettings getConversationSettings(@NotNull AIChatConversation conversation) {
        DBPDataSourceContainer dataSource = conversation.getDataSource();
        if (dataSource == null) {
            return null;
        }
        AICompletionSettings dataSourceSettings = new AICompletionSettings(dataSource);
        AIChatConversationSettings customSettings = conversation.getCustomSettings();
        if (customSettings != null && !dataSourceSettings.equalsSettings(customSettings)) {
            return customSettings;
        }
        return dataSourceSettings;
    }

    public void init() throws DBException {
        if (initialized) {
            return;
        }

        RuntimeUtils.runTask(
            monitor -> {
                try {
                    this.conversations = new LinkedHashMap<>();
                    List<QMAIConversationHistory> conversationsHistory = storage.findConversations(
                        sessionIdProvider.getSessionId(monitor)
                    );
                    for (QMAIConversationHistory history : conversationsHistory) {
                        DBPDataSourceContainer dataSourceContainer = getContainer(history.getDataSource());
                        AIChatConversation conversation = QMAIChatHistoryMapper.toAIChatConversation(
                            getAssistant(),
                            history,
                            dataSourceContainer
                        );
                        String contextJson = history.getContext().getContextJson();
                        if (contextJson != null) {
                            AIChatConversationSettings convSettings = new AIChatConversationSettings(this, conversation);
                            convSettings.loadSettingsFromString(contextJson);
                            convSettings.loadDataSourceDefaults();
                            conversation.setCustomSettings(convSettings);
                        }
                        this.conversations.put(conversation.getId(), conversation);
                    }

                    initialized = true;
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            }, "Initialize AI chat session", 10000
        );
    }

    @Nullable
    public AIChatConversation getLastConversation(@Nullable DBPDataSourceContainer container) throws DBException {
        init();

        AIChatConversation lastConversation = null;
        for (AIChatConversation conversation : conversations.values()) {
            if (conversation.getDataSource() == container) {
                if (lastConversation == null || conversation.getLastMessageTime().isAfter(lastConversation.getLastMessageTime())) {
                    lastConversation = conversation;
                }
            }
        }
        return lastConversation;
    }

    public void addConversation(@NotNull AIChatConversation conversation) throws DBException {
        init();

        conversations.put(conversation.getId(), conversation);
    }

    public void removeConversation(@NotNull AIChatConversation conversation) throws DBException {
        init();

        conversations.remove(conversation.getId());
        conversation.clearPendingDeclinedFunctionCallMessages();

        if (conversation.isTemporary()) {
            return;
        }

        storage.deleteConversation(
            conversation.getId().toString()
        );
    }

    @Nullable
    public AIChatConversation getConversation(@NotNull String id) throws DBException {
        return getConversation(UUID.fromString(id));
    }

    @Nullable
    public AIChatConversation getConversation(@NotNull UUID id) throws DBException {
        init();
        return conversations.get(id);
    }

    @NotNull
    public List<AIChatConversation> getAllConversations(@Nullable DBPDataSourceContainer container) throws DBException {
        init();

        return conversations.values().stream()
            .filter(it -> it.getDataSource() == container)
            .sorted(Comparator.comparing(AIChatConversation::getLastMessageTime).reversed())
            .toList();
    }

    @NotNull
    public Set<DBPDataSourceContainer> getAllDataSources() throws DBException {
        init();

        Set<DBPDataSourceContainer> dsList = new LinkedHashSet<>();
        for (AIChatConversation conversation : conversations.values()) {
            if (conversation.getDataSource() != null) {
                dsList.add(conversation.getDataSource());
            }
        }
        return dsList;
    }

    public void addListener(@NotNull AIChatListener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NotNull AIChatListener listener) {
        listeners.remove(listener);
    }

    public <T> void notifyListeners(@NotNull BiConsumer<AIChatListener, T> consumer, @NotNull T payload) {
        for (AIChatListener listener : listeners.toArray(AIChatListener[]::new)) {
            try {
                consumer.accept(listener, payload);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    public void notifyMessageChunkReceived(
        @NotNull AIChatConversation conversation,
        @NotNull AIChatMessage message,
        @NotNull String chunk
    ) {
        this.notifyListeners(
            (listener, payload) -> listener.messageChunkReceived(conversation, message, payload),
            chunk
        );
    }

    public void notifyMessageAdd(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message) {
        this.notifyListeners(
            (aiChatListener, message1) ->
                aiChatListener.messageAdded(conversation, message1), message
        );
    }

    public void notifyMessageRemove(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message) {
        final int index = conversation.getMessages().indexOf(message);
        if (index >= 0) {
            notifyMessagesRemove(conversation, conversation.getMessages().subList(index, index + 1));
        }
        conversation.removeMessage(message);
    }

    public void notifyConversationRenamed(@NotNull AIChatConversation conversation, @NotNull String newName) {
        this.notifyListeners(
            (aiChatListener, message) ->
                aiChatListener.conversationRenamed(conversation, newName), newName
        );
    }

    public void notifyMessagesRemove(@NotNull AIChatConversation conversation, @NotNull AIChatMessage afterInclusive) {
        final int index = conversation.getMessages().indexOf(afterInclusive);
        if (index >= 0) {
            notifyMessagesRemove(conversation, conversation.getMessages().subList(index, conversation.getMessages().size()));
        }
    }

    private void notifyMessagesRemove(@NotNull AIChatConversation conversation, @NotNull List<AIChatMessage> view) {
        for (int i = view.size(); i > 0; i--) {
            this.notifyListeners(
                (aiChatListener, message) ->
                    aiChatListener.messageRemoved(conversation, message), view.get(i - 1)
            );
        }
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        if (this.busy != busy) {
            this.busy = busy;
            this.notifyListeners(AIChatListener::busyChanged, busy);
        }
    }

    @NotNull
    public AIAssistant getAssistant() {
        if (assistant == null) {
            assistant = AIAssistantRegistry.getInstance().getAssistant(workspace);
        }
        return assistant;
    }

    public void saveConversationSettings(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIChatConversation conversation
    ) throws DBException {
        saveConversation(monitor, conversation);
    }

    /**
     * Submits conversation and returns future with string result.
     */
    @NotNull
    public synchronized CompletableFuture<AIChatConversation> submitConversation(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AIContextSettings settings,
        @NotNull AIChatConversation conversation,
        @Nullable AIConfirmation confirmation
    ) throws DBException {
        if (!AIUtils.hasValidConfiguration()) {
            throw new DBException("Invalid AI engine configuration");
        }
        final AIChatResponseConsumer subscriber = new AIChatSessionResponseConsumer(
            this,
            conversation);

        return processAICompletion(monitor, conversation, subscriber, settings, confirmation);
    }

    /**
     * Adds declined function calls to the next model request without mutating the visible conversation.
     */
    public void declineFunctionCalls(
        @NotNull AIChatConversation conversation,
        @NotNull List<AIFunctionCall> declinedCalls
    ) {
        if (declinedCalls.isEmpty()) {
            return;
        }

        conversation.addPendingDeclinedFunctionCallMessages(createDeclinedFunctionCallMessages(declinedCalls));
    }

    /**
     * Send prompt to LLM or confirm previous action
     */
    @NotNull
    public CompletableFuture<AIChatConversation> processAICompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIChatConversation conversation,
        @NotNull AIChatResponseConsumer chatListener,
        @Nullable AIContextSettings settings,
        @Nullable AIConfirmation confirmation
    ) throws DBException {
        String sessionId = sessionIdProvider.getSessionId(monitor);
        String engineId = AISettingsManager.getInstance().getSettings().activeEngine();
        QuotaStatus quotaStatus = quotaService.getUserQuotaStatus(
            sessionId,
            engineId
        );
        if (quotaStatus.exceeded()) {
            throw new DBException(
                "Token quota for " +
                    engineId +
                    " exceeded. The quota will be reset at " +
                    USER_DATE_TIME_FORMATTER
                        .format(quotaStatus.resetAt())
            );
        }

        AIPromptGenerator promptGenerator = conversation.getPromptGenerator();
        AIDatabaseContext context;
        try {
            context = settings == null ? null : createDatabaseContext(monitor, promptGenerator, settings);
        } catch (DBException e) {
            chatListener.warning("Failed to connect to the database");
            return finishConversationWithError(conversation, chatListener, e);
        }
        if (isContextChanged(conversation.getId(), context) && !conversation.isTemporary()) {
            storage.saveConversation(
                sessionId,
                QMAIChatHistoryMapper.toQMAIChatHistory(
                    conversation,
                    settings,
                    context
                )
            );
        }

        List<AIMessage> messages = new ArrayList<>(conversation.getMessages().stream()
            .map(AIChatMessage::message)
            .filter(m -> !m.getRole().isLocal())
            .toList());
        messages.addAll(getDeclinedFunctionCallMessages(conversation, confirmation));
        var request = new AIChatRequest(
            context,
            messages,
            confirmation
        );
        try {
            return getAssistant().generateTextStream(
                monitor,
                this,
                conversation,
                request,
                chatListener
            );
        } catch (DBException e) {
            return finishConversationWithError(conversation, chatListener, e);
        }
    }

    @NotNull
    private List<AIMessage> getDeclinedFunctionCallMessages(
        @NotNull AIChatConversation conversation,
        @Nullable AIConfirmation confirmation
    ) {
        List<AIMessage> messages = conversation.getPendingDeclinedFunctionCallMessages();
        if (messages.isEmpty()) {
            return List.of();
        }
        if (shouldConsumeDeclinedFunctionCallMessages(confirmation)) {
            conversation.clearPendingDeclinedFunctionCallMessages();
        }
        return messages;
    }

    private boolean shouldConsumeDeclinedFunctionCallMessages(@Nullable AIConfirmation confirmation) {
        if (confirmation == null) {
            return true;
        }
        if (confirmation instanceof AIFunctionCallConfirmation fcc) {
            return fcc.hasInformationFunctions(getAssistant().getToolboxManager());
        }
        return true;
    }

    @NotNull
    private static List<AIMessage> createDeclinedFunctionCallMessages(@NotNull List<AIFunctionCall> declinedCalls) {
        List<AIMessage> messages = new ArrayList<>(declinedCalls.size() + 1);
        messages.add(AIMessage.systemMessage("The user declined the following function call(s): " +
            String.join(", ", declinedCalls.stream().map(AIFunctionCall::getFunctionName).toList()) +
            ". Continue without requesting them again for the current user request."));
        for (AIFunctionCall functionCall : declinedCalls) {
            messages.add(AIMessage.functionCall(
                functionCall,
                new AIFunctionResult(AIFunctionType.ACTION, DECLINED_FUNCTION_CALL_MESSAGE)
            ));
        }
        return messages;
    }

    @Nullable
    private AIDatabaseContext createDatabaseContext(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIPromptGenerator promptGenerator,
        @NotNull AIContextSettings settings
    ) throws DBException {
        DBPDataSourceContainer container = settings.getDataSourceContainer();
        if (container == null) {
            return null;
        }
        if (!container.isConnected()) {
            boolean connected = container.connect(monitor, true, true);
            if (!connected || !container.isConnected()) {
                String connectionError = container.getConnectionError();
                throw new DBException("Error connecting to data source '" + container.getName() + "'" +
                    (connectionError == null || connectionError.isBlank() ? "" : ": " + connectionError));
            }
        }
        DBCExecutionContext executionContext = contextProvider.getExecutionContext(container);
        DBSLogicalDataSource lgDataSource = DBSLogicalDataSource.createLogicalDataSource(container, executionContext);

        updateScopeSettingsIfNeeded(settings, container);
        assert settings.getScope() != null;
        AIDatabaseContext.Builder builder = new AIDatabaseContext.Builder(lgDataSource).setScope(settings.getScope());
        if (executionContext != null) {
            builder.setExecutionContext(executionContext);
        }
        if (settings.getScope() == AIDatabaseScope.CUSTOM) {
            DBPDataSource dataSource = container.getDataSource();
            if (dataSource != null) {
                String[] customObjectIds = settings.getCustomObjectIds();
                if (!ArrayUtils.isEmpty(customObjectIds)) {
                    Set<String> idSet = new LinkedHashSet<>();
                    Collections.addAll(idSet, customObjectIds); // Duplicates are allowed - we cannot use Set.of here
                    builder.setCustomEntities(AITextUtils.loadCustomEntities(monitor, dataSource, idSet));
                }
            }
        }
        return promptGenerator.configureDatabaseContext(builder).build();
    }

    @NotNull
    private static CompletableFuture<AIChatConversation> finishConversationWithError(
        @NotNull AIChatConversation conversation,
        @NotNull AIChatResponseConsumer chatListener,
        @NotNull Throwable error
    ) {
        try {
            chatListener.error(error);
        } finally {
            chatListener.complete(List.of(), true);
        }
        return CompletableFuture.completedFuture(conversation);
    }

    public void updateScopeSettingsIfNeeded(@NotNull AIContextSettings settings, @NotNull DBPDataSourceContainer container) {
        if (settings.getScope() != null || !container.isConnected()) {
            return;
        }
        DBCExecutionContext executionContext = contextProvider.getExecutionContext(container);
        AIUtils.updateScopeSettingsIfNeeded(settings, container, executionContext);
    }

    private boolean isContextChanged(
        @NotNull UUID conversationId,
        @Nullable AIDatabaseContext context
    ) {
        if (context == null) {
            return false;
        }

        CRC32 crc32 = new CRC32();
        crc32.update(context.getScope().name().getBytes());

        if (context.getScope() == AIDatabaseScope.CUSTOM && context.getCustomEntities() != null) {
            context.getCustomEntities()
                .stream()
                .filter(Objects::nonNull)
                .map(it -> it.getName().getBytes())
                .forEach(crc32::update);
        }

        long checksum = crc32.getValue();
        Long prevChecksum = conversationToContextChecksum.put(conversationId, checksum);
        return prevChecksum == null || prevChecksum != checksum;
    }

    @Nullable
    private DBPDataSourceContainer getContainer(@Nullable QMAIDataSource dataSource) {
        if (dataSource == null) {
            return null;
        }

        DBPProject project = workspace.getProjectById(dataSource.projectId());
        if (project == null) {
            return null;
        }

        return project.getDataSourceRegistry().getDataSource(dataSource.dataSourceId());
    }

    // Chat session provider
    public interface SessionIdProvider {
        @NotNull
        String getSessionId(@NotNull DBRProgressMonitor monitor) throws DBException;
    }

    private static boolean isStatsLoggingEnabled() {
        return AISettingsManager.getInstance()
            .getSettings()
            .getProperty(AIConstants.LOG_STATS_PROPERTY, false);
    }

    private void logConversationStats(@NotNull UUID conversationId) throws DBException {
        List<QMAIMessageMeta> conversationHistoryMeta = storage
            .getConversationHistoryMeta(conversationId);
        if (conversationHistoryMeta.isEmpty()) {
            return;
        }

        int embeddingTokenCount = 0;
        int totalPromptLength = 0;
        int totalInputTokens = 0;
        int totalOutputTokens = 0;
        int totalCachedTokens = 0;
        Duration generatingResponseDuration = Duration.ZERO;
        Duration embeddingDuration = Duration.ZERO;

        for (QMAIMessageMeta qmaiMessageMeta : conversationHistoryMeta) {
            switch (qmaiMessageMeta.type()) {
                case AIMetaTypes.PROMPT -> {
                    totalPromptLength = totalPromptLength + qmaiMessageMeta.systemPromptLength();
                    totalInputTokens = totalInputTokens + qmaiMessageMeta.totalInputTokens();
                    totalOutputTokens = totalOutputTokens + qmaiMessageMeta.totalOutputTokens();
                    totalCachedTokens = totalCachedTokens + qmaiMessageMeta.cachedTokens();
                    generatingResponseDuration = generatingResponseDuration.plus(qmaiMessageMeta.timeSpent());
                }
                case AIMetaTypes.EMBEDDING -> {
                    embeddingTokenCount = embeddingTokenCount + qmaiMessageMeta.totalInputTokens();
                    embeddingDuration = embeddingDuration.plus(qmaiMessageMeta.timeSpent());
                }
                default -> {
                    // Do nothing?
                }
            }
        }

        String template = """
            Conversation Total Stats:
            Embedding tokens: %d
            Embedding duration: %s
            Total prompt length: %d
            Total input tokens: %d
            Total output tokens: %d
            Total cached tokens: %d
            Generating response duration: %s
            
            Last Message Stats:
            %s
            """;

        log.debug(
            String.format(
                template,
                embeddingTokenCount,
                embeddingDuration,
                totalPromptLength,
                totalInputTokens,
                totalOutputTokens,
                totalCachedTokens,
                generatingResponseDuration,
                conversationHistoryMeta.getLast()
            )
        );
    }

    private class ChatSessionListener implements AIChatListener {
        @NotNull
        private final AIChatStorage storage;

        public ChatSessionListener(@NotNull AIChatStorage storage) {
            this.storage = storage;
        }

        @Override
        public void messageAdded(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message) {
            RuntimeUtils.runTask(
                monitor -> {
                    try {
                        saveConversation(monitor, conversation);
                        if (isStatsLoggingEnabled()) {
                            logConversationStats(conversation.getId());
                        }
                    } catch (Exception e) {
                        log.error("Error appending message to chat storage", e);
                    }
                }, "Save AI conversation", 10000
            );
        }

        @Override
        public void messageRemoved(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message) {
            if (conversation.isTemporary()) {
                return;
            }

            try {
                storage.deleteMessage(conversation.getId().toString(), message.id());
            } catch (DBException e) {
                log.error("Error appending message to chat storage", e);
            }
        }

        @Override
        public void conversationRenamed(@NotNull AIChatConversation conversation, @NotNull String newName) {
            if (conversation.isTemporary()) {
                return;
            }

            try {
                storage.renameConversation(conversation.getId().toString(), newName);
            } catch (DBException e) {
                log.error("Error renaming conversation", e);
            }
        }
    }
}
