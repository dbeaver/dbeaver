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
import org.jkiss.dbeaver.model.ai.registry.*;
import org.jkiss.dbeaver.model.ai.utils.ThrowableSupplier;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.concurrent.Flow;

public class AIAssistantImpl implements AIAssistant {
    private static final Log log = Log.getLog(AIAssistantImpl.class);

    private static final int MANY_REQUESTS_RETRIES = 3;
    private static final int MANY_REQUESTS_TIMEOUT = 500;

    protected final AISettingsRegistry settingsRegistry;
    protected final AIEngineRegistry engineRegistry;
    protected final AISqlFormatterRegistry formatterRegistry;
    protected final AIEngineRequestFactory requestFactory;

    public AIAssistantImpl() {
        this(
            AISettingsRegistry.getInstance(),
            AIEngineRegistry.getInstance(),
            AISqlFormatterRegistry.getInstance(),
            new AIEngineRequestFactory(
                new AIDatabaseSnapshotService(AISchemaGeneratorRegistry.getInstance()),
                new DummyTokenCounter()
            )
        );
    }

    public AIAssistantImpl(
        AISettingsRegistry settingsRegistry,
        AIEngineRegistry engineRegistry,
        AISqlFormatterRegistry formatterRegistry,
        AIEngineRequestFactory requestFactory
    ) {
        this.settingsRegistry = settingsRegistry;
        this.engineRegistry = engineRegistry;
        this.formatterRegistry = formatterRegistry;
        this.requestFactory = requestFactory;
    }

