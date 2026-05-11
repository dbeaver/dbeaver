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

import com.google.gson.JsonSyntaxException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIFunctionCall;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotResponsesResponse;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIClient;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.*;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


public class CopilotClient extends CopilotBaseClient<CopilotResponsesRequest, CopilotResponsesResponse>{
    private static final Log log = Log.getLog(CopilotClient.class);

    private static final String CHAT_REQUEST_URL = "https://api.githubcopilot.com/v1/responses";

    private static final String DATA_EVENT = "data: ";
    private static final String EVENT_EVENT = "event: ";


    public static final String EVENT_TYPE_RESPONSE_COMPLETED = "response.completed";
    public static final String EVENT_TYPE_ITEM_DONE = "response.output_item.done";
    public static final String EVENT_TYPE_ARGUMENTS_DELTA = "response.function_call_arguments.delta";
    public static final String EVENT_TYPE_TEXT_DELTA = "response.output_text.delta";

    protected CopilotClient(@NotNull String baseAuthURL) {
        super(baseAuthURL);
    }

    @NotNull
    private static AIFunctionCall createFunctionCall(OAIMessage message) throws DBException {
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
    public HttpClient getHttpClient() {
        return client.getHttpClient();
    }

    @NotNull
    public List<OAIModel> getModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(CHAT_REQUEST_URL, "models"))
            .GET()
            .timeout(TIMEOUT)
            .build();

        return CopilotUtils.GSON.fromJson(client.send(monitor, request), OAIModelList.class).data();
    }

    private HttpRequest createCompletionRequest(@NotNull CopilotResponsesRequest completionRequest, String token) throws DBException {
        return HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(CHAT_REQUEST_URL))
            .header(HttpConstants.HEADER_AUTHORIZATION, "Bearer " + token)
            .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON)
            .header("Editor-Version", CHAT_EDITOR_VERSION)
            .POST(HttpRequest.BodyPublishers.ofString(serializeValue(completionRequest)))
            .timeout(TIMEOUT)
            .build();
    }

    @NotNull
    public CopilotResponsesResponse chat(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String token,
        @NotNull CopilotResponsesRequest chatRequest
    ) throws DBException {
        HttpRequest request = createCompletionRequest(chatRequest, token);

        String responseJson = client.send(monitor, request);
        return CopilotUtils.GSON.fromJson(responseJson, CopilotResponsesResponse.class);
    }

    public void createChatCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String token,
        @NotNull CopilotResponsesRequest chatRequest,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        chatRequest.stream = true;
        HttpRequest request = createCompletionRequest(chatRequest, token);

        Consumer<String> stringConsumer = new StreamConsumer(listener);
        client.sendAsync(
            request,
            stringConsumer,
            listener::error,
            listener::completeBlock
        );
    }

    @NotNull
    protected static String serializeValue(@Nullable Object value) throws DBException {
        try {
            return CopilotUtils.GSON.toJson(value);
        } catch (Exception e) {
            throw new DBException("Error serializing value", e);
        }
    }

    private static class StreamConsumer implements Consumer<String> {
        private final AIEngineResponseConsumer listener;
        private boolean functionCall;

        public StreamConsumer(AIEngineResponseConsumer listener) {
            this.listener = listener;
        }

        @Override
        public void accept(String event) {
            if (CommonUtils.isEmpty(event)) {
                return;
            }
            if (event.startsWith(DATA_EVENT)) {
                String data = event.substring(DATA_EVENT.length()).trim();
                try {
                    OAIResponsesChunk chunk = CopilotUtils.GSON.fromJson(data, OAIResponsesChunk.class);
                    if (chunk.error != null) {
                        listener.error(new DBException(chunk.error.code + ": " + chunk.error.message));
                        return;
                    }
                    if (EVENT_TYPE_RESPONSE_COMPLETED.equals(chunk.type)) {
                        listener.usage(chunk.response.getAIUsage());
                    } else {

                        if (chunk.item != null && OAIMessage.TYPE_FUNCTION_CALL.equals(chunk.item.type)) {
                            if (EVENT_TYPE_ITEM_DONE.equals(chunk.type)) {
                                listener.nextChunk(new AIEngineResponseChunk(
                                    createFunctionCall(chunk.item)));
                                functionCall = false;
                            } else {
                                functionCall = true;
                            }
                            return;
                        }
                        if (functionCall) {
                            // do nothing
                        } else {
                            List<String> choices = new ArrayList<>();
                            if (OpenAIClient.EVENT_TYPE_TEXT_DELTA.equals(chunk.type)) {
                                choices.add(chunk.delta);
                            } else if (chunk.response != null) {
                                for (OAIMessage msg : chunk.response.output) {
                                    for (OAIMessageContent content : msg.content) {
                                        if (!CommonUtils.isEmpty(content.text)) {
                                            choices.add(content.text);
                                        }
                                    }
                                }
                            }

                            if (!choices.isEmpty()) {
                                listener.nextChunk(new AIEngineResponseChunk(choices));
                            }
                        }
                    }
                } catch (Exception e) {
                    listener.error(e);
                }
            } else if (event.startsWith(EVENT_EVENT)) {
                String eventType = event.substring(EVENT_EVENT.length()).trim();
                if (!CommonUtils.isEmpty(eventType)) {
                    switch (eventType) {
                        case "response.created":
                        case "response.in_progress":
                        case "response.output_item.added":
                        case EVENT_TYPE_TEXT_DELTA:
                        case "response.output_text.done":
                        case "response.content_part.done":
                        case "response.output_item.done":
                        case EVENT_TYPE_RESPONSE_COMPLETED:
                            break;
                        case "error":
                            break;
                    }
                }
            } else {
                log.debug("Unknown Copilot event: " + event);
            }
        }
    }
}
