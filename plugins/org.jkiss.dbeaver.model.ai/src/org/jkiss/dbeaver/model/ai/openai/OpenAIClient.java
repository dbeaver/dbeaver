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
package org.jkiss.dbeaver.model.ai.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.TooManyRequestsException;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class OpenAIClient {
    private static final Log log = Log.getLog(OpenAIClient.class);

    private static final String DATA_EVENT = "data: ";
    private static final String DONE_EVENT = "[DONE]";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

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
            return deserializeValue(response.body(), ChatCompletionResult.class);
        } else if (response.statusCode() == 429) {
            throw new TooManyRequestsException("Too many requests: " + response.body());
        } else {
            log.error("Request failed [status=" + response.statusCode() + ", body=" + response.body() + "]");

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
                            ChatCompletionChunk chunk = MAPPER.readValue(data, ChatCompletionChunk.class);
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

    public void close() {
        client.close();
    }

    private HttpRequest applyFilters(HttpRequest request) throws DBException {
        for (HttpRequestFilter filter : requestFilters) {
            request = filter.filter(request);
        }
        return request;
    }

    @Nullable
    private static String serializeValue(@Nullable Object value) throws DBException {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new DBException("Error serializing value", e);
        }
    }

    @NotNull
    private static <T> T deserializeValue(@NotNull String value, @NotNull Class<T> type) throws DBException {
        try {
            return MAPPER.readValue(value, type);
        } catch (Exception e) {
            throw new DBException("Error deserializing value", e);
        }
    }

    public interface HttpRequestFilter {
        @NotNull
        HttpRequest filter(@NotNull HttpRequest request) throws DBException;
    }
}
