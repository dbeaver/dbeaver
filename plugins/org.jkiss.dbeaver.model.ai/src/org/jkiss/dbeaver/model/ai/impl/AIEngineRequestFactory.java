/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIEngineRequest;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    private final AIDatabaseSnapshotService databaseSnapshotService;
    private final TokenCounter tokenCounter;

    public AIEngineRequestFactory(
        @NotNull AIDatabaseSnapshotService databaseSnapshotService,
        @NotNull TokenCounter tokenCounter
    ) {
        this.databaseSnapshotService = databaseSnapshotService;
        this.tokenCounter = tokenCounter;
    }

    public AIEngineRequest build(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngine engine,
        @NotNull AIEngineDescriptor engineDescriptor,
        @NotNull AIPromptGenerator systemPromptGenerator,
        @Nullable AIDatabaseContext databaseContext,
        @NotNull List<AIMessage> messages
    ) throws DBException {
        String systemPrompt = systemPromptGenerator.build();

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

        // Build DB snapshot

        String dbSnapshot = "";
        boolean isContextTruncated = false;
        if (databaseContext != null && dbSnapshotTokenBudget > 0) {
            AISchemaGenerationOptions ddlOptions = buildOptions(dbSnapshotTokenBudget);
            AIDatabaseSnapshotService.TokenBoundedStringBuilder dbSnapshotBuilder = databaseSnapshotService.createDbSnapshot(monitor, databaseContext, ddlOptions);
            if (dbSnapshotBuilder != null) {
                dbSnapshot = dbSnapshotBuilder.toString();
                isContextTruncated = dbSnapshotBuilder.isTruncated();
            }
        }

        // Compose system message

        String fullSystemPrompt = dbSnapshot.isBlank()
            ? systemPrompt
            : systemPrompt + "\n" + DB_SNAPSHOT_SECTION_HEADER + dbSnapshot;

        AIMessage systemMessage = AIMessage.systemMessage(fullSystemPrompt);

        // Truncate chat to fit the window

        ChatTruncator chatTruncator = ChatTruncator.builder()
            .maxTokens(maxContextWindowSize)
            .reserveForSystem(systemPromptTokenBudget)
            .reserveForReply(REPLY_TOKEN_RESERVE)
            .reserveForOverhead(OVERHEAD_TOKEN_RESERVE)
            .tokenCounter(tokenCounter)
            .build();

        List<AIMessage> allMessages = new ArrayList<>(1 + messages.size());
        allMessages.add(systemMessage);
        allMessages.addAll(messages);

        List<AIMessage> truncated = chatTruncator.truncate(allMessages);
        AIEngineRequest request = new AIEngineRequest(truncated);
        request.setWasPromptTruncated(isContextTruncated);

        determineRequestTools(monitor, engineDescriptor, systemPromptGenerator, request);

        return request;
    }

    protected void determineRequestTools(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineDescriptor engineDescriptor,
        @NotNull AIPromptGenerator systemPromptGenerator,
        @NotNull AIEngineRequest request
    ) {
        if (!engineDescriptor.isSupportsFunctions()) {
            return;
        }
        List<AIFunctionDescriptor> functions = new ArrayList<>();
        for (AIFunctionDescriptor fd : AIFunctionRegistry.getInstance().getAllFunctions()) {
            if (fd.isGlobal() || fd.isApplicable(engineDescriptor, systemPromptGenerator)) {
                functions.add(fd);
            }
        }

        AISettings aiSettings = AISettingsManager.getInstance().getSettings();
        Set<String> enabledFunctions = aiSettings.getEnabledFunctions();
        Set<String> enabledFunctionCategories = aiSettings.getEnabledFunctionCategories();
        functions.removeIf(aiFunctionDescriptor ->
            !enabledFunctions.contains(aiFunctionDescriptor.getId()) &&
                !enabledFunctionCategories.contains(aiFunctionDescriptor.getCategoryId())
        );

        request.setFunctions(functions);
    }


    private static int getContextWindowSize(@NotNull DBRProgressMonitor monitor, @NotNull AIEngine engine) {
        try {
            return engine.getContextWindowSize(monitor);
        } catch (DBException e) {
            log.debug("Cannot determine engine " + engine + " context window size. Set to default " +
                AIConstants.DEFAULT_CONTEXT_WINDOW_SIZE, e);
            return AIConstants.DEFAULT_CONTEXT_WINDOW_SIZE;
        }
    }

    protected AISchemaGenerationOptions buildOptions(int dbSnapshotTokenBudget) {
        DBPPreferenceStore prefs = DBWorkbench.getPlatform().getPreferenceStore();

        return AISchemaGenerationOptions.builder()
            .withMaxDbSnapshotTokens(dbSnapshotTokenBudget)
            .withSendObjectComment(prefs.getBoolean(AIConstants.AI_SEND_DESCRIPTION))
            .withSendColumnTypes(prefs.getBoolean(AIConstants.AI_SEND_TYPE_INFO))
            .build();

    }
}
