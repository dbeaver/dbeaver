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
package org.jkiss.dbeaver.model.ai.engine.openai;

import com.google.gson.JsonSyntaxException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionParameter;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.engine.AIEngineRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.*;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.CommonUtils;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenAiUtils {
    private OpenAiUtils() {
    }

    @NotNull
    public static AIFunctionCall createFunctionCall(@NotNull OAIMessage message) throws DBException {
        String argumentsStr = message.arguments;
        Map<String, Object> arguments;
        try {
            arguments = JSONUtils.GSON.fromJson(argumentsStr, JSONUtils.MAP_TYPE_TOKEN);
        } catch (JsonSyntaxException e) {
            throw new DBException("Error parsing function call arguments", e);
        }
        Map<String, String> metadata = CommonUtils.isEmpty(message.callId) ? null :
            Map.of(OpenAIConstants.TOOL_RESULT_CALL_ID, message.callId);
        return new AIFunctionCall(message.name, arguments, metadata);
    }



    @NotNull
    public static OAIResponsesRequest createOpenAiRequest(
        @NotNull AIEngineRequest request,
        @Nullable String modelName,
        @Nullable Double temperature
    ) {
        OAIResponsesRequest oaiRequest = new OAIResponsesRequest();
        List<AIMessage> messages = request.getMessages();
        oaiRequest.input = fromMessages(messages);
        oaiRequest.store = false;
        oaiRequest.model = modelName;
        if (temperature != null) {
            oaiRequest.temperature = temperature;
        }
        if (!CommonUtils.isEmpty(request.getFunctions())) {
            List<OAITool> tools = new ArrayList<>();
            for (AIFunctionDescriptor fd : request.getFunctions()) {
                OAITool tool = new OAITool();
                tool.type = OAITool.TYPE_FUNCTION;
                tool.name = fd.getFullId();
                tool.description = fd.getAiDescription();
                tool.parameters.type = OAIToolParameters.TYPE_OBJECT;
                List<String> requiredFields = new ArrayList<>();
                for (AIFunctionParameter param : fd.getParameters()) {
                    OAIToolParameter tp = new OAIToolParameter();
                    tp.type = param.getType();
                    tp.description = param.getDescription();
                    tp.enumItems = param.getValidValues();
                    requiredFields.add(param.getName());
                    tool.parameters.properties.put(param.getName(), tp);
                }
                tool.parameters.required = requiredFields.toArray(new String[0]);
                tools.add(tool);
            }
            oaiRequest.tools = tools;
        }

        return oaiRequest;
    }

    @NotNull
    private static List<OAIMessage> fromMessages(@NotNull List<AIMessage> messages) {
        List<OAIMessage> result = new ArrayList<>(messages.size());
        for (AIMessage message : messages) {
            if (message.getFunctionCall() != null) {
                OAIMessage functionCallMessage = OAIMessageFactory.fromAIMessage(message);
                if (!CommonUtils.isEmpty(functionCallMessage.callId)) {
                    result.add(functionCallMessage);
                    // OpenAI Responses API requires matching function_call_output for each function_call.
                    // Keep orphan function calls in history as regular assistant messages.
                    result.add(OAIMessageFactory.fromAIMessage(message, functionCallMessage.callId));
                }
            } else {
                result.add(OAIMessageFactory.fromAIMessage(message));
            }
        }
        return result;
    }

    public static boolean processErrors(
        @NotNull MonitoredHttpClient.ErrorMapper mapper,
        @NotNull Consumer<Throwable> errorHandler,
        @NotNull HttpResponse<Stream<String>> response,
        @NotNull AtomicBoolean suppressCompletion,
        @Nullable Consumer<String> backupOption,
        int statusCode
    ) {
        if (statusCode != 200) {
            String responseBody = response.body().collect(Collectors.joining());
            if (backupOption != null && statusCode == 400) {
                String reason;
                if (responseBody.contains("is not supported via Responses API")) {
                    reason = OpenAIConstants.LEGACY_FALLBACK;
                } else if (responseBody.contains("Unsupported parameter: 'temperature'")) {
                    reason = OpenAIConstants.TEMPERATURE_NOT_SUPPORTED;
                } else {
                    errorHandler.accept(mapper.map(statusCode, responseBody));
                    return true;
                }
                suppressCompletion.set(true);
                backupOption.accept(reason);
            } else {
                errorHandler.accept(mapper.map(statusCode, responseBody));
            }
            return true;
        }
        return false;
    }
}
