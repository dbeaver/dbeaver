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
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionParameter;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.model.ai.AIImageDimensions;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageMeta;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.AIUsage;
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
import java.util.function.ToIntFunction;
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
    public static AIFunctionCall createFunctionCall(@NotNull OAIMessage message, @NotNull List<OAIMessage> reasoning) throws DBException {
        return createFunctionCall(message, reasoning, null);
    }

    @NotNull
    public static AIFunctionCall createFunctionCall(
        @NotNull OAIMessage message,
        @NotNull List<OAIMessage> reasoning,
        @Nullable String reasoningSource
    ) throws DBException {
        AIFunctionCall call = createFunctionCall(message);
        if (!reasoning.isEmpty() || reasoningSource != null) {
            Map<String, String> metadata = new HashMap<>();
            if (call.getMessageMetadata() != null) {
                metadata.putAll(call.getMessageMetadata());
            }
            metadata.putAll(createReasoningMetadata(reasoning, reasoningSource));
            call.setMessageMetadata(metadata);
        }
        return call;
    }

    @NotNull
    static Map<String, String> createReasoningMetadata(@NotNull List<OAIMessage> reasoning, @Nullable String reasoningSource) {
        return createReasoningMetadata(reasoning, reasoningSource, null);
    }

    @NotNull
    static Map<String, String> createReasoningMetadata(
        @NotNull List<OAIMessage> reasoning,
        @Nullable String reasoningSource,
        @Nullable AIUsage usage
    ) {
        Map<String, String> metadata = new HashMap<>();
        if (!reasoning.isEmpty()) {
            metadata.put(OpenAIConstants.TOOL_CALL_REASONING, JSONUtils.GSON.toJson(reasoning));
        }
        if (reasoningSource != null) {
            metadata.put(OpenAIConstants.REASONING_SOURCE, reasoningSource);
        }
        if (!reasoning.isEmpty() && usage != null && usage.reasoningTokens() > 0) {
            metadata.put(OpenAIConstants.REASONING_TOKENS, Integer.toString(usage.reasoningTokens()));
        }
        return metadata;
    }

    @NotNull
    public static AIEngineResponse toEngineResponse(
        @NotNull OAIResponsesResponse result,
        @Nullable String reasoningSource
    ) throws DBException {
        result.validateCompletion();
        List<OAIMessage> reasoning = new ArrayList<>();
        List<AIFunctionCall> calls = new ArrayList<>();
        for (OAIMessage item : result.output) {
            if (OAIMessage.TYPE_FUNCTION_REASONING.equals(item.type)) {
                reasoning.add(item);
            } else if (OAIMessage.TYPE_FUNCTION_CALL.equals(item.type)) {
                calls.add(createFunctionCall(item));
            }
        }
        AIUsage usage = result.getAIUsage();
        if (!calls.isEmpty()) {
            completeResponse(calls, result.output, reasoningSource, usage);
            return new AIEngineResponse(calls, usage);
        }
        List<String> choices = result.output.stream().filter(item -> OAIMessage.TYPE_MESSAGE.equals(item.type))
            .map(OAIMessage::getFullText).toList();
        AIEngineResponse response = new AIEngineResponse(AIMessageType.ASSISTANT,
            choices.isEmpty() ? List.of(AIMessages.ai_empty_engine_response) : choices, usage);
        response.setMessageMetadata(createReasoningMetadata(reasoning, reasoningSource, usage));
        return response;
    }

    @NotNull
    static Map<String, String> completeResponse(
        @NotNull List<AIFunctionCall> calls,
        @NotNull List<OAIMessage> output,
        @Nullable String reasoningSource,
        @Nullable AIUsage usage
    ) {
        List<OAIMessage> reasoning = output.stream().filter(item -> OAIMessage.TYPE_FUNCTION_REASONING.equals(item.type)).toList();
        Map<String, String> metadata = createReasoningMetadata(reasoning, reasoningSource, usage);
        if (!calls.isEmpty()) {
            // results may return in approval order, so retain the original response as one group
            metadata.put(OpenAIConstants.RESPONSE_OUTPUT, JSONUtils.GSON.toJson(output));
            for (AIFunctionCall call : calls) {
                Map<String, String> callMetadata = new HashMap<>(metadata);
                if (call.getMessageMetadata() != null) {
                    callMetadata.putAll(call.getMessageMetadata());
                }
                call.setMessageMetadata(callMetadata);
            }
        }
        return metadata;
    }

    @NotNull
    public static OAIResponsesRequest createOpenAiRequest(
        @NotNull AIEngineRequest request,
        @Nullable String modelName,
        @Nullable Double temperature
    ) {
        return createOpenAiRequest(request, modelName, temperature, null);
    }

    @NotNull
    public static OAIResponsesRequest createOpenAiRequest(
        @NotNull AIEngineRequest request,
        @Nullable String modelName,
        @Nullable Double temperature,
        @Nullable String reasoningSource
    ) {
        return createOpenAiRequest(request, modelName, temperature, reasoningSource, false);
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
        oaiRequest.include = new String[] {"reasoning.encrypted_content"};
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
        List<OAIMessage> outputs = new ArrayList<>();
        List<String> previousReasoning = null;
        Set<String> replayedResponses = new HashSet<>();
        int turnStart = 0;
        for (int index = 0; index < messages.size(); index++) {
            AIMessage message = messages.get(index);
            if (message.getRole() == AIMessageType.USER) {
                replayedResponses.clear();
                turnStart = index;
            }
            Map<String, String> responseMetadata = getResponseMetadata(message);
            String responseOutput = responseMetadata.get(OpenAIConstants.RESPONSE_OUTPUT);
            if (responseOutput != null) {
                result.addAll(outputs);
                outputs.clear();
                previousReasoning = null;
                if (replayedResponses.contains(responseOutput)) {
                    continue;
                }
                List<OAIMessage> restored = restoreResponseOutput(
                    messages, turnStart, responseOutput, reasoningSource, reasoningRequired);
                if (restored == null) {
                    result.add(importResponse(message));
                } else {
                    replayedResponses.add(responseOutput);
                    result.addAll(restored);
                }
                continue;
            }
            AIFunctionCall call = message.getFunctionCall();
            if (call != null) {
                Map<String, String> metadata = call.getMessageMetadata();
                if (reasoningRequired && (metadata == null || !metadata.containsKey(OpenAIConstants.TOOL_CALL_REASONING))
                    || metadata != null && (metadata.containsKey(OpenAIConstants.TOOL_CALL_REASONING)
                    || metadata.containsKey(OpenAIConstants.REASONING_SOURCE))
                    && !isReasoningCompatible(metadata, reasoningSource)
                ) {
                    result.addAll(outputs);
                    outputs.clear();
                    previousReasoning = null;
                    // foreign tool state cannot be replayed as this provider's function call
                    result.add(importResponse(message));
                    continue;
                }
                OAIMessage functionCallMessage = OAIMessageFactory.fromAIMessage(message);
                if (!CommonUtils.isEmpty(functionCallMessage.callId)) {
                    String reasoning = call.getMessageMetadata() == null ? null
                        : call.getMessageMetadata().get(OpenAIConstants.TOOL_CALL_REASONING);
                    List<OAIMessage> reasoningItems = reasoning == null ? List.of()
                        : List.of(JSONUtils.GSON.fromJson(reasoning, OAIMessage[].class));
                    List<String> reasoningKeys = reasoningItems.stream().map(JSONUtils.GSON::toJson).toList();
                    boolean continuation = reasoning != null && previousReasoning != null
                        && isReasoningPrefix(previousReasoning, reasoningKeys);
                    if (!continuation) {
                        result.addAll(outputs);
                        outputs.clear();
                    }
                    // streamed calls can carry growing snapshots of the same reasoning group
                    int afterCall = metadata == null ? 0 : CommonUtils.toInt(metadata.get(OpenAIConstants.REASONING_AFTER_CALL));
                    int beforeCall = reasoningItems.size() - afterCall;
                    result.addAll(reasoningItems.subList(continuation ? previousReasoning.size() : 0, beforeCall));
                    previousReasoning = reasoning == null ? null : reasoningKeys;
                    result.add(functionCallMessage);
                    result.addAll(reasoningItems.subList(beforeCall, reasoningItems.size()));
                    outputs.add(OAIMessageFactory.fromAIMessage(message, functionCallMessage.callId));
                }
            } else {
                result.addAll(outputs);
                outputs.clear();
                previousReasoning = null;
                Map<String, String> metadata = responseMetadata;
                if (Boolean.parseBoolean(metadata.get(AIConstants.RESPONSE_INCOMPLETE))
                    || !metadata.isEmpty() && !isReasoningCompatible(metadata, reasoningSource)
                    || reasoningRequired && message.getRole() == AIMessageType.ASSISTANT
                        && !metadata.containsKey(OpenAIConstants.TOOL_CALL_REASONING)
                ) {
                    result.add(importResponse(message));
                    continue;
                }
                String reasoning = metadata.get(OpenAIConstants.TOOL_CALL_REASONING);
                if (reasoning != null) {
                    result.addAll(List.of(JSONUtils.GSON.fromJson(reasoning, OAIMessage[].class)));
                }
                result.add(OAIMessageFactory.fromAIMessage(message));
            }
        }
        result.addAll(outputs);
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
            .filter(state -> state.containsKey(OpenAIConstants.TOOL_CALL_REASONING)
                || state.containsKey(OpenAIConstants.REASONING_SOURCE) || state.containsKey(OpenAIConstants.RESPONSE_OUTPUT)
                || state.containsKey(AIConstants.RESPONSE_INCOMPLETE))
            .findFirst().orElse(Map.of());
    }

    @Nullable
    private static List<OAIMessage> restoreResponseOutput(
        @NotNull List<AIMessage> messages,
        int turnStart,
        @NotNull String responseOutput,
        @Nullable String reasoningSource,
        boolean reasoningRequired
    ) {
        Map<String, AIMessage> results = new HashMap<>();
        for (int index = turnStart; index < messages.size(); index++) {
            AIMessage message = messages.get(index);
            if (index > turnStart && message.getRole() == AIMessageType.USER) {
                break;
            }
            Map<String, String> metadata = getResponseMetadata(message);
            if (!responseOutput.equals(metadata.get(OpenAIConstants.RESPONSE_OUTPUT))) {
                continue;
            }
            if (!isReasoningCompatible(metadata, reasoningSource)
                || Boolean.parseBoolean(metadata.get(AIConstants.RESPONSE_INCOMPLETE))
            ) {
                return null;
            }
            if (message.getFunctionCall() != null) {
                results.put(metadata.get(OpenAIConstants.TOOL_RESULT_CALL_ID), message);
            }
        }
        List<OAIMessage> output = List.of(JSONUtils.GSON.fromJson(responseOutput, OAIMessage[].class));
        if (reasoningRequired && output.stream().noneMatch(item -> OAIMessage.TYPE_FUNCTION_REASONING.equals(item.type))) {
            return null;
        }
        List<OAIMessage> restored = new ArrayList<>(output);
        for (OAIMessage item : output) {
            if (OAIMessage.TYPE_FUNCTION_CALL.equals(item.type)) {
                AIMessage result = results.get(item.callId);
                if (result == null || CommonUtils.isEmpty(item.callId)) {
                    // incomplete groups must not introduce calls without their outputs
                    return null;
                }
                restored.add(OAIMessageFactory.fromAIMessage(result, item.callId));
            }
        }
        return restored;
    }

    private static boolean isReasoningPrefix(@NotNull List<String> prefix, @NotNull List<String> reasoning) {
        return prefix.size() <= reasoning.size() && prefix.equals(reasoning.subList(0, prefix.size()));
    }

    private static boolean isReasoningCompatible(@NotNull Map<String, String> metadata, @Nullable String reasoningSource) {
        return reasoningSource == null || reasoningSource.equals(metadata.get(OpenAIConstants.REASONING_SOURCE));
    }

    @NotNull
    private static OAIMessage importResponse(@NotNull AIMessage message) {
        // reasoning providers may reject assistant turns without their original state
        return OAIMessageFactory.fromAIMessage(AIMessage.userMessage("Previous assistant response:\n" + message.getContent())
            .withImages(message.getImages()));
    }

    public static int countContextTokens(
        @NotNull List<AIMessage> messages,
        @NotNull String reasoningSource,
        boolean reasoningRequired,
        @NotNull ToIntFunction<String> textCounter
    ) {
        List<OAIMessage> input = fromMessages(messages, reasoningSource, reasoningRequired);
        long tokens = 0;
        Set<String> retainedReasoning = new HashSet<>();
        for (OAIMessage message : input) {
            tokens += countTextTokens(message, textCounter);
            if (OAIMessage.TYPE_FUNCTION_REASONING.equals(message.type)) {
                retainedReasoning.add(JSONUtils.GSON.toJson(message));
            }
        }
        Map<String, ReasoningUsage> reportedTokens = new HashMap<>();
        for (AIMessage message : messages) {
            if (message.getFunctionCall() != null) {
                Map<String, String> metadata = message.getFunctionCall().getMessageMetadata();
                if (metadata != null) {
                    collectReasoningUsage(reportedTokens, metadata, 0);
                }
            } else if (message.getMeta() != null) {
                for (AIMessageMeta meta : message.getMeta()) {
                    collectReasoningUsage(
                        reportedTokens, meta.responseMetadata(), meta.usage() == null ? 0 : meta.usage().reasoningTokens());
                }
            }
        }
        for (ReasoningUsage usage : reportedTokens.values()) {
            if (retainedReasoning.containsAll(usage.keys())) {
                long estimatedTokens = usage.items().stream().mapToLong(item -> countTextTokens(item, textCounter)).sum();
                tokens += Math.max(0, usage.tokens() - estimatedTokens);
            }
        }
        return (int) Math.min(tokens, Integer.MAX_VALUE);
    }

    private static void collectReasoningUsage(
        @NotNull Map<String, ReasoningUsage> groups,
        @NotNull Map<String, String> metadata,
        int reportedTokens
    ) {
        String reasoning = metadata.get(OpenAIConstants.TOOL_CALL_REASONING);
        if (reasoning != null) {
            int tokens = Math.max(reportedTokens, CommonUtils.toInt(metadata.get(OpenAIConstants.REASONING_TOKENS)));
            List<OAIMessage> items = List.of(JSONUtils.GSON.fromJson(reasoning, OAIMessage[].class));
            if (items.isEmpty()) {
                return;
            }
            List<String> keys = items.stream().map(JSONUtils.GSON::toJson).toList();
            String group = keys.getFirst();
            ReasoningUsage previous = groups.get(group);
            if (previous == null || isReasoningPrefix(previous.keys(), keys)) {
                groups.put(group, new ReasoningUsage(items, keys, Math.max(tokens, previous == null ? 0 : previous.tokens())));
            } else if (isReasoningPrefix(keys, previous.keys())) {
                groups.put(group, new ReasoningUsage(previous.items(), previous.keys(), Math.max(tokens, previous.tokens())));
            } else {
                groups.put(reasoning, new ReasoningUsage(items, keys, tokens));
            }
        }
    }

    private record ReasoningUsage(@NotNull List<OAIMessage> items, @NotNull List<String> keys, int tokens) {
    }

    private static long countTextTokens(@NotNull OAIMessage message, @NotNull ToIntFunction<String> textCounter) {
        long tokens = textCounter.applyAsInt(message.getFullText());
        if (message.arguments != null) {
            tokens += textCounter.applyAsInt(message.arguments);
        }
        if (message.output != null) {
            tokens += textCounter.applyAsInt(message.output);
        }
        if (message.summary != null) {
            for (OAIMessageContent summary : message.summary) {
                tokens += summary.text == null ? 0 : textCounter.applyAsInt(summary.text);
            }
        }
        if (OAIMessage.TYPE_FUNCTION_REASONING.equals(message.type) && message.getFullText().isEmpty()
            && message.encryptedContent != null
        ) {
            tokens += textCounter.applyAsInt(message.encryptedContent);
        }
        return tokens;
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
