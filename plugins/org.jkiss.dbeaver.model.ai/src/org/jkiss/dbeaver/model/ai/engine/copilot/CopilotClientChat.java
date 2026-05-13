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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.*;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CopilotClientChat extends CopilotClientBase<CopilotChatRequest, CopilotChatResponseLegacy> {
    private static final Log log = Log.getLog(CopilotClientChat.class);
    private static final String DATA_EVENT = "data: ";
    private static final String DONE_EVENT = "[DONE]";

    private static final String CHAT_REQUEST_URL = "https://api.githubcopilot.com/chat/completions";

    public CopilotClientChat(@NotNull String authProviderBaseURL) {
        super(authProviderBaseURL);
    }


    /**
     * Chat with Copilot
     */
    @NotNull
    public CopilotChatResponseLegacy chat(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String token,
        @NotNull CopilotChatRequest chatRequest
    ) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(CHAT_REQUEST_URL))
            .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON)
            .header(HttpConstants.HEADER_AUTHORIZATION, "Bearer " + token)
            .header("Editor-Version", CHAT_EDITOR_VERSION)
            .POST(HttpRequest.BodyPublishers.ofString(CopilotUtils.GSON.toJson(chatRequest)))
            .timeout(TIMEOUT)
            .build();

        String responseJson = client.send(monitor, request);
        return CopilotUtils.GSON.fromJson(responseJson, CopilotChatResponseLegacy.class);
    }

    @Override
    public void createChatCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String token,
        @NotNull CopilotChatRequest chatRequest,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(CHAT_REQUEST_URL))
            .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON)
            .header(HttpConstants.HEADER_AUTHORIZATION, "Bearer " + token)
            .header("Editor-Version", CHAT_EDITOR_VERSION)
            .POST(HttpRequest.BodyPublishers.ofString(CopilotUtils.GSON.toJson(chatRequest)))
            .timeout(TIMEOUT)
            .build();

        client.sendAsync(
            request,
            new StreamConsumer(listener),
            listener::error,
            listener::completeBlock
        );

    }

    private static class StreamConsumer implements Consumer<String> {
        private static final String FINISH_REASON_TOOL_CALLS = "tool_calls";

        private final AIEngineResponseConsumer listener;
        private final Map<Integer, CopilotToolCallAccumulator> toolCalls = new HashMap<>();

        private StreamConsumer(@NotNull AIEngineResponseConsumer listener) {
            this.listener = listener;
        }

        @Override
        public void accept(@NotNull String line) {
            if (!line.startsWith(DATA_EVENT)) {
                return;
            }

            String data = line.substring(DATA_EVENT.length()).trim();
            if (DONE_EVENT.equals(data)) {
                return;
            }

            try {
                handleChunk(CopilotUtils.GSON.fromJson(data, CopilotChatChunk.class));
            } catch (Exception e) {
                listener.error(e);
            }
        }

        private void handleChunk(@NotNull CopilotChatChunk chunk) throws DBException {
            for (CopilotChunkChoice choice : chunk.choices()) {
                if (FINISH_REASON_TOOL_CALLS.equals(choice.finishReason())) {
                    flushToolCalls();
                }

                CopilotChunkDelta delta = choice.delta();
                if (delta == null) {
                    continue;
                }

                if (delta.content() != null) {
                    listener.nextChunk(new AIEngineResponseChunk(List.of(delta.content())));
                }

                accumulateToolCalls(delta.toolCalls());
            }

            listener.usage(chunk.getAIUsage());
        }

        private void accumulateToolCalls(@Nullable List<CopilotChatResponseLegacy.ToolCall> deltaToolCalls) {
            if (CommonUtils.isEmpty(deltaToolCalls)) {
                return;
            }

            for (CopilotChatResponseLegacy.ToolCall toolCall : deltaToolCalls) {
                CopilotToolCallAccumulator accumulator = toolCalls.computeIfAbsent(
                    toolCall.index(),
                    key -> new CopilotToolCallAccumulator()
                );
                if (toolCall.id() != null) {
                    accumulator.setId(toolCall.id());
                }

                CopilotChatResponseLegacy.Function function = toolCall.function();
                if (function == null) {
                    continue;
                }
                if (function.name() != null) {
                    accumulator.setName(function.name());
                }
                if (function.arguments() != null) {
                    accumulator.appendArguments(function.arguments());
                }
            }
        }

        private void flushToolCalls() throws DBException {
            if (toolCalls.isEmpty()) {
                return;
            }

            for (CopilotToolCallAccumulator accumulator : toolCalls.values()) {
                listener.nextChunk(new AIEngineResponseChunk(CopilotUtils.createFunctionCall(accumulator)));
            }
            toolCalls.clear();
        }
    }


}
