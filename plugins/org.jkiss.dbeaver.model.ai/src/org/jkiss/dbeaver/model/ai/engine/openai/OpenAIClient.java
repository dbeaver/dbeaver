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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.LegacyAPIException;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesResponse;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.function.Consumer;

public class OpenAIClient extends OpenAiClientBase {
    private static final Log log = Log.getLog(OpenAIClient.class);

    public static final String OPENAI_ENDPOINT = "https://api.openai.com/v1/";

    private final OpenAIClientLegacy backupClient;

    public OpenAIClient(
        @NotNull String baseUrl,
        @NotNull List<HttpRequestFilter> requestFilters
    ) {
        super(baseUrl, requestFilters);
        this.backupClient = createBackupClient();
    }

    @NotNull
    public HttpClient getHttpClient() {
        return client.getHttpClient();
    }

    @NotNull
    public static OpenAIClient createClient(@NotNull String baseUrl, @NotNull String token) {
        return new OpenAIClient(
            baseUrl,
            List.of(new OpenAIRequestFilter(token))
        );
    }

    @NotNull
    public OAIResponsesResponse createChatCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OAIResponsesRequest completionRequest
    ) throws DBException {
        HttpRequest request = createCompletionRequest(completionRequest);

        HttpRequest modifiedRequest = applyFilters(request);
        try {
            String responseJson = client.send(monitor, modifiedRequest);
            return GSON.fromJson(responseJson, OAIResponsesResponse.class);
        } catch (Exception exception) {
            if (exception.getMessage().contains("is not supported via Responses API")) {
                // If the request failed due to an unsupported model, fallback to the legacy client which might support it
                return backupClient.createChatCompletion(monitor, completionRequest);
            } else {
                throw exception;
            }
        }
    }

    public void createChatCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull OAIResponsesRequest completionRequest,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        HttpRequest request = createCompletionRequest(completionRequest);

        HttpRequest modifiedRequest = applyFilters(request);

        Consumer<String> stringConsumer = new OpenAiAPIStreamConsumer(listener);
        client.sendAsync(
            modifiedRequest,
            stringConsumer,
            listener::error,
            listener::completeBlock
        ).exceptionally(e -> {
            if (e instanceof LegacyAPIException) {
                // If the request failed due to an unsupported model, fallback to the legacy client which might support it
                try {
                    backupClient.createChatCompletionStream(monitor, completionRequest, listener);
                } catch (DBException ex) {
                    listener.error(ex);
                }
            }
            return null;
        });
    }

    @NotNull
    protected OpenAIClientLegacy createBackupClient() {
        return new OpenAIClientLegacy(baseUrl, requestFilters);
    }

    @Override
    public void close() {
        super.close();
        backupClient.close();
    }

}

