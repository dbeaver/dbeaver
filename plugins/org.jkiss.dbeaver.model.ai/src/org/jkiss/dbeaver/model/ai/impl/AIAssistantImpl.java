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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.internal.AIMessages;
import org.jkiss.dbeaver.model.ai.qm.AIChatStorage;
import org.jkiss.dbeaver.model.ai.qm.QMAIChatStorageInMemory;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.ai.registry.AIToolboxRegistry;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.ai.utils.ThrowableSupplier;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.exec.DBCMessageException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AIAssistantImpl implements AIAssistant {
    private static final Log log = Log.getLog(AIAssistantImpl.class);

    private static final boolean PRINT_SCOPE_INFO = false;
    private static final int MANY_REQUESTS_RETRIES = 3;
    private static final int MANY_REQUESTS_TIMEOUT = 500;
    public static final String LOG_INDENT = "\t";
    protected static final int MAX_FUNCTION_CALLS = 10;

    protected final DBPWorkspace workspace;
    private final String chatSessionId;

    private AIEngineRequestFactory requestFactory;
    private AIToolboxManager toolboxManager;

    public AIAssistantImpl(@NotNull DBPWorkspace workspace) {
        this.workspace = workspace;
        this.chatSessionId = UUID.randomUUID().toString();
    }

    @NotNull
    protected AIToolboxManager createToolboxManager() {
        return new AIToolboxRegistry();
    }

    @NotNull
    protected AIEngineRequestFactory getRequestFactory() {
        if (requestFactory == null) {
            requestFactory = createRequestFactory();
        }
        return requestFactory;
    }

    @NotNull
    protected AIEngineRequestFactory createRequestFactory() {
        return new AIEngineRequestFactory(new DummyTokenCounter());
    }

    @NotNull
    @Override
    public AIAssistantResponse generateText(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIFunctionContext functionContext,
        @NotNull List<AIMessage> messages
    ) throws DBException {
        checkAiEnablement();

        AIEngineDescriptor engineDescriptor = getEngineDescriptor();
        try (AIEngine<?> engine = engineDescriptor.createEngineInstance()) {
            AIEngineRequest completionRequest = buildAiEngineRequest(
                monitor,
                functionContext,
                messages,
                engine,
                engineDescriptor
            );

            AIEngineRequest request = completionRequest;

            for (int tryIndex = 0; tryIndex < MAX_FUNCTION_CALLS; tryIndex++) {
                Instant now = Instant.now();
                AIEngineResponse completionResponse = requestCompletion(engine, monitor, request);
                int systemPromptLength = AIPromptUtils.calcSystemPromptLength(completionRequest.getMessages());
                AIUsage usage = completionResponse.getUsage() != null ?
                    completionResponse.getUsage() :
                    new AIUsage(0, 0, 0, 0);

                AIMessageMeta requestMeta = new AIMessageMeta(
                    AIMetaTypes.PROMPT,
                    engineDescriptor.getId(),
                    engine.getProperties().getModel(),
                    usage,
                    Duration.between(now, Instant.now()),
                    systemPromptLength
                );

                if (completionResponse.getType() == AIMessageType.FUNCTION) {
                    AIFunctionCall functionCall = completionResponse.getFunctionCall();
                    if (functionCall != null) {
                        AIFunctionResult result = callFunction(functionContext, functionCall);
                        String stringValue = CommonUtils.toString(result.getValue());
                        if (result.getType() == AIFunctionType.ACTION) {
                            return new AIAssistantResponse(
                                AIAssistantResponse.Type.FUNCTION,
                                stringValue,
                                List.of(requestMeta)
                            );
                        } else {
                            List<AIMessage> newMessages = new ArrayList<>(request.getMessages());
                            AIMessage fcMessage = AIMessage.functionCall(functionCall, result);
                            newMessages.add(fcMessage);
                            AIEngineRequest newRequest = new AIEngineRequest(newMessages);
                            newRequest.setFunctions(request.getFunctions());

                            request = newRequest;
                            continue;
                        }
                    }
                } else {
                    List<String> variants = completionResponse.getVariants();
                    if (variants != null && !variants.isEmpty()) {
                        return new AIAssistantResponse(
                            AIAssistantResponse.Type.TEXT,
                            variants.getFirst(),
                            List.of(requestMeta)
                        );
                    }
                }
                return new AIAssistantResponse(
                    AIAssistantResponse.Type.ERROR,
                    AIMessages.ai_empty_engine_response,
                    List.of(requestMeta)
                );
            }
            throw new DBException("Too many AI function calls (" + MAX_FUNCTION_CALLS + ")");
        }
    }

    @NotNull
    @Override
    public CompletableFuture<AIChatConversation> generateTextStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIChatSession chatSession,
        @NotNull AIChatConversation conversation,
        @NotNull AIChatRequest request,
        @NotNull AIChatResponseConsumer chatListener
    ) throws DBException {
        checkAiEnablement();
        CompletableFuture<AIChatConversation> future = conversation.startConversation();

        try {
            AIEngineDescriptor engineDescriptor = getEngineDescriptor();
            AIEngine<?> engine = engineDescriptor.createEngineInstance();
            AIFunctionContext functionContext = new AIFunctionContext(
                monitor,
                request.context(),
                conversation.getPromptGenerator()
            );
            List<AIMessage> curMessages = new ArrayList<>(request.messages());

            AIEngineResponseConsumerImpl engineResponseConsumer = new AIEngineResponseConsumerImpl(
                chatListener,
                monitor,
                engine,
                conversation,
                engineDescriptor,
                new AIFunctionCallConsumer(chatSession, chatListener, conversation, monitor)
            );
            engineResponseConsumer.setLogResponses(isLoggingEnabled());

            if (request.confirmation() != null) {
                if (request.confirmation() instanceof AIFunctionCallConfirmation fcc) {
                    processFunctionCalls(
                        chatSession,
                        conversation,
                        chatListener,
                        functionContext,
                        request,
                        fcc.getFunctionCalls()
                    );
                } else {
                    conversation.promptProcessed(true);
                    throw new DBCFeatureNotSupportedException();
                }
            } else {
                // Stream request runs in async mode
                // When request finishes we process all function calls in response consumer
                executeEngineStreamRequest(
                    monitor,
                    functionContext,
                    curMessages,
                    engineResponseConsumer,
                    engine,
                    engineDescriptor
                );
            }

            return future;
        } catch (Exception e) {
            if (e instanceof DBException dbe) {
                throw dbe;
            } else {
                throw new DBException("Error requesting completion stream", e);
            }
        }
    }

    private void executeEngineStreamRequest(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIFunctionContext functionContext,
        @NotNull List<AIMessage> messages,
        @NotNull AIEngineResponseConsumer listener,
        @NotNull AIEngine<?> engine,
        @NotNull AIEngineDescriptor engineDescriptor
    ) throws DBException {
        AIEngineRequest request = getRequestFactory().build(
            monitor,
            this,
            engine,
            engineDescriptor,
            functionContext,
            messages
        );

        if (isLoggingEnabled()) {
            log.debug("AI chat request:\n" + CommonUtils.addTextIndent(request.getMessages().toString(), LOG_INDENT));
            log.debug("AI chat request functions: " + request.getFunctions().stream().map(AIFunctionDescriptor::getId).toList());

            AIDatabaseContext context = functionContext.getContext();
            if (context != null && PRINT_SCOPE_INFO) {
                if (context.getScope() == AIDatabaseScope.CUSTOM && !CommonUtils.isEmpty(context.getCustomEntities())) {
                    List<String> selectedObjects = context.getCustomEntities().stream().filter(Objects::nonNull)
                        .map(it -> DBUtils.getObjectTypeName(it) + ": " + it.getName()).toList();
                    log.debug("AI chat request custom scope selected objects (" + selectedObjects.size() + "): " + selectedObjects);
                } else {
                    log.debug("AI chat request scope: " + context.getScope());
                }
            }
        }

        AtomicBoolean isTruncated = new AtomicBoolean();
        isTruncated.set(request.wasPromptTruncated());
        callWithRetry(listener, () -> {
            if (isTruncated.get()) {
                isTruncated.set(false);
                listener.warning(
                    "Context description was truncated");
            }
            int systemPromptLength = AIPromptUtils.calcSystemPromptLength(request.getMessages());
            listener.systemPromptLength(systemPromptLength);
            if (AIUtils.useStreamMode()) {
                engine.requestCompletionStream(monitor, request, listener);
            } else {
                AIEngineResponse response = engine.requestCompletion(monitor, request);

                if (response.getFunctionCall() != null) {
                    listener.nextChunk(new AIEngineResponseChunk(response.getFunctionCall()));
                    listener.usage(response.getUsage());
                    listener.completeBlock();
                } else if (response.getVariants() != null) {
                    listener.nextChunk(new AIEngineResponseChunk(response.getVariants()));
                    listener.usage(response.getUsage());
                    listener.completeBlock();
                } else {
                    listener.error(new DBException("Empty response"));
                }
            }

            return null;
        });
    }

    private void processFunctionCalls(
        @NotNull AIChatSession chatSession,
        @NotNull AIChatConversation conversation,
        @NotNull AIChatResponseConsumer chatListener,
        @NotNull AIFunctionContext functionContext,
        @NotNull AIChatRequest request,
        @NotNull List<AIFunctionCall> functionCalls
    ) {
        if (chatSession.isClosed()) {
            return;
        }
        List<AIMessage> messages = request.messages();
        AIDatabaseContext context = request.context();
        List<AIMessage> newMessages = new ArrayList<>(messages);
        RuntimeUtils.scheduleJob("Process AI function calls", monitor -> {
            // Post-process function calls
            for (AIFunctionCall fc : functionCalls) {
                AIFunctionDescriptor function = fc.getOrResolveFunction(getToolboxManager());
                if (function == null) {
                    log.warn("Invalid function call without function reference");
                    continue;
                }
                if (functionContext.getFunctionCalls().size() >= AIAssistantImpl.MAX_FUNCTION_CALLS) {
                    chatListener.error(
                        new DBException(
                            "Too many AI function calls (" + AIAssistantImpl.MAX_FUNCTION_CALLS + ")"));
                    chatListener.complete(List.of(), true);
                    return;
                }
                fc.transformArguments(context, functionContext);
                functionContext.addFunctionCall(fc);

                try {
                    // Call
                    AIFunctionResult result;
                    if (function.getType() == AIFunctionType.ACTION && DBWorkbench.getPlatform().getApplication().isHeadlessMode()) {
                        result = new AIFunctionResult(AIFunctionType.ACTION, fc.getArguments());
                    } else {
                        result = this.callFunction(functionContext, fc);
                    }
                    // Create meta info
                    AIMessage fcMessage = AIMessage.functionCall(fc, result);
                    // Visualize result in chat
                    chatListener.processFunctionCall(fcMessage);
                    if (function.getType() == AIFunctionType.INFORMATION || result.getException() != null) {
                        newMessages.add(fcMessage);
                    }
                } catch (Exception e) {
                    chatListener.error(e);
                    conversation.promptProcessed(true);
                    return;
                }
            }
            if (!newMessages.equals(messages)) {
                try {
                    generateTextStream(monitor, chatSession, conversation, new AIChatRequest(context, newMessages, null), chatListener);
                } catch (Exception e) {
                    chatListener.error(e);
                }
            } else {
                // No more messages for AI
                conversation.promptProcessed(true);
            }
        });
    }

    @Override
    public boolean isFunctionSupported() {
        AIToolboxManager toolboxManager = this.getToolboxManager();
        AIFunctionSettings functionSettings = toolboxManager.getFunctionSettings();
        if (!functionSettings.isFunctionsEnabled()) {
            return false;
        }
        try {
            AIEngineDescriptor engineDescriptor = getEngineDescriptor();
            return engineDescriptor.isSupportsFunctions();
        } catch (DBException e) {
            log.debug(e);
            return false;
        }
    }

    @NotNull
    @Override
    public AIToolboxManager getToolboxManager() {
        if (toolboxManager == null) {
            toolboxManager = createToolboxManager();
        }
        return toolboxManager;
    }

    @NotNull
    @Override
    public AIChatSession.SessionIdProvider getChatSessionProvider() {
        return monitor -> chatSessionId;
    }

    @NotNull
    @Override
    public AIChatStorage createChatStorage() {
        return new QMAIChatStorageInMemory();
    }

    @NotNull
    public AIEngineRequest buildAiEngineRequest(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIFunctionContext functionContext,
        @NotNull List<AIMessage> messages,
        @NotNull AIEngine<?> engine,
        @NotNull AIEngineDescriptor engineDescriptor
    ) throws DBException {
        return getRequestFactory().build(
            monitor,
            this,
            engine,
            engineDescriptor,
            functionContext,
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
            systemGenerator
        );
    }

    @NotNull
    protected AIFunctionResult callFunction(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionCall functionCall
    ) throws DBException {
        String functionName = functionCall.getFunctionName();
        if (CommonUtils.isEmpty(functionName)) {
            throw new DBCMessageException("Function name not specified");
        }
        AIFunctionDescriptor function = functionCall.getFunction();
        if (function == null) {
            function = getToolboxManager().getFunctionByFullId(functionName);
            if (function != null) {
                functionCall.setFunction(function);
            }
        }
        if (function == null) {
            throw new DBCMessageException("Function '" + functionName + "' not found");
        }
        Map<String, Object> arguments = functionCall.getArguments();
        log.debug("Call AI function " + function.getId() + "(" +
            arguments.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(",")) +
            ")");
        DBPDataSourceContainer container = context.getContext() != null
            ? context.getContext().getExecutionContext().getDataSource().getContainer() : null;
        AIBaseFeatures.AI_CHAT_FUNCTION_CALL.use(AIBaseFeatures.buildFeatureParameters(
            container,
            Map.of(
                AIBaseFeatures.FUNCTION_NAME, functionCall.getFunctionName(),
                AIBaseFeatures.PROMPT_TYPE, context.getPrompt().generatorId()
            )
        ));
        AIFunctionResult result;
        try {
            result = function.getToolbox().callFunction(context, function, arguments);
        } catch (DBException e) {
            result = new AIFunctionResult(
                function.getType(),
                "Error calling function '" + function.getId() + "': " + e.getMessage(),
                null,
                e
            );
        }

        return result;
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

    protected boolean isLoggingEnabled() {
        try {
            AIEngineProperties activeEngineConfiguration = getActiveEngineConfiguration();
            if (activeEngineConfiguration == null) {
                log.warn("No active AI engine configuration found");
                return false;
            }

            return activeEngineConfiguration.isLoggingEnabled();
        } catch (DBException e) {
            log.debug("Error getting AI configuration: " + e.getMessage());
            return false;
        }
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
        return callWithRetry(null, supplier);
    }

    protected static <T> T callWithRetry(
        @Nullable AIEngineResponseConsumer listener,
        @NotNull ThrowableSupplier<T, DBException> supplier
    ) throws DBException {
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
        DBException dbException = new DBException("Request failed after " + MANY_REQUESTS_RETRIES + " attempts");
        if (listener != null) {
            listener.error(dbException);
        }
        throw new DBException("Request failed after " + MANY_REQUESTS_RETRIES + " attempts");
    }

}
