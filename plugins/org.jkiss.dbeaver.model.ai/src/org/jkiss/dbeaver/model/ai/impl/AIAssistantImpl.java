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
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.metadata.MetadataProcessor;
import org.jkiss.dbeaver.model.ai.prompt.AIPromptBuilder;
import org.jkiss.dbeaver.model.ai.prompt.AIPromptFormatter;
import org.jkiss.dbeaver.model.ai.registry.AIAssistantRegistry;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AIFormatterRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsRegistry;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.ai.utils.ThrowableSupplier;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;

import java.util.List;
import java.util.concurrent.Flow;

public class AIAssistantImpl implements AIAssistant {
    private static final Log log = Log.getLog(AIAssistantImpl.class);

    private static final int MAX_RETRIES = 3;

    private final AISettingsRegistry settingsRegistry = AISettingsRegistry.getInstance();
    private final AIEngineRegistry engineRegistry = AIEngineRegistry.getInstance();
    private final AIFormatterRegistry formatterRegistry = AIFormatterRegistry.getInstance();
    private final AIAssistantRegistry assistantRegistry = AIAssistantRegistry.getInstance();
    private static final MetadataProcessor metadataProcessor = MetadataProcessor.INSTANCE;

    /**
     * Translate the specified text to SQL.
     *
     * @param monitor the progress monitor
     * @param request the translate request
     * @return the translated SQL
     * @throws DBException if an error occurs
     */
    @NotNull
    @Override
    public String translateTextToSql(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AITranslateRequest request
    ) throws DBException {
        AICompletionEngine engine = request.engine() != null ?
            request.engine() :
            getActiveEngine();

        AIChatMessage userMessage = new AIChatMessage(AIChatRole.USER, request.text());

        String prompt = buildPrompt(
            monitor,
            engine,
            request.context()
        ).addGoals(
            "Translate natural language text to SQL."
        ).addOutputFormats(
            "Place any explanation or comments before the SQL code block.",
            "Provide the SQL query in a fenced Markdown code block."
        ).build();

        List<AIChatMessage> chatMessages = List.of(
            AIChatMessage.systemMessage(prompt),
            userMessage
        );

        AICompletionRequest completionRequest = new AICompletionRequest(
            AIUtils.truncateMessages(true, chatMessages, engine.getMaxContextSize(monitor))
        );

        AICompletionResponse completionResponse = requestCompletion(engine, monitor, completionRequest);

        MessageChunk[] messageChunks = processAndSplitCompletion(
            monitor,
            request.context(),
            completionResponse.choices().get(0).text()
        );

        return AITextUtils.convertToSQL(
            userMessage,
            messageChunks,
            request.context().getExecutionContext().getDataSource()
        );
    }

    /**
     * Translate the specified user command to SQL.
     *
     * @param monitor the progress monitor
     * @param request the command request
     * @return the command result
     * @throws DBException if an error occurs
     */
    @NotNull
    @Override
    public AICommandResult command(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AICommandRequest request
    ) throws DBException {
        AICompletionEngine engine = request.engine() != null ?
            request.engine() :
            getActiveEngine();

        String prompt = buildPrompt(
            monitor,
            engine,
            request.context()
        ).addGoals(
            "Translate natural language text to SQL."
        ).addOutputFormats(
            "Place any explanation or comments before the SQL code block.",
            "Provide the SQL query in a fenced Markdown code block."
        ).build();

        List<AIChatMessage> chatMessages = List.of(
            AIChatMessage.systemMessage(prompt),
            AIChatMessage.userMessage(request.text())
        );

        AICompletionRequest completionRequest = new AICompletionRequest(
            AIUtils.truncateMessages(true, chatMessages, engine.getMaxContextSize(monitor))
        );

        AICompletionResponse completionResponse = requestCompletion(engine, monitor, completionRequest);

        MessageChunk[] messageChunks = processAndSplitCompletion(
            monitor,
            request.context(),
            completionResponse.choices().get(0).text()
        );

        String finalSQL = null;
        StringBuilder messages = new StringBuilder();
        for (MessageChunk chunk : messageChunks) {
            if (chunk instanceof MessageChunk.Code code) {
                finalSQL = code.text();
            } else if (chunk instanceof MessageChunk.Text textChunk) {
                messages.append(textChunk.text());
            }
        }
        return new AICommandResult(finalSQL, messages.toString());
    }

