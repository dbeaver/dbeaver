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
package org.jkiss.dbeaver.model.ai.engine.ollama;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AIUsage;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.AbstractHttpAIClient;
import org.jkiss.dbeaver.model.ai.engine.ollama.dto.*;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.GsonUtils;
import org.jkiss.utils.HttpConstants;

import java.net.http.HttpRequest;
import java.time.Instant;
import java.util.List;

public class OllamaClient extends AbstractHttpAIClient {
    private static final Log log = Log.getLog(OllamaClient.class);
    private static final Gson GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .serializeNulls()
        .registerTypeAdapter(Instant.class, new GsonUtils.InstantIsoAdapter())
        .create();


    private final String baseUrl;
    private final String token;

    public OllamaClient(@NotNull String baseUrl) {
        this(baseUrl, null);
    }

    public OllamaClient(@NotNull String baseUrl, @Nullable String token) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    @NotNull
    public List<OllamaModel> loadModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        HttpRequest request = newRequestBuilder()
            .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON)
            .uri(AIHttpUtils.resolve(baseUrl, "/api/tags"))
            .GET()
            .timeout(timeout)
            .build();

        return GSON.fromJson(client.send(monitor, request), OllamaModels.class).models();
    }

    @NotNull
    public OllamaChatResponse chat(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OllamaChatRequest chatRequest
    ) throws DBException {
        HttpRequest request = newRequestBuilder()
            .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON)
            .uri(AIHttpUtils.resolve(baseUrl, "/api/chat"))
            .POST(HttpRequest.BodyPublishers.ofString(serializeChatRequest(chatRequest)))
            .timeout(timeout)
            .build();

        String responseJson = client.send(monitor, request);
        return GSON.fromJson(responseJson, OllamaChatResponse.class);
    }

    @NotNull
    private String serializeChatRequest(@NotNull OllamaChatRequest request) {
        JsonObject json = GSON.toJsonTree(request).getAsJsonObject();
        json.remove("useServerDefaultTemperature");
        if (request.useServerDefaultTemperature()) {
            json.getAsJsonObject("options").remove("temperature");
        }
        return GSON.toJson(json);
    }

    @NotNull
    @Override
    protected DBException mapHttpError(int statusCode, @NotNull String body) {
        log.debug("Ollama request failed: " + statusCode + ", " + body);
        return new DBException("Ollama request failed: " + AIHttpUtils.parseOpenAIStyleErrorMessage(statusCode, body));
    }

    public void createChatCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OllamaChatRequest completionRequest,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        HttpRequest request = newRequestBuilder()
            .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON)
            .uri(AIHttpUtils.resolve(baseUrl, "/api/chat")) //$NON-NLS-1$
            .POST(HttpRequest.BodyPublishers.ofString(serializeChatRequest(completionRequest)))
            .timeout(timeout)
            .build();

        client.sendAsync(
            request,
            line -> {
                try {
                    OllamaChatChunk chunk = GSON.fromJson(line, OllamaChatChunk.class);
                    OllamaChatMessage message = chunk.message();
                    List<OllamaToolCall> toolCalls = message == null ? null : message.toolCalls();
                    if (!CommonUtils.isEmpty(toolCalls)) {
                        if (CommonUtils.isNotEmpty(message.content())) {
                            listener.nextChunk(new AIEngineResponseChunk(List.of(message.content())));
                        }
                        for (OllamaToolCall toolCall : toolCalls) {
                            listener.nextChunk(new AIEngineResponseChunk(toolCall.toFunctionCall()));
                        }
                    } else if (!chunk.done() && message != null && message.content() != null) {
                        listener.nextChunk(
                            new AIEngineResponseChunk(
                                List.of(message.content())
                            )
                        );
                    }
                    if (chunk.done()) {
                        listener.usage(new AIUsage(
                            chunk.promptEvalCount(),
                            0,
                            chunk.evalCount(),
                            0
                        ));
                    }
                } catch (Exception e) {
                    listener.error(e);
                }
            },
            listener::error,
            listener::completeBlock
        );
    }

    @NotNull
    HttpRequest.Builder newRequestBuilder() {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        if (!CommonUtils.isEmpty(token)) {
            builder.header(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.BEARER_PREFIX + token);
        }
        return builder;
    }
}
