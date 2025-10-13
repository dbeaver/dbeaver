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
import org.jkiss.dbeaver.model.ai.internal.AIMessages;
import org.jkiss.dbeaver.model.ai.registry.*;
import org.jkiss.dbeaver.model.ai.utils.ThrowableSupplier;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCMessageException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class AIAssistantImpl implements AIAssistant {
    private static final Log log = Log.getLog(AIAssistantImpl.class);

    private static final int MANY_REQUESTS_RETRIES = 3;
    private static final int MANY_REQUESTS_TIMEOUT = 500;
    public static final String LOG_INDENT = "\t";
    protected static final int MAX_FUNCTION_CALLS = 5;

    protected final DBPWorkspace workspace;

    protected final AIEngineRequestFactory requestFactory;
    protected AISqlFormatter sqlFormatter;

    public AIAssistantImpl(@NotNull DBPWorkspace workspace) {
        this.workspace = workspace;
        this.requestFactory = createRequestFactory();
        this.sqlFormatter = createSqlFormatter();
    }

    protected AISqlFormatter createSqlFormatter() {
        try {
            return AIAssistantRegistry.getInstance().getDescriptor().createSqlFormatter();
        } catch (DBException e) {
            log.error("Error creating SQL formatter", e);
            return new SimpleSqlFormatterImpl();
        }
    }

    protected AIEngineRequestFactory createRequestFactory() {
        return new AIEngineRequestFactory(
            new AIDatabaseSnapshotService(),
            new DummyTokenCounter()
        );
    }

    @NotNull
    @Override
    public AIAssistantResponse generateText(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AIDatabaseContext context,
        @NotNull AIPromptGenerator systemGenerator,
        @NotNull List<AIMessage> messages
    ) throws DBException {
        checkAiEnablement();

        AIEngineDescriptor engineDescriptor = getEngineDescriptor();
        try (AIEngine<?> engine = engineDescriptor.createEngineInstance()) {
            AIEngineRequest completionRequest = buildAiEngineRequest(
                monitor,
                context,
                systemGenerator,
                messages,
                engine,
                engineDescriptor
            );
            AIFunctionContext functionContext = createAiFunctionContext(monitor, context, systemGenerator, messages);

            AIEngineRequest request = completionRequest;
            for (int tryIndex = 0; tryIndex < MAX_FUNCTION_CALLS; tryIndex++) {
                AIEngineResponse completionResponse = requestCompletion(engine, monitor, request);

                if (completionResponse.getType() == AIMessageType.FUNCTION) {
                    AIFunctionCall functionCall = completionResponse.getFunctionCall();
                    if (functionCall != null) {
                        functionContext.addFunctionCall(functionCall);
                        AIFunctionResult result = callFunction(functionContext, functionCall);
                        String stringValue = CommonUtils.toString(result.getValue());
                        if (result.getType() == AIFunctionResult.FunctionType.ACTION) {
                            return new AIAssistantResponse(AIAssistantResponse.Type.FUNCTION, stringValue);
                        } else {
                            List<AIMessage> newMessages = new ArrayList<>(request.getMessages());
                            newMessages.add(new AIMessage(AIMessageType.USER, stringValue));
                            AIEngineRequest newRequest = new AIEngineRequest(newMessages);
                            newRequest.setFunctions(request.getFunctions());

                            request = newRequest;
                            continue;
                        }
                    }
                } else {
                    List<String> variants = completionResponse.getVariants();
                    if (variants != null && !variants.isEmpty()) {
                        return new AIAssistantResponse(AIAssistantResponse.Type.TEXT, variants.getFirst());
                    }
                }
                return new AIAssistantResponse(AIAssistantResponse.Type.ERROR, AIMessages.ai_empty_engine_response);
            }
            throw new DBException("Too many AI function calls (" + MAX_FUNCTION_CALLS + ")");
        }
    }

    @NotNull
    public AIEngineRequest buildAiEngineRequest(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AIDatabaseContext context,
        @NotNull AIPromptGenerator systemGenerator,
        @NotNull List<AIMessage> messages,
        @NotNull AIEngine<?> engine,
        @NotNull AIEngineDescriptor engineDescriptor
    ) throws DBException {
        return requestFactory.build(
            monitor,
            engine,
            engineDescriptor,
            systemGenerator,
            context,
            messages
        );
    }

    @NotNull
    private static AIFunctionContext createAiFunctionContext(
        @NotNull DBRProgressMonitor monitor,
        @Nullable AIDatabaseContext context,
        @NotNull AIPromptGenerator systemGenerator,
        @NotNull List<AIMessage> messages
    ) {
        return new AIFunctionContext(
            monitor,
            context,
            systemGenerator,
            messages
        );
    }

    @NotNull
    protected AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionCall functionCall
    ) throws DBException {
        AIFunctionRegistry registry = AIFunctionRegistry.getInstance();
        String functionName = functionCall.getFunctionName();
        AIFunctionDescriptor function = registry.getFunction(functionName);
        if (function == null) {
            throw new DBCMessageException("Function '" + functionName + "' not found");
        }
        functionCall.setFunction(function);
        return registry.callFunction(context, function, functionCall.getArguments());
    }

    protected void checkAiEnablement() throws DBException {
        if (AISettingsManager.getInstance().getSettings().isAiDisabled()) {
            throw new DBException("AI integration is disabled");
        }
    }

    public static String getActiveEngineId() {
        return AISettingsManager.getInstance().getSettings().activeEngine();
    }

    public boolean isEngineSupports(Class<?> api) {
        return AIEngineRegistry.getInstance().isEngineSupports(
            getActiveEngineId(),
            api);
    }

    @NotNull
    public AIEngine<?> createEngine() throws DBException {
        return AIEngineRegistry.getInstance().createEngine(getActiveEngineId());
    }

    @NotNull
    public AIEngineDescriptor getEngineDescriptor() throws DBException {
        AIEngineDescriptor descriptor = AIEngineRegistry.getInstance().getEngineDescriptor(getActiveEngineId());
        if (descriptor == null) {
            log.trace("Active engine is not present in the configuration, switching to default active engine");
            AIEngineDescriptor defaultCompletionEngineDescriptor =
                AIEngineRegistry.getInstance().getDefaultCompletionEngineDescriptor();
            if (defaultCompletionEngineDescriptor == null) {
                throw new DBException("AI engine  not found");
            }
            descriptor = defaultCompletionEngineDescriptor;
        }
        return descriptor;
    }

    @NotNull
    protected AIEngineResponse requestCompletion(
        @NotNull AIEngine<?> engine,
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        try {
            boolean loggingEnabled = isLoggingEnabled();
            if (loggingEnabled) {
                log.debug("AI request:\n" + CommonUtils.addTextIndent(request.getMessages().toString(), LOG_INDENT));
            }

            AIEngineResponse completionResponse = callWithRetry(() -> engine.requestCompletion(monitor, request));

            if (loggingEnabled) {
                log.debug("AI response:\n" + CommonUtils.addTextIndent(completionResponse.toString(), LOG_INDENT));
            }

            return completionResponse;
        } catch (Exception e) {
            if (e instanceof DBException dbe) {
                throw dbe;
            } else {
                throw new DBException("Error requesting completion", e);
            }
        }
    }

    protected boolean isLoggingEnabled() throws DBException {
        AIEngineProperties activeEngineConfiguration = getActiveEngineConfiguration();
        if (activeEngineConfiguration == null) {
            log.warn("No active AI engine configuration found");
            return false;
        }

        return activeEngineConfiguration.isLoggingEnabled();
    }

    @Nullable
    private AIEngineProperties getActiveEngineConfiguration() throws DBException {
        AISettingsManager settingsManager = AISettingsManager.getInstance();
        String activeEngine = settingsManager.getSettings().activeEngine();
        if (activeEngine == null || activeEngine.isEmpty()) {
            log.warn("No active AI engine configured");
            return null;
        }
        return settingsManager.getSettings().getEngineConfiguration(activeEngine);
    }


    protected static <T> T callWithRetry(ThrowableSupplier<T, DBException> supplier) throws DBException {
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

}
