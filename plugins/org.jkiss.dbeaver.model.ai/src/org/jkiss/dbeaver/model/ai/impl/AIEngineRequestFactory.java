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
package org.jkiss.dbeaver.model.ai.impl;

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseCapabilities;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIEngineRequest;
import org.jkiss.dbeaver.model.ai.functions.AIFunctionParams;
import org.jkiss.dbeaver.model.ai.registry.AIPromptGeneratorDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIPromptGeneratorRegistry;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class AIEngineRequestFactory {
    private static final Log log = Log.getLog(AIEngineRequestFactory.class);

    // Section header used before the DB snapshot inside the system prompt
    private static final String DB_SNAPSHOT_SECTION_HEADER = "Database snapshot:\n";

    // Percentage of remaining context tokens allocated to system prompt + snapshot
    private static final int SYSTEM_PROMPT_TOKEN_BUDGET_PERCENT = 80;

    // Reserved tokens that must remain for the model's reply
    private static final int REPLY_TOKEN_RESERVE = 2000;

    // Reserved tokens for overhead (API limits, formatting, metadata, etc.)
    private static final int OVERHEAD_TOKEN_RESERVE = 100;

    private final TokenCounter tokenCounter;

    public record RequestFunctions(
        @NotNull Collection<AIFunctionDescriptor> autoFunctions,
        @NotNull Collection<AIFunctionDescriptor> supportedFunctions
    ) {
        private RequestFunctions() {
            this(Set.of(), Set.of());
        }
    }

    public AIEngineRequestFactory(@NotNull TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }

    @NotNull
    public AIEngineRequest build(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIAssistant assistant,
        @NotNull AIConfigurationProfile profile,
        @NotNull AIEngine<?> engine,
        @NotNull AIFunctionContext functionContext,
        @NotNull List<AIMessage> messages
    ) throws DBException {
        if (messages.stream().anyMatch(message -> !message.getImages().isEmpty()) && !engine.supportsImageInput(monitor)) {
            throw new DBException("The selected AI model does not support images. Select a model with image input support.");
        }
        AIPromptGenerator promptGenerator = functionContext.getPrompt();
        AIDatabaseContext databaseContext = functionContext.getContext();
        String systemPrompt = promptGenerator.build(assistant, databaseContext);
        AIChatConversation conversation = functionContext.getConversation();
        if (conversation != null && conversation.getDataSources().size() > 1) {
            systemPrompt += describeConnections(functionContext);
        }

        final RequestFunctions requestFunctions = determineRequestTools(
            assistant,
            profile,
            functionContext,
            messages.stream().filter(aiMessage -> aiMessage.getRole() == AIMessageType.USER).count()
        );

        // Tokens available for user/system/chat history after we reserve reply + overhead
        int maxContextWindowSize = getContextWindowSize(monitor, engine);
        int availableContextTokens = maxContextWindowSize - REPLY_TOKEN_RESERVE - OVERHEAD_TOKEN_RESERVE;
        if (availableContextTokens < 0) {
            availableContextTokens = 0; // clamp, just in case caller gave a tiny window
        }

        // Max tokens allowed for the system prompt (including DB snapshot section)
        int systemPromptTokenBudget = availableContextTokens * SYSTEM_PROMPT_TOKEN_BUDGET_PERCENT / 100;

        // Pre-calc token counts
        int snapshotHeaderTokenCount = tokenCounter.count(DB_SNAPSHOT_SECTION_HEADER);
        int systemPromptTokenCount = tokenCounter.count(systemPrompt);

        // Remaining budget specifically for DB snapshot (excludes the header & base system prompt)
        int dbSnapshotTokenBudget = systemPromptTokenBudget - systemPromptTokenCount - snapshotHeaderTokenCount;
        if (dbSnapshotTokenBudget < 0) {
            dbSnapshotTokenBudget = 0;
        }

        String dbSnapshot = "";
        boolean isContextTruncated = false;

        // Build full DB snapshot (t) in first prompt if engine doesn't support functions
        // (functions provide smart context read)
        if (databaseContext != null && dbSnapshotTokenBudget > 0) {
            AIDatabaseSnapshotService databaseSnapshotService = new AIDatabaseSnapshotService();

            boolean functionsEnabled = isFunctionsEnabled(assistant, profile);
            AIDatabaseSnapshotService.TokenBoundedStringBuilder dbSnapshotBuilder = databaseSnapshotService.createDbSnapshot(
                monitor,
                databaseContext,
                functionsEnabled,
                dbSnapshotTokenBudget
            );
            if (dbSnapshotBuilder != null) {
                dbSnapshot = dbSnapshotBuilder.build();
                isContextTruncated = dbSnapshotBuilder.isTruncated();
            }
        }

        // Compose system message
        String fullSystemPrompt = dbSnapshot.isBlank()
            ? systemPrompt
            : systemPrompt + "\n" + DB_SNAPSHOT_SECTION_HEADER + dbSnapshot;

        AIMessage systemMessage = AIMessage.systemMessage(fullSystemPrompt);

        // Truncate chat to fit the window

        Map<AIImageAttachment, Integer> imageTokenCounts = new IdentityHashMap<>();
        ChatTruncator chatTruncator = ChatTruncator.builder()
            .maxTokens(maxContextWindowSize)
            .reserveForSystem(systemPromptTokenBudget)
            .reserveForReply(REPLY_TOKEN_RESERVE)
            .reserveForOverhead(OVERHEAD_TOKEN_RESERVE)
            .tokenCounter(tokenCounter)
            .imageTokenCounter(image -> imageTokenCounts.computeIfAbsent(image, engine::estimateImageTokens))
            .build();

        List<AIMessage> allMessages = new ArrayList<>(1 + messages.size());
        allMessages.add(systemMessage);
        allMessages.addAll(messages);

        List<AIMessage> truncated = chatTruncator.tryTruncate(allMessages);
        List<AIMessage> toSend = truncated != null ? truncated : allMessages;
        AIEngineRequest request = new AIEngineRequest(toSend);
        request.setWasPromptTruncated(isContextTruncated || truncated != null);
        request.setFunctions(new ArrayList<>(requestFunctions.supportedFunctions()));

        return request;
    }

    @NotNull
    static String describeConnections(@NotNull AIFunctionContext context) {
        AIChatConversation conversation = context.getConversation();
        if (conversation == null) {
            return "";
        }
        StringBuilder connections = new StringBuilder("\nAvailable conversation connections (JSON):\n");
        Gson gson = new Gson();
        for (var container : conversation.getDataSources()) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("connectionId", container.getId());
            details.put("name", container.getName());
            details.put("primary", container == conversation.getDataSource());
            details.put("connected", container.isConnected());
            details.put("metadataAvailable", false);
            details.put("supportsCatalogs", "unknown");
            details.put("supportsSchemas", "unknown");
            if (container.isConnected()) {
                try {
                    AIFunctionContext selected = context.withConnection(container.getId());
                    AIDatabaseContext database = selected.getContext();
                    if (database != null) {
                        details.put("metadataAvailable", true);
                        var execution = database.getExecutionContext();
                        details.put("dialect", execution.getDataSource().getSQLDialect().getDialectName());
                        AIDatabaseCapabilities capabilities = AIDatabaseCapabilities.read(selected);
                        details.put("supportsCatalogs", capabilities.catalogs() == null ? "unknown" : capabilities.catalogs());
                        details.put("supportsSchemas", capabilities.schemas() == null ? "unknown" : capabilities.schemas());
                        var defaults = execution.getContextDefaults();
                        if (defaults != null) {
                            var catalog = defaults.getDefaultCatalog();
                            if (catalog != null && AIUtils.isCatalogInScope(database, execution, catalog)) {
                                details.put("defaultCatalog", catalog.getName());
                            }
                            var schema = defaults.getDefaultSchema();
                            if (schema != null && AIUtils.isSchemaInScope(database, execution, schema)) {
                                details.put("defaultSchema", schema.getName());
                            }
                        }
                    }
                } catch (DBException exception) {
                    log.debug("Cannot describe conversation connection capabilities", exception);
                }
            }
            connections.append(gson.toJson(details)).append('\n');
        }
        connections.append("Use connectionId to select a connection for database tools. " +
            "An omitted or empty connectionId selects the primary connection. " +
            "Only these connections are available. Each connection has its own dialect, scope and access restrictions. " +
            "The primary connection's dialect and namespace defaults do not apply to other connections. " +
            "Call listSchemaNames only for connections that support schemas. " +
            "Omit schema/catalog parameters when the selected connection does not support them. " +
            "Unknown capabilities do not imply support; never guess schema or catalog names.");
        return connections.toString();
    }

    @NotNull
    static AIFunctionVerifier.FunctionState getRequestFunctionState(
        @NotNull AIFunctionDescriptor function,
        @NotNull AIFunctionContext context
    ) {
        AIFunctionVerifier.FunctionState state = function.getFunctionState(context);
        AIChatConversation conversation = context.getConversation();
        if (state != AIFunctionVerifier.FunctionState.NOT_APPLICABLE || conversation == null
            || function.getParameter(AIFunctionParams.CONNECTION_ID_PARAM) == null) {
            return state;
        }
        for (var container : conversation.getDataSources()) {
            if (container == conversation.getDataSource() || !container.isConnected()) {
                continue;
            }
            try {
                if (function.getFunctionState(context.withConnection(container.getId())) == AIFunctionVerifier.FunctionState.APPLICABLE) {
                    return AIFunctionVerifier.FunctionState.APPLICABLE;
                }
            } catch (DBException exception) {
                log.debug("Cannot check function for conversation connection", exception);
            }
        }
        return state;
    }

    private boolean isFunctionsEnabled(@NotNull AIAssistant assistant, @NotNull AIConfigurationProfile profile) throws DBException {
        if (!assistant.isFunctionSupported(profile)) {
            return false;
        }
        AIToolboxManager toolboxManager = assistant.getToolboxManager();
        AIFunctionSettings functionSettings = toolboxManager.getFunctionSettings();
        return profile.getEngineDescriptor().isSupportsFunctions() && functionSettings.isFunctionsEnabled();
    }

    @NotNull
    protected RequestFunctions determineRequestTools(
        @NotNull AIAssistant assistant,
        @NotNull AIConfigurationProfile profile,
        @NotNull AIFunctionContext functionContext,
        long userMessageCount
    ) throws DBException {
        if (!isFunctionsEnabled(assistant, profile)) {
            return new RequestFunctions();
        }
        AIToolboxManager toolboxManager = assistant.getToolboxManager();
        AIFunctionSettings functionSettings = toolboxManager.getFunctionSettings();

        AIPromptGenerator promptGenerator = functionContext.getPrompt();
        AIPromptGeneratorDescriptor prompt = AIPromptGeneratorRegistry.getInstance()
            .getPromptGenerator(promptGenerator.generatorId());
        if (prompt == null) {
            log.error("Prompt '" + promptGenerator.generatorId() + "' not found. Functions were disabled.");
            return new RequestFunctions();
        }

        List<AIFunctionDescriptor> functions = new ArrayList<>();
        List<AIFunctionDescriptor> autoFunctions = new ArrayList<>();
        for (AIFunctionDescriptor fd : toolboxManager.getAllFunctions(AIFunctionPurpose.TOOL)) {
            AIFunctionVerifier.FunctionState state = getRequestFunctionState(fd, functionContext);
            switch (state) {
                case APPLICABLE -> functions.add(fd);
                case AUTO_CALL -> autoFunctions.add(fd);
            }
        }

        Set<AIFunctionDescriptor> enabledFunctions = new LinkedHashSet<>();
        for (AIToolbox toolbox : toolboxManager.getAllToolboxes()) {
            if (!toolbox.isEnabled() || !toolbox.isAccessible()) {
                continue;
            }
            AIFunctionSettings.ToolboxSettings toolboxSettings = functionSettings.getToolboxSettings(toolbox);
            for (AIFunctionDescriptor function : toolbox.getSupportedFunctions(AIFunctionPurpose.TOOL)) {
                if (toolboxSettings.isFunctionEnabled(function)) {
                    enabledFunctions.add(function);
                }
            }
        }

        Set<AIFunctionDescriptor> selectedFunctions = new LinkedHashSet<>(functions);
        selectedFunctions.removeIf(aiFunctionDescriptor ->
            !enabledFunctions.contains(aiFunctionDescriptor)
        );

        Set<String> requiredByDeps = resolveDependencies(selectedFunctions);

        if (!requiredByDeps.isEmpty()) {
            for (AIFunctionDescriptor f : functions) {
                if (requiredByDeps.contains(f.getId())) {
                    selectedFunctions.add(f);
                }
            }
        }

        if (!prompt.isSupportsActions(userMessageCount) || !promptGenerator.supportsUIAndActionFunctions()) {
            // Filter out actions
            selectedFunctions.removeIf(fd -> fd.getType() == AIFunctionType.ACTION);
        }
        if (!prompt.isSupportsUi(userMessageCount) || !promptGenerator.supportsUIAndActionFunctions()) {
            // Filter out ui functions
            selectedFunctions.removeIf(AIFunctionDescriptor::isUI);
        }

        return new RequestFunctions(autoFunctions, selectedFunctions);
    }


    private static int getContextWindowSize(@NotNull DBRProgressMonitor monitor, @NotNull AIEngine<?> engine) {
        try {
            return engine.getContextWindowSize(monitor);
        } catch (DBException e) {
            log.debug("Cannot determine engine " + engine.getClass().getSimpleName() +
                " context window size. Set to default " +
                AIConstants.DEFAULT_CONTEXT_WINDOW_SIZE, e);
            return AIConstants.DEFAULT_CONTEXT_WINDOW_SIZE;
        }
    }

    /**
     * Resolves transitive dependencies for the given list of already selected function descriptors.
     */
    @NotNull
    private static Set<String> resolveDependencies(@NotNull Set<AIFunctionDescriptor> selected) {
        Set<String> result = new HashSet<>();
        for (AIFunctionDescriptor fd : selected) {
            collectDependencies(fd.getToolbox(), fd.getDependsOn(), result);
        }
        return result;
    }

    private static void collectDependencies(
        @NotNull AIToolbox toolbox,
        @NotNull String[] dependencies,
        @NotNull Set<String> result
    ) {
        for (String depId : dependencies) {
            if (CommonUtils.isEmpty(depId)) {
                continue;
            }
            if (!result.add(depId)) {
                continue;
            }
            AIFunctionDescriptor dep = toolbox.getFunctionById(depId);
            if (dep != null) {
                collectDependencies(toolbox, dep.getDependsOn(), result);
            }
        }
    }
}
