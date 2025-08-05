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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.TooManyRequestsException;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.*;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.Closeable;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class OpenAIClient implements Closeable {
    public static final String OPENAI_ENDPOINT = "https://api.openai.com/v1/";

    private static final String DATA_EVENT = "data: ";
    private static final String DONE_EVENT = "[DONE]";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Gson GSON = new GsonBuilder().create();

    private final String baseUrl;
    private final List<HttpRequestFilter> requestFilters;
    private final MonitoredHttpClient client = new MonitoredHttpClient(HttpClient.newBuilder().build());

    public OpenAIClient(
        @NotNull String baseUrl,
        @NotNull List<HttpRequestFilter> requestFilters
    ) {
        this.baseUrl = baseUrl;
        this.requestFilters = requestFilters;
    }

    @NotNull
    public HttpClient getHttpClient() {
        return client.getHttpClient();
    }

    public static OpenAIClient createClient(String token) {
        return new OpenAIClient(
            OPENAI_ENDPOINT,
            List.of(new OpenAIRequestFilter(token))
        );
    }

    @NotNull
    public List<Model> getModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(baseUrl, "models"))
            .GET()
            .timeout(TIMEOUT)
            .build();

        HttpRequest modifiedRequest = applyFilters(request);
        HttpResponse<String> response = client.send(monitor, modifiedRequest);
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), ModelList.class).data();
        } else {
            throw new DBException("Request failed: " + response.statusCode() + ", body=" + response.body());
        }
    }

    @NotNull
    public ChatCompletionResult createChatCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull ChatCompletionRequest completionRequest
    ) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(baseUrl, "chat/completions"))
            .POST(HttpRequest.BodyPublishers.ofString(serializeValue(completionRequest)))
            .timeout(TIMEOUT)
            .build();

        HttpRequest modifiedRequest = applyFilters(request);
        HttpResponse<String> response = client.send(monitor, modifiedRequest);
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), ChatCompletionResult.class);
        } else if (response.statusCode() == 429) {
            throw new TooManyRequestsException("Too many requests: " + response.body());
        } else {
            throw new DBException("Request failed: " + response.statusCode() + ", body=" + response.body());
        }
    }

    @NotNull
    public Flow.Publisher<ChatCompletionChunk> createChatCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull ChatCompletionRequest completionRequest
    ) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(baseUrl, "chat/completions"))
            .POST(HttpRequest.BodyPublishers.ofString(serializeValue(completionRequest)))
            .timeout(TIMEOUT)
            .build();

        HttpRequest modifiedRequest = applyFilters(request);

        SubmissionPublisher<ChatCompletionChunk> publisher = new SubmissionPublisher<>();

        client.sendAsync(
            modifiedRequest,
            event -> {
                if (event.startsWith(DATA_EVENT)) {
                    String data = event.substring(6).trim();
                    if (DONE_EVENT.equals(data)) {
                        publisher.close();
                    } else {
                        try {
                            ChatCompletionChunk chunk = GSON.fromJson(data, ChatCompletionChunk.class);
                            publisher.submit(chunk);
                        } catch (Exception e) {
                            publisher.closeExceptionally(e);
                        }
                    }
                }
            },
            publisher::closeExceptionally,
            publisher::close
        );

        return publisher;
    }

    @Override
    public void close() {
        client.close();
    }

    public HttpRequest applyFilters(@NotNull HttpRequest request) throws DBException {
        return applyFilters(request, true);
    }

    public HttpRequest applyFilters(@NotNull HttpRequest request, boolean setContentType) throws DBException {
        for (HttpRequestFilter filter : requestFilters) {
            request = filter.filter(request, setContentType);
        }
        return request;
    }

    @Nullable
    private static String serializeValue(@Nullable Object value) throws DBException {
        try {
            return GSON.toJson(value);
        } catch (Exception e) {
            throw new DBException("Error serializing value", e);
        }
    }

    public interface HttpRequestFilter {
        @NotNull
        HttpRequest filter(@NotNull HttpRequest request, boolean setContentType) throws DBException;
    }
}