    @Override
    public void initialize(@NotNull DBPWorkspace workspace) {
        // no-op
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
        @NotNull AITranslateRequest request
    ) throws DBException {
        AIEngine engine = request.engine() != null ?
            request.engine() :
            getActiveEngine();

        AIMessage userMessage = new AIMessage(AIMessageType.USER, request.text());

        AIPromptBuilder promptBuilder = createPromptBuilder();
        promptBuilder
            .addContexts(AIPromptBuilder.describeContext(request.context().getDataSource()))
            .addInstructions(AIPromptBuilder.createInstructionList(request.context().getDataSource()))
            .addGoals(
                "Translate natural language text to SQL."
            )
            .addOutputFormats(
                "Place any explanation or comments before the SQL code block.",
                "Provide the SQL query in a fenced Markdown code block."
            );
        addSqlCompletionInstructions(promptBuilder);
        String prompt = promptBuilder.build();

        AIEngineRequest completionRequest = requestFactory.build(
            monitor,
            prompt,
            request.context(),
            List.of(userMessage),
            engine.getContextWindowSize(monitor)
        );

        AIEngineResponse completionResponse = requestCompletion(engine, monitor, completionRequest);

        MessageChunk[] messageChunks = processAndSplitCompletion(
            monitor,
            request.context(),
            completionResponse.variants().getFirst()
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
        AIEngine engine = request.engine() != null ?
            request.engine() :
            getActiveEngine();

        AIPromptBuilder promptBuilder = createPromptBuilder();
        promptBuilder
            .addContexts(AIPromptBuilder.describeContext(request.context().getDataSource()))
            .addInstructions(AIPromptBuilder.createInstructionList(request.context().getDataSource()))
            .addGoals(
                "Translate natural language text to SQL."
            )
            .addOutputFormats(
                "Place any explanation or comments before the SQL code block.",
                "Provide the SQL query in a fenced Markdown code block."
            );
        addSqlCompletionInstructions(promptBuilder);
        String prompt = promptBuilder.build();

        AIEngineRequest completionRequest = requestFactory.build(
            monitor,
            prompt,
            request.context(),
            List.of(AIMessage.userMessage(request.text())),
            engine.getContextWindowSize(monitor)
        );

        List<AIMessage> chatMessages = List.of(
            AIMessage.systemMessage(prompt),
            AIMessage.userMessage(request.text())
        );

        AIEngineResponse completionResponse = requestCompletion(engine, monitor, completionRequest);

        MessageChunk[] messageChunks = processAndSplitCompletion(
            monitor,
            request.context(),
            completionResponse.variants().getFirst()
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
        AIEngineSettings<?> activeEngineConfiguration = getActiveEngineConfiguration();
        if (activeEngineConfiguration == null) {
            log.warn("No active AI engine configuration found");
            return false;
        }

        return activeEngineConfiguration.isValid();
    }

    protected MessageChunk[] processAndSplitCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIDatabaseContext context,
        @NotNull String completion
    ) throws DBException {
        String processedCompletion = formatterRegistry.getSqlPostProcessor().formatGeneratedQuery(
            monitor,
            context.getExecutionContext(),
            context.getScopeObject(),
            completion
        );

        return AITextUtils.splitIntoChunks(
            SQLUtils.getDialectFromDataSource(context.getExecutionContext().getDataSource()),
            processedCompletion
        );
    }

    private static <T> T callWithRetry(ThrowableSupplier<T, DBException> supplier) throws DBException {
        int retry = 0;
        while (retry < MANY_REQUESTS_RETRIES) {
            try {
                return supplier.get();
            } catch (TooManyRequestsException e) {
                retry++;
                if (retry < MANY_REQUESTS_RETRIES) {
                    log.debug("Too many engine requests. Retry after " + MANY_REQUESTS_TIMEOUT + "ms");
                    RuntimeUtils.pause(MANY_REQUESTS_TIMEOUT);
                }
            }
        }
        throw new DBException("Request failed after " + MANY_REQUESTS_RETRIES + " attempts");
    }

    @NotNull
    @Override
    public AIEngine getActiveEngine() throws DBException {
        return engineRegistry.getCompletionEngine(settingsRegistry.getSettings().activeEngine());
    }

    @Nullable
    @Override
    public AIEngineDescriptor getActiveEngineDescriptor() {
        return engineRegistry.getEngineDescriptor(settingsRegistry.getSettings().activeEngine());
    }

    protected AIEngineResponse requestCompletion(
        @NotNull AIEngine engine,
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        try {
            boolean loggingEnabled = isLoggingEnabled();
            if (loggingEnabled) {
                log.debug("AI request:\n" + CommonUtils.addTextIndent(request.toString(), "\t"));
            }

            AIEngineResponse completionResponse = callWithRetry(() -> engine.requestCompletion(monitor, request));

            if (loggingEnabled) {
                log.debug("AI response:\n" + CommonUtils.addTextIndent(completionResponse.toString(), "\t"));
            }

            return completionResponse;
        } catch (Exception e) {
            if (e instanceof DBException) {
                throw (DBException) e;
            } else {
                throw new DBException("Error requesting completion", e);
            }
        }
    }

    protected Flow.Publisher<AIEngineResponseChunk> requestCompletionStream(
        @NotNull AIEngine engine,
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        try {
            Flow.Publisher<AIEngineResponseChunk> publisher = callWithRetry(() -> engine.requestCompletionStream(monitor, request));
            boolean loggingEnabled = isLoggingEnabled();

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

    protected AIPromptBuilder createPromptBuilder() throws DBException {
        return AIPromptBuilder.create();
    }

    /**
     * Adds any extra instruction for SQL completion
     */
    protected void addSqlCompletionInstructions(AIPromptBuilder promptBuilder) {

    }

    private boolean isLoggingEnabled() throws DBException {
        AIEngineSettings<?> activeEngineConfiguration = getActiveEngineConfiguration();
        if (activeEngineConfiguration == null) {
            log.warn("No active AI engine configuration found");
            return false;
        }

        return activeEngineConfiguration.isLoggingEnabled();
    }

    @Nullable
    private AIEngineSettings<?> getActiveEngineConfiguration() throws DBException {
        String activeEngine = settingsRegistry.getSettings().activeEngine();
        if (activeEngine == null || activeEngine.isEmpty()) {
            log.warn("No active AI engine configured");
            return null;
        }
        return settingsRegistry.getSettings().getEngineConfiguration(activeEngine);
    }
}
