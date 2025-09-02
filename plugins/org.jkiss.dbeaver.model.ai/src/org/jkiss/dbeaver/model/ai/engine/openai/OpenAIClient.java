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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.engine.TooManyRequestsException;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.*;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.io.Closeable;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class OpenAIClient implements Closeable {
    private static final Log log = Log.getLog(OpenAIClient.class);

    public static final String OPENAI_ENDPOINT = "https://api.openai.com/v1/";

    private static final String DATA_EVENT = "data: ";
    private static final String EVENT_EVENT = "event: ";
    private static final String DONE_EVENT = "[DONE]";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Gson GSON = new GsonBuilder().create();
    public static final String EVENT_TYPE_RESPONSE_COMPLETED = "response.completed";
    public static final String EVENT_TYPE_TEXT_DELTA = "response.output_text.delta";

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

    public static OpenAIClient createClient(String baseUrl, String token) {
        return new OpenAIClient(
            baseUrl,
            List.of(new OpenAIRequestFilter(token))
        );
    }

    @NotNull
    public List<OAIModel> getModels(@NotNull DBRProgressMonitor monitor) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(baseUrl, "models"))
            .GET()
            .timeout(TIMEOUT)
            .build();

        HttpRequest modifiedRequest = applyFilters(request);
        HttpResponse<String> response = client.send(monitor, modifiedRequest);
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), OAIModelList.class).data();
        } else {
            throw new DBException("Request failed: " + response.statusCode() + ", body=" + response.body());
        }
    }

    private HttpRequest createCompletionRequest(@NotNull OAIResponsesRequest completionRequest) throws DBException {
        return HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(baseUrl, "responses"))
            .header(HttpConstants.HEADER_USER_AGENT, GeneralUtils.getProductTitle())
            .POST(HttpRequest.BodyPublishers.ofString(serializeValue(completionRequest)))
            .timeout(TIMEOUT)
            .build();
    }

    @NotNull
    public OAIResponsesResponse createChatCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OAIResponsesRequest completionRequest
    ) throws DBException {
        HttpRequest request = createCompletionRequest(completionRequest);

        HttpRequest modifiedRequest = applyFilters(request);
        HttpResponse<String> response = client.send(monitor, modifiedRequest);
        String body = response.body();
        if (response.statusCode() == 200) {
            return GSON.fromJson(body, OAIResponsesResponse.class);
        } else if (response.statusCode() == 429) {
            throw new TooManyRequestsException("Too many requests: " + body);
        } else {
            throw new DBException("Request failed: " + response.statusCode() + ", body=" + body);
        }
    }

    @NotNull
    public Flow.Publisher<OAIResponsesChunk> createChatCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OAIResponsesRequest completionRequest
    ) throws DBException {
        HttpRequest request = createCompletionRequest(completionRequest);

        HttpRequest modifiedRequest = applyFilters(request);

        SubmissionPublisher<OAIResponsesChunk> publisher = new SubmissionPublisher<>();

        client.sendAsync(
            modifiedRequest, //  "type" : "response.content_part.done"
            // {"type":"response.output_item.done","sequence_number":25,"output_index":0,"item":{"id":"msg_68b6f090a8d88195a69524dd04f8eac90c5742e9db37e5a3","type":"message","status":"completed","content":[{"type":"output_text","annotations":[],"logprobs":[],"text":"If you have any more questions or need further assistance with SQL queries, feel free to ask!"}],"role":"assistant"}},
            event -> {
                if (CommonUtils.isEmpty(event)) {
                    return;
                }
                if (event.startsWith(DATA_EVENT)) {
                    String data = event.substring(DATA_EVENT.length()).trim();
                    try {
                        OAIResponsesChunk chunk = GSON.fromJson(data, OAIResponsesChunk.class);
                        if (EVENT_TYPE_RESPONSE_COMPLETED.equals(chunk.type)) {
                            publisher.close();
                        } else {
                            publisher.submit(chunk);
                        }
                    } catch (Exception e) {
                        publisher.closeExceptionally(e);
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
                        }
                    }
                } else {
                    log.debug("Unknown OpenAI event: " + event);
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

    @NotNull
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
