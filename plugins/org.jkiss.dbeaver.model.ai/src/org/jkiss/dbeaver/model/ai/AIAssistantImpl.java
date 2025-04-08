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
import java.util.stream.Stream;

public class AIAssistantImpl implements AIAssistant {
    private static final Log log = Log.getLog(AIAssistantImpl.class);

    private static final int MAX_RETRIES = 3;

    private final AISettingsRegistry settingsRegistry = AISettingsRegistry.getInstance();
    private final AIEngineRegistry engineRegistry = AIEngineRegistry.getInstance();
    private final AIFormatterRegistry formatterRegistry = AIFormatterRegistry.getInstance();
    private final MetadataProcessor metadataProcessor = MetadataProcessor.INSTANCE;

    /**
     * Chat with the AI assistant.
     *
     * @param monitor the progress monitor
     * @param chatCompletionRequest the chat completion request
     * @throws DBException if an error occurs
     */
    @NotNull
    @Override
    public Flow.Publisher<DAICompletionChunk> chat(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAIChatRequest chatCompletionRequest
    ) throws DBException {
        DAICompletionEngine engine = chatCompletionRequest.engine() != null ?
            chatCompletionRequest.engine() :
            getActiveEngine();

        List<DAIChatMessage> chatMessages = Stream.concat(
            Stream.of(
                DAIChatMessage.systemMessage(
                    getSystemPrompt() + System.lineSeparator() + metadataProcessor.describeContext(
                        monitor,
                        chatCompletionRequest.context(),
                        formatter(),
                        engine.getMaxContextSize(monitor) -  AIConstants.MAX_RESPONSE_TOKENS
                    )
                )
            ),
            chatCompletionRequest.messages().stream()
        ).toList();

        List<DAIChatMessage> truncatedMessages = AIUtils.truncateMessages(
            true,
            chatMessages,
            engine.getMaxContextSize(monitor)
        );

        return requestCompletionStream(
            engine,
            monitor,
            new DAICompletionRequest(
                truncatedMessages
            )
        );
    }

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

        List<DAIChatMessage> chatMessages = List.of(
            DAIChatMessage.systemMessage(
                getSystemPrompt() + System.lineSeparator() + metadataProcessor.describeContext(
                    monitor,
                    request.context(),
                    formatter(),
                    engine.getMaxContextSize(monitor) -  AIConstants.MAX_RESPONSE_TOKENS
                )
            ),
            userMessage
        );

        DAICompletionRequest completionRequest = new DAICompletionRequest(
            AIUtils.truncateMessages(true, chatMessages, engine.getMaxContextSize(monitor))
        );

        DAICompletionResponse completionResponse = requestCompletion(engine, monitor, completionRequest);

        MessageChunk[] messageChunks = processAndSplitCompletion(
            monitor,
            request.context(),
            completionResponse.text()
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

        List<DAIChatMessage> chatMessages = List.of(
            DAIChatMessage.systemMessage(
                getSystemPrompt() + System.lineSeparator() + metadataProcessor.describeContext(
                    monitor,
                    request.context(),
                    formatter(),
                    engine.getMaxContextSize(monitor) -  AIConstants.MAX_RESPONSE_TOKENS
                )
            ),
            DAIChatMessage.userMessage(request.text())
        );

        DAICompletionRequest completionRequest = new DAICompletionRequest(
            AIUtils.truncateMessages(true, chatMessages, engine.getMaxContextSize(monitor))
        );

        DAICompletionResponse completionResponse = requestCompletion(engine, monitor, completionRequest);

        MessageChunk[] messageChunks = processAndSplitCompletion(monitor, request.context(), completionResponse.text());

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

    private MessageChunk[] processAndSplitCompletion(
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

    private DAICompletionEngine getActiveEngine() throws DBException {
        return engineRegistry.getCompletionEngine(settingsRegistry.getSettings().getActiveEngine());
    }

    private DAICompletionResponse requestCompletion(
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

    private Flow.Publisher<DAICompletionChunk> requestCompletionStream(
        @NotNull DAICompletionEngine engine,
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request
    ) throws DBException {
        try {
            Flow.Publisher<DAICompletionChunk> publisher = callWithRetry(() -> engine.requestCompletionStream(monitor, request));

            return subscriber -> {
                if (engine.isLoggingEnabled()) {
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

    protected String getSystemPrompt() {
        return """
            You are SQL assistant. You must produce SQL code for given prompt.
            You must produce valid SQL statement enclosed with Markdown code block and terminated with semicolon.
            All database object names should be properly escaped according to the SQL dialect.
            All comments MUST be placed before query outside markdown code block.
            Be polite.
            """;
    }

    private IAIFormatter formatter() throws DBException {
        return formatterRegistry.getFormatter(AIConstants.CORE_FORMATTER);
    }
}
