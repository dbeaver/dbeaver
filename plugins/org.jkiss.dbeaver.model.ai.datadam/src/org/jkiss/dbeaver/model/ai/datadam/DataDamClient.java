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
package org.jkiss.dbeaver.model.ai.datadam;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIUsage;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClientChat;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClientResponses;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesResponse;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.net.http.HttpRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DataDamClient extends OpenAIClientResponses {

    private static final String DATA_EVENT = "data: ";
    private static final String DONE_MARKER = "[DONE]";

    private final OpenAIClientChat chatClient;

    public DataDamClient(@NotNull String baseUrl, @NotNull List<HttpRequestFilter> requestFilters) {
        super(baseUrl, requestFilters);
        this.chatClient = new OpenAIClientChat(baseUrl, requestFilters);
    }

    @NotNull
    @Override
    public OAIResponsesResponse createChatCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OAIResponsesRequest completionRequest
    ) throws DBException {
        return chatClient.createChatCompletion(monitor, completionRequest);
    }

    @Override
    public void createChatCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OAIResponsesRequest completionRequest,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(baseUrl, OpenAIConstants.ENDPOINT_CHAT))
            .POST(HttpRequest.BodyPublishers.ofString(serializeValue(toChatRequest(completionRequest))))
            .timeout(TIMEOUT)
            .build();

        client.sendAsync(
            applyFilters(request),
            new ChatStreamConsumer(listener),
            listener::error,
            listener::completeBlock
        );
    }

    @Override
    public void close() {
        super.close();
        chatClient.close();
    }

    @NotNull
    private static Map<String, Object> toChatRequest(@NotNull OAIResponsesRequest completionRequest) {
        List<Map<String, Object>> messages = completionRequest.input.stream()
            .map(om -> {
                Map<String, Object> message = new LinkedHashMap<>();
                message.put("role", om.role);
                message.put("content", om.content.getFirst().text);
                if (om.name != null) {
                    message.put("name", om.name);
                }
                return message;
            })
            .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", completionRequest.model);
        body.put("messages", messages);
        body.put("stream", true);
        if (completionRequest.temperature != null) {
            body.put("temperature", completionRequest.temperature);
        }
        return body;
    }

    private static class ChatStreamConsumer implements Consumer<String> {
        private final AIEngineResponseConsumer listener;

        ChatStreamConsumer(@NotNull AIEngineResponseConsumer listener) {
            this.listener = listener;
        }

        @Override
        public void accept(@Nullable String event) {
            if (CommonUtils.isEmpty(event) || !event.startsWith(DATA_EVENT)) {
                return;
            }
            String data = event.substring(DATA_EVENT.length()).trim();
            if (DONE_MARKER.equals(data)) {
                return;
            }
            try {
                ChatChunk chunk = GSON.fromJson(data, ChatChunk.class);
                if (chunk == null) {
                    return;
                }
                if (chunk.choices != null) {
                    List<String> deltas = chunk.choices.stream()
                        .map(choice -> choice.delta != null ? choice.delta.content : null)
                        .filter(text -> !CommonUtils.isEmpty(text))
                        .toList();
                    if (!deltas.isEmpty()) {
                        listener.nextChunk(new AIEngineResponseChunk(deltas));
                    }
                }
                if (chunk.usage != null) {
                    listener.usage(chunk.usage.toAIUsage());
                }
            } catch (Exception e) {
                listener.error(e);
            }
        }
    }

    private static final class ChatChunk {
        private List<ChatChunkChoice> choices;
        private ChatUsage usage;
    }

    private static final class ChatChunkChoice {
        private ChatChunkDelta delta;
    }

    private static final class ChatChunkDelta {
        private String content;
    }

    private static final class ChatUsage {
        @SerializedName("prompt_tokens")
        private int promptTokens;
        @SerializedName("completion_tokens")
        private int completionTokens;
        @SerializedName("prompt_tokens_details")
        private ChatTokenDetails promptTokensDetails;
        @SerializedName("completion_tokens_details")
        private ChatTokenDetails completionTokensDetails;

        @NotNull
        private AIUsage toAIUsage() {
            int cachedTokens = promptTokensDetails != null ? promptTokensDetails.cachedTokens : 0;
            int reasoningTokens = completionTokensDetails != null ? completionTokensDetails.reasoningTokens : 0;
            return new AIUsage(promptTokens, cachedTokens, completionTokens, reasoningTokens);
        }
    }

    private static final class ChatTokenDetails {
        @SerializedName("cached_tokens")
        private int cachedTokens;
        @SerializedName("reasoning_tokens")
        private int reasoningTokens;
    }
}
