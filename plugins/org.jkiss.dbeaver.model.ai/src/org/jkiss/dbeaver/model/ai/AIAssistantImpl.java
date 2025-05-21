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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.completion.*;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.ai.metadata.MetadataProcessor;
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
        @NotNull DAITranslateRequest request
    ) throws DBException {
        DAICompletionEngine engine = request.engine() != null ?
            request.engine() :
            getActiveEngine();

        DAIChatMessage userMessage = new DAIChatMessage(DAIChatRole.USER, request.text());

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

        List<DAIChatMessage> chatMessages = List.of(
            DAIChatMessage.systemMessage(prompt),
            userMessage
        );

        DAICompletionRequest completionRequest = new DAICompletionRequest(
            AIUtils.truncateMessages(true, chatMessages, engine.getMaxContextSize(monitor))
        );

        DAICompletionResponse completionResponse = requestCompletion(engine, monitor, completionRequest);

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
    public CommandResult command(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICommandRequest request
    ) throws DBException {
        DAICompletionEngine engine = request.engine() != null ?
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

        List<DAIChatMessage> chatMessages = List.of(
            DAIChatMessage.systemMessage(prompt),
            DAIChatMessage.userMessage(request.text())
        );

        DAICompletionRequest completionRequest = new DAICompletionRequest(
            AIUtils.truncateMessages(true, chatMessages, engine.getMaxContextSize(monitor))
        );

        DAICompletionResponse completionResponse = requestCompletion(engine, monitor, completionRequest);

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
        return new CommandResult(finalSQL, messages.toString());
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
        @NotNull DAICompletionContext context,
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

    protected DAICompletionEngine getActiveEngine() throws DBException {
        return engineRegistry.getCompletionEngine(settingsRegistry.getSettings().activeEngine());
    }

    protected DAICompletionResponse requestCompletion(
        @NotNull DAICompletionEngine engine,
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request
    ) throws DBException {
        try {
            if (engine.isLoggingEnabled()) {
                log.debug("Requesting completion [request=" + request + "]");
            }

            DAICompletionResponse completionResponse = callWithRetry(() -> engine.requestCompletion(monitor, request));

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

    protected Flow.Publisher<DAICompletionChunk> requestCompletionStream(
        @NotNull DAICompletionEngine engine,
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request
    ) throws DBException {
        try {
            Flow.Publisher<DAICompletionChunk> publisher = callWithRetry(() -> engine.requestCompletionStream(monitor, request));
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

    protected IAIFormatter formatter() throws DBException {
        return formatterRegistry.getFormatter(AIConstants.CORE_FORMATTER);
    }

    protected AIAssistant assistant() throws DBException {
        return assistantRegistry.getAssistant();
    }

    protected PromptBuilder buildPrompt(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionEngine engine,
        @Nullable DAICompletionContext context
    ) throws DBException {
        PromptBuilder promptBuilder = PromptBuilder.createForDataSource(
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
        @NotNull DAICompletionEngine engine,
        @Nullable DAICompletionContext context,
        PromptBuilder promptBuilder
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
