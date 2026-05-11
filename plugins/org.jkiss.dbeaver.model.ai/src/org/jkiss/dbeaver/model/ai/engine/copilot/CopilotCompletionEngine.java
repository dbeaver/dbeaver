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
package org.jkiss.dbeaver.model.ai.engine.copilot;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.AIUsage;
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.*;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAiUtils;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessage;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesResponse;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAITool;
import org.jkiss.dbeaver.model.ai.internal.AIMessages;
import org.jkiss.dbeaver.model.ai.utils.DisposableLazyValue;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CopilotCompletionEngine<P extends CopilotProperties> extends BaseCompletionEngine<P> {

    protected final DisposableLazyValue<CopilotClientResponses, DBException> client = new DisposableLazyValue<>() {
        @NotNull
        @Override
        protected CopilotClientResponses initialize() throws DBException {
            return createClient(getProperties().getBaseAuthUrl());
        }

        @Override
        protected void onDispose(@NotNull CopilotClientResponses disposedValue) {
            disposedValue.close();
        }
    };
    private CopilotSessionToken sessionToken;

    public CopilotCompletionEngine(@NotNull P properties) {
        super(properties);
    }

    @NotNull
    @Override
    public List<AIModel> getModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<CopilotModel> copilotModels = client.getInstance().loadModels(monitor, requestSessionToken(monitor).token());
        List<AIModel> list = new ArrayList<>();
        for (CopilotModel model : copilotModels) {
            AIModel aiModel = new AIModel(model.id(), null, Set.of(AIModelFeature.CHAT));
            list.add(aiModel);
        }
        return list;
    }

    @NotNull
    @Override
    public AIEngineResponse requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        OAIResponsesResponse chatResponse = client.getInstance().chat(
            monitor,
            requestSessionToken(monitor).token(),
            createLegacyChatRequest(request, false),
            OpenAiUtils.createOpenAiRequest(request, getModelName(), getProperties().getTemperature())
        );

        return toEngineResponse(chatResponse);
    }

    @Override
    public void requestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        client.getInstance().createChatCompletionStream(
            monitor,
            requestSessionToken(monitor).token(),
            OpenAiUtils.createOpenAiRequest(request, getModelName(), null),
            createLegacyChatRequest(request, true),
            listener
        );
    }

    @Override
    public int getContextWindowSize(@NotNull DBRProgressMonitor monitor) throws DBException {
        Integer contextWindowSize = properties.getContextWindowSize();
        if (contextWindowSize != null) {
            return contextWindowSize;
        }

        throw new DBException("Context window size is not defined in Copilot properties. " +
            "Please set it explicitly or use a known model with a predefined context window size.");
    }

    @Override
    public void close() throws DBException {
        client.dispose();
    }

    @NotNull
    protected CopilotSessionToken requestSessionToken(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (sessionToken != null) {
            return sessionToken;
        }

        synchronized (this) {
            if (sessionToken == null) {
                sessionToken = client.getInstance().requestSessionToken(monitor, properties.getToken());
            }
        }
        return sessionToken;
    }

    @NotNull
    public String getModelName() {
        return CommonUtils.toString(
            properties.getModel(),
            OpenAIConstants.DEFAULT_MODEL
        );
    }

    @NotNull
    private CopilotChatRequest createLegacyChatRequest(
        @NotNull AIEngineRequest request,
        boolean stream
    ) {
        return CopilotChatRequest.builder()
            .withModel(getModelName())
            .withMessages(toCopilotMessages(request.getMessages()))
            .withTools(request.getFunctions().stream()
                .map(OAITool::fromDescriptor)
                .map(CopilotFunction::new)
                .toList())
            .withTemperature(properties.getTemperature())
            .withStream(stream)
            .withIntent(false)
            .withTopP(1)
            .withN(1)
            .build();
    }


    @NotNull
    private AIEngineResponse toEngineResponse(@NotNull OAIResponsesResponse response) throws DBException {
        List<OAIMessage> messages = response.output.stream()
            .filter(msg -> !OAIMessage.TYPE_FUNCTION_REASONING.equals(msg.type))
            .toList();
        AIUsage usage = response.getAIUsage();
        if (messages.isEmpty()) {
            return new AIEngineResponse(
                AIMessageType.ASSISTANT,
                List.of(AIMessages.ai_empty_engine_response),
                usage
            );
        }
        OAIMessage message = messages.getFirst();
        if (OAIMessage.TYPE_FUNCTION_CALL.equals(message.type)) {
            AIFunctionCall fc = OpenAiUtils.createFunctionCall(message);
            return new AIEngineResponse(fc, usage);
        } else {
            List<String> choices = messages.stream()
                .map(OAIMessage::getFullText)
                .toList();

            return new AIEngineResponse(AIMessageType.ASSISTANT, choices, usage);
        }
    }

    @NotNull
    private static List<CopilotMessage> toCopilotMessages(@NotNull List<AIMessage> messages) {
        return messages.stream()
            .flatMap(message -> CopilotMessage.from(message).stream())
            .toList();
    }

    @NotNull
    protected CopilotClientResponses createClient(@NotNull String baseAuthUrl) throws DBException {
        String token = properties.getToken();
        if (token == null || token.isEmpty()) {
            throw new DBException("Copilot API token is not set");
        }

        return new CopilotClientResponses(baseAuthUrl);
    }
}
