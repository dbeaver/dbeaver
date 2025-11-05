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
package org.jkiss.dbeaver.model.ai.engine.openai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.*;
import org.jkiss.dbeaver.model.ai.internal.AIMessages;
import org.jkiss.dbeaver.model.ai.registry.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.utils.DisposableLazyValue;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class OpenAIEngine<PROPS extends OpenAIBaseProperties> extends BaseCompletionEngine<PROPS> {

    protected final DisposableLazyValue<OpenAIClient, DBException> openAiService = new DisposableLazyValue<>() {
        @NotNull
        @Override
        protected OpenAIClient initialize() throws DBException {
            return createClient();
        }

        @Override
        protected void onDispose(@NotNull OpenAIClient disposedValue) {
            disposedValue.close();
        }
    };

    public OpenAIEngine(@NotNull PROPS properties) {
        super(properties);
    }

    @NotNull
    @Override
    public List<AIModel> getModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        return openAiService.getInstance().getModels(monitor)
            .stream()
            .map(model -> OpenAIModels.KNOWN_MODELS.getOrDefault(
                model.id(),
                new AIModel(model.id(), null, OpenAIModels.detectModelFeatures(model.id()))
            ))
            .toList();
    }

    @NotNull
    @Override
    public AIEngineResponse requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        OAIResponsesResponse completionResult = complete(monitor, request);
        List<OAIMessage> messages = completionResult.output;
        if (messages.isEmpty()) {
            return new AIEngineResponse(AIMessageType.ASSISTANT, List.of(AIMessages.ai_empty_engine_response));
        }
        OAIMessage message = messages.getFirst();
        if (OAIMessage.TYPE_FUNCTION_CALL.equals(message.type)) {
            AIFunctionCall fc = OpenAIClient.createFunctionCall(message);
            return new AIEngineResponse(fc);
        } else {
            List<String> choices = messages.stream()
                .map(OAIMessage::getFullText)
                .toList();

            return new AIEngineResponse(AIMessageType.ASSISTANT, choices);
        }
    }

    @Override
    public void requestCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        OAIResponsesRequest oaiRequest = createOpenAiRequest(request);
        oaiRequest.stream = true;
        openAiService.getInstance().createChatCompletionStream(monitor, oaiRequest, listener);
    }

    @Override
    public int getContextWindowSize(@NotNull DBRProgressMonitor monitor) throws DBException {
        Integer contextWindowSize = properties.getContextWindowSize();
        if (contextWindowSize != null) {
            return contextWindowSize;
        }

        throw new DBException("Context window size is not set for the model: " + model());
    }

    @Override
    public void close() throws DBException {
        openAiService.dispose();
    }

    @NotNull
    protected OAIResponsesResponse complete(
        @NotNull DBRProgressMonitor monitor,
        @NotNull AIEngineRequest request
    ) throws DBException {
        OAIResponsesRequest oaiRequest = createOpenAiRequest(request);

        return openAiService.getInstance().createChatCompletion(monitor, oaiRequest);
    }

    @NotNull
    private OAIResponsesRequest createOpenAiRequest(@NotNull AIEngineRequest request) throws DBException {
        OAIResponsesRequest oaiRequest = new OAIResponsesRequest();
        List<AIMessage> messages = request.getMessages();
        oaiRequest.input = fromMessages(messages);
        oaiRequest.temperature = temperature();
        oaiRequest.store = false;
        oaiRequest.model = model();

        if (!CommonUtils.isEmpty(request.getFunctions())) {
            List<OAITool> tools = new ArrayList<>();
            for (AIFunctionDescriptor fd : request.getFunctions()) {
                OAITool tool = new OAITool();
                tool.type = OAITool.TYPE_FUNCTION;
                tool.name = fd.getId();
                tool.description = fd.getDescription();
                tool.parameters.type = OAIToolParameters.TYPE_OBJECT;
                for (AIFunctionDescriptor.Parameter param : fd.getParameters()) {
                    OAIToolParameter tp = new OAIToolParameter();
                    tp.type = param.getType();
                    tp.description = param.getDescription();
                    tp.enumItems = param.getValidValues();
                    tool.parameters.properties.put(param.getName(), tp);
                }
                tools.add(tool);
            }
            oaiRequest.tools = tools;
        }

        return oaiRequest;
    }

    @NotNull
    private static List<OAIMessage> fromMessages(@NotNull List<AIMessage> messages) {
        return messages.stream()
            .map(OAIMessage::new)
            .toList();
    }

    @NotNull
    protected OpenAIClient createClient() throws DBException {
        String token = properties.getToken();
        if (token == null || token.isEmpty()) {
            throw new DBException("OpenAI API token is not set");
        }
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = OpenAIClient.OPENAI_ENDPOINT;
        }
        if (properties.isLegacyApi()) {
            return OpenAIClientLegacy.createClient(baseUrl, token);
        }
        return OpenAIClient.createClient(baseUrl, token);
    }

    @Nullable
    protected String model() throws DBException {
        return properties.getModel();
    }

    protected double temperature() throws DBException {
        return properties.getTemperature();
    }
}