    /**
     * Check if the AI assistant has valid configuration.
     *
     * @return true if the AI assistant has valid configuration, false otherwise
     * @throws DBException if an error occurs
     */
    @Override
    public boolean hasValidConfiguration() throws DBException {
        return getActiveEngine().hasValidConfiguration();
    }

    protected MessageChunk[] processAndSplitCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AICompletionContext context,
        @NotNull String completion
    ) throws DBException {
        String processedCompletion = AIUtils.processCompletion(
            monitor,
            context.getExecutionContext(),
            context.getScopeObject(),
            completion,
            formatter(),
            true
        );

        return AITextUtils.splitIntoChunks(
            SQLUtils.getDialectFromDataSource(context.getExecutionContext().getDataSource()),
            processedCompletion
        );
    }

    private static <T> T callWithRetry(ThrowableSupplier<T, DBException> supplier) throws DBException {
        int retry = 0;
        while (retry < MAX_RETRIES) {
            try {
                return supplier.get();
            } catch (TooManyRequestsException e) {
                retry++;
            }
        }
        throw new DBException("Request failed after " + MAX_RETRIES + " attempts");
    }

    protected AICompletionEngine getActiveEngine() throws DBException {
        return engineRegistry.getCompletionEngine(settingsRegistry.getSettings().activeEngine());
    }

    protected AICompletionResponse requestCompletion(
        @NotNull AICompletionEngine engine,
        @NotNull DBRProgressMonitor monitor,
        @NotNull AICompletionRequest request
    ) throws DBException {
        try {
            if (engine.isLoggingEnabled()) {
                log.debug("Requesting completion [request=" + request + "]");
            }

            AICompletionResponse completionResponse = callWithRetry(() -> engine.requestCompletion(monitor, request));

            if (engine.isLoggingEnabled()) {
                log.debug("Received completion [response=" + completionResponse + "]");
            }

            return completionResponse;
        } catch (Exception e) {
            log.error("Error requesting completion", e);

            if (e instanceof DBException) {
                throw (DBException) e;
            } else {
                throw new DBException("Error requesting completion", e);
            }
        }
    }

    protected Flow.Publisher<AICompletionChunk> requestCompletionStream(
        @NotNull AICompletionEngine engine,
        @NotNull DBRProgressMonitor monitor,
        @NotNull AICompletionRequest request
    ) throws DBException {
        try {
            Flow.Publisher<AICompletionChunk> publisher = callWithRetry(() -> engine.requestCompletionStream(monitor, request));
            boolean loggingEnabled = engine.isLoggingEnabled();

            return subscriber -> {
                if (loggingEnabled) {
                    log.debug("Requesting completion stream [request=" + request + "]");
                    publisher.subscribe(new LogSubscriber(log, subscriber));
                } else {
                    publisher.subscribe(subscriber);
                }
            };
        } catch (Exception e) {
            log.error("Error requesting completion stream", e);

            if (e instanceof DBException) {
                throw (DBException) e;
            } else {
                throw new DBException("Error requesting completion stream", e);
            }
        }
    }

    protected AIPromptFormatter formatter() throws DBException {
        return formatterRegistry.getFormatter(AIConstants.CORE_FORMATTER);
    }

    protected AIAssistant assistant() throws DBException {
        return assistantRegistry.getAssistant();
    }

    protected AIPromptBuilder buildPrompt(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AICompletionEngine engine,
        @Nullable AICompletionContext context
    ) throws DBException {
        AIPromptBuilder promptBuilder = AIPromptBuilder.createForDataSource(
            context != null ?
                context.getExecutionContext().getDataSource() :
                null,
            formatter()
        );

        describeDatabaseMetadata(monitor, engine, context, promptBuilder);

        return promptBuilder;
    }

    protected void describeDatabaseMetadata(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AICompletionEngine engine,
        @Nullable AICompletionContext context,
        AIPromptBuilder promptBuilder
    ) throws DBException {
        if (context != null) {
            String description = metadataProcessor.describeContext(
                monitor,
                context,
                formatter(),
                AIUtils.getMaxRequestTokens(engine, monitor)
            );

            promptBuilder.addDatabaseSnapshot(description);
        }
    }
}
