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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionParameter;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.model.ai.AIImageDimensions;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageMeta;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.engine.AIEngineRequest;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponse;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.*;
import org.jkiss.dbeaver.model.ai.internal.AIMessages;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenAiUtils {
    private static final Log log = Log.getLog(OpenAiUtils.class);
    private static final int MINI_IMAGE_BASE_TOKENS = 2833;
    private static final int MINI_IMAGE_TILE_TOKENS = 5667;
    private static final int MAX_IMAGE_TILES = 8;
    private static final int IMAGE_TILE_SIZE = 512;
    private static final int IMAGE_MAX_SIDE = 2048;
    private static final int IMAGE_SHORT_SIDE = 768;

    private OpenAiUtils() {
    }

    public static int estimateImageTokens(@Nullable String model, @NotNull AIImageAttachment image) {
        if (model == null || !model.contains("gpt-4o-mini")) {
            return AIImageAttachment.DEFAULT_TOKEN_ESTIMATE;
        }
        int tiles = MAX_IMAGE_TILES;
        try {
            AIImageDimensions dimensions = AIImageDimensions.read(image.getBytes());
            double width = dimensions.width();
            double height = dimensions.height();
            double scale = Math.min(1, IMAGE_MAX_SIDE / Math.max(width, height));
            width *= scale;
            height *= scale;
            scale = Math.min(1, IMAGE_SHORT_SIDE / Math.min(width, height));
            tiles = (int) (Math.ceil(width * scale / IMAGE_TILE_SIZE) * Math.ceil(height * scale / IMAGE_TILE_SIZE));
        } catch (IOException | IllegalArgumentException exception) {
            log.debug("Cannot estimate image tokens from dimensions", exception);
        }
        // auto detail can use the high-detail tile budget
        return MINI_IMAGE_BASE_TOKENS + Math.min(MAX_IMAGE_TILES, tiles) * MINI_IMAGE_TILE_TOKENS;
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
    public static AIEngineResponse toEngineResponse(
        @NotNull OAIResponsesResponse result,
        @NotNull String reasoningSource
    ) throws DBException {
        result.validateCompletion();
        List<AIFunctionCall> calls = new ArrayList<>();
        for (OAIMessage item : result.output) {
            if (OAIMessage.TYPE_FUNCTION_CALL.equals(item.type)) {
                calls.add(createFunctionCall(item));
            }
        }
        Map<String, String> metadata = completeResponse(calls, result.output, reasoningSource);
        AIEngineResponse response;
        if (calls.isEmpty()) {
            List<String> choices = result.output.stream().filter(item -> OAIMessage.TYPE_MESSAGE.equals(item.type))
                .map(OAIMessage::getFullText).toList();
            response = new AIEngineResponse(AIMessageType.ASSISTANT,
                choices.isEmpty() ? List.of(AIMessages.ai_empty_engine_response) : choices, result.getAIUsage());
        } else {
            response = new AIEngineResponse(calls, result.getAIUsage());
        }
        response.setMessageMetadata(metadata);
        return response;
    }

    @NotNull
    static Map<String, String> completeResponse(
        @NotNull List<AIFunctionCall> calls,
        @NotNull List<OAIMessage> output,
        @Nullable String reasoningSource
    ) {
        if (reasoningSource == null || output.stream().noneMatch(item -> OAIMessage.TYPE_FUNCTION_REASONING.equals(item.type))) {
            return Map.of();
        }
        // preserve the native order even if the user approves tool calls in a different order
        Map<String, String> metadata = Map.of(OpenAIConstants.RESPONSE_OUTPUT, JSONUtils.GSON.toJson(output),
            OpenAIConstants.REASONING_SOURCE, reasoningSource);
        for (AIFunctionCall call : calls) {
            Map<String, String> callMetadata = new HashMap<>(metadata);
            if (call.getMessageMetadata() != null) {
                callMetadata.putAll(call.getMessageMetadata());
            }
            call.setMessageMetadata(callMetadata);
        }
        return metadata;
    }

    @NotNull
    public static OAIResponsesRequest createOpenAiRequest(
        @NotNull AIEngineRequest request,
        @Nullable String modelName,
        @Nullable Double temperature
    ) {
        return createOpenAiRequest(request, modelName, temperature, null, false);
    }

    @NotNull
    public static OAIResponsesRequest createOpenAiRequest(
        @NotNull AIEngineRequest request,
        @Nullable String modelName,
        @Nullable Double temperature,
        @Nullable String reasoningSource,
        boolean reasoningHistoryRequired
    ) {
        OAIResponsesRequest oaiRequest = new OAIResponsesRequest();
        List<AIMessage> messages = request.getMessages();
        oaiRequest.input = fromMessages(messages, reasoningSource, reasoningHistoryRequired && !request.getFunctions().isEmpty());
        oaiRequest.reasoningSource = reasoningSource;
        oaiRequest.store = false;
        if (reasoningSource != null) {
            oaiRequest.include = new String[] {"reasoning.encrypted_content"};
        }
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
                tool.parameters = new OAIToolParameters();
                tool.parameters.type = OAIToolParameters.TYPE_OBJECT;
                if (fd.getParameters().length > 0) {
                    List<String> requiredFields = new ArrayList<>();
                    for (AIFunctionParameter param : fd.getParameters()) {
                        OAIToolParameter tp = new OAIToolParameter();
                        tp.type = param.getType();
                        tp.description = param.getDescription();
                        tp.enumItems = param.getValidValues();
                        if (param.isRequired()) {
                            requiredFields.add(param.getName());
                        }
                        tool.parameters.properties.put(param.getName(), tp);
                    }
                    if (!requiredFields.isEmpty()) {
                        tool.parameters.required = requiredFields.toArray(new String[0]);
                    }
                }
                tools.add(tool);
            }
            oaiRequest.tools = tools;
        }

        return oaiRequest;
    }

    static void prepareChatGptAccountRequest(@NotNull OAIResponsesRequest request) {
        if (request.input == null) {
            return;
        }
        String instructions = request.input.stream()
            .filter(message -> "system".equals(message.role))
            .map(OAIMessage::getFullText)
            .filter(CommonUtils::isNotEmpty)
            .collect(Collectors.joining("\n"));
        request.instructions = CommonUtils.isEmpty(instructions) ? null : instructions;
        request.input = request.input.stream()
            .filter(message -> !"system".equals(message.role))
            .toList();
    }

    @NotNull
    private static List<OAIMessage> fromMessages(
        @NotNull List<AIMessage> messages,
        @Nullable String reasoningSource,
        boolean reasoningRequired
    ) {
        List<OAIMessage> result = new ArrayList<>(messages.size());
        Set<String> replayed = new HashSet<>();
        int turnStart = 0;
        for (int index = 0; index < messages.size(); index++) {
            AIMessage message = messages.get(index);
            if (message.getRole() == AIMessageType.USER) {
                replayed.clear();
                turnStart = index;
            }
            String output = getResponseMetadata(message).get(OpenAIConstants.RESPONSE_OUTPUT);
            if (output != null && reasoningSource != null) {
                if (replayed.contains(output)) {
                    continue;
                }
                List<OAIMessage> restored = restoreResponseOutput(messages, turnStart, output, reasoningSource);
                if (restored == null) {
                    result.add(importResponse(message));
                } else {
                    replayed.add(output);
                    result.addAll(restored);
                }
            } else if (reasoningRequired && (message.getRole() == AIMessageType.ASSISTANT
                || message.getRole() == AIMessageType.FUNCTION || message.getFunctionCall() != null)
            ) {
                // deepseek requires original reasoning for assistant/tool history when tools are enabled
                result.add(importResponse(message));
            } else if (message.getFunctionCall() != null) {
                OAIMessage call = OAIMessageFactory.fromAIMessage(message);
                if (!CommonUtils.isEmpty(call.callId)) {
                    result.add(call);
                    result.add(OAIMessageFactory.fromAIMessage(message, call.callId));
                }
            } else {
                result.add(OAIMessageFactory.fromAIMessage(message));
            }
        }
        return result;
    }

    @NotNull
    private static Map<String, String> getResponseMetadata(@NotNull AIMessage message) {
        if (message.getFunctionCall() != null) {
            Map<String, String> metadata = message.getFunctionCall().getMessageMetadata();
            return metadata == null ? Map.of() : metadata;
        }
        return message.getMeta() == null ? Map.of() : message.getMeta().stream()
            .map(AIMessageMeta::responseMetadata)
            .filter(metadata -> metadata.containsKey(OpenAIConstants.RESPONSE_OUTPUT))
            .findFirst().orElse(Map.of());
    }

    @Nullable
    private static List<OAIMessage> restoreResponseOutput(
        @NotNull List<AIMessage> messages,
        int turnStart,
        @NotNull String output,
        @Nullable String reasoningSource
    ) {
        Map<String, AIMessage> results = new HashMap<>();
        for (int index = turnStart; index < messages.size(); index++) {
            AIMessage message = messages.get(index);
            if (index > turnStart && message.getRole() == AIMessageType.USER) {
                break;
            }
            Map<String, String> metadata = getResponseMetadata(message);
            if (output.equals(metadata.get(OpenAIConstants.RESPONSE_OUTPUT))) {
                if (Boolean.parseBoolean(metadata.get(AIMessage.RESPONSE_CONTENT_MODIFIED))
                    || reasoningSource != null && !reasoningSource.equals(metadata.get(OpenAIConstants.REASONING_SOURCE))
                ) {
                    return null;
                }
                if (message.getFunctionCall() != null) {
                    results.put(metadata.get(OpenAIConstants.TOOL_RESULT_CALL_ID), message);
                }
            }
        }
        List<OAIMessage> nativeOutput = List.of(JSONUtils.GSON.fromJson(output, OAIMessage[].class));
        List<OAIMessage> restored = new ArrayList<>(nativeOutput);
        for (OAIMessage item : nativeOutput) {
            if (OAIMessage.TYPE_FUNCTION_CALL.equals(item.type)) {
                AIMessage result = results.get(item.callId);
                if (result == null || CommonUtils.isEmpty(item.callId)) {
                    return null;
                }
                restored.add(OAIMessageFactory.fromAIMessage(result, item.callId));
            }
        }
        return restored;
    }

    @NotNull
    private static OAIMessage importResponse(@NotNull AIMessage message) {
        return OAIMessageFactory.fromAIMessage(AIMessage.userMessage("Previous assistant response:\n" + message.getContent()));
    }

    public static boolean shouldFallbackToLegacyChat(@Nullable String message) {
        if (message == null) {
            return false;
        }
        return message.contains("is not supported via Responses API")
            || message.contains("does not support Responses API")
            || message.contains("model_not_supported")
            || message.contains("The requested model is not supported");
    }

    public static boolean isTemperatureNotSupported(@Nullable String message) {
        return message != null
            && ((message.contains("Unsupported parameter") && message.contains("temperature"))
                || message.contains("`temperature` is deprecated"));
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
                if (shouldFallbackToLegacyChat(responseBody)) {
                    reason = OpenAIConstants.LEGACY_FALLBACK;
                } else if (isTemperatureNotSupported(responseBody)) {
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
