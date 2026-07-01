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
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotChatRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAiAPIStreamConsumer;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAiUtils;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIModel;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIModelList;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesRequest;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIResponsesResponse;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.HttpConstants;
import org.jkiss.utils.Pair;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class CopilotClientResponses extends CopilotClientBase<Pair<OAIResponsesRequest, CopilotChatRequest>, Object> {
    private static final Log log = Log.getLog(CopilotClientResponses.class);
    private static final Set<String> MODELS_WITHOUT_TEMPERATURE = new HashSet<>();
    private static final String CHAT_REQUEST_URL = "https://api.githubcopilot.com/v1/responses";

    private final CopilotClientChat backupClient;

    protected CopilotClientResponses(@NotNull String baseAuthURL) {
        super(baseAuthURL);
        backupClient = createLegacyBackupClient();
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

    @NotNull
    private HttpRequest createCompletionRequest(@NotNull OAIResponsesRequest completionRequest, @NotNull String token) throws DBException {
        if (completionRequest.model != null && MODELS_WITHOUT_TEMPERATURE.contains(completionRequest.model)) {
            completionRequest.temperature = null;
        }
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
    public Object chat(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String token,
        @NotNull Pair<OAIResponsesRequest, CopilotChatRequest> chatRequest
    ) throws DBException {
        HttpRequest request = createCompletionRequest(chatRequest.getFirst(), token);
        try {
            String responseJson = client.send(monitor, request);
            return CopilotUtils.GSON.fromJson(responseJson, OAIResponsesResponse.class);
        } catch (DBException e) {
            if (e.getMessage() != null && e.getMessage().contains("is not supported via Responses API")) {
                return backupClient.chat(monitor, token, chatRequest.getSecond());
            } else {
                log.error("Error in chat request, falling back to legacy client", e);
                throw e;
            }
        }
    }

    public void createChatCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String token,
        @NotNull Pair<OAIResponsesRequest, CopilotChatRequest> chatRequest,
        @NotNull AIEngineResponseConsumer listener
    ) throws DBException {
        chatRequest.getFirst().stream = true;
        HttpRequest request = createCompletionRequest(chatRequest.getFirst(), token);

        Consumer<String> stringConsumer = new OpenAiAPIStreamConsumer(listener);
        client.sendAsync(
            request,
            stringConsumer,
            listener::error,
            listener::completeBlock,
            (failureReason) -> {
                if (OpenAIConstants.LEGACY_FALLBACK.equals(failureReason)) {
                    try {
                        backupClient.createChatCompletionStream(monitor, token, chatRequest.getSecond(), listener);
                    } catch (DBException ex) {
                        log.error("Error in legacy client fallback", ex);
                        listener.error(ex);
                    }
                } else if (OpenAIConstants.TEMPERATURE_NOT_SUPPORTED.equals(failureReason)) {
                    chatRequest.getFirst().temperature = null;
                    MODELS_WITHOUT_TEMPERATURE.add(chatRequest.getFirst().model);
                    try {
                        createChatCompletionStream(monitor, token, chatRequest, listener);
                    } catch (DBException e) {
                        log.error("Error in client fallback", e);
                        listener.error(e);
                    }
                }
            }
        );
    }

    @Override
    public void close() {
        super.close();
        backupClient.close();
    }

    @NotNull
    @Override
    protected DBException mapHttpError(int statusCode, @NotNull String body) {
        if (statusCode == 400) {
            if (body.contains("is not supported via Responses API")) {
                // just return DBException, we will fall back to legacy client in case of this error, no need to log it as error
                return new DBException("Copilot request failed: " + AIHttpUtils.parseOpenAIStyleErrorMessage(body));
            }
        }
        return super.mapHttpError(statusCode, body);
    }

    @NotNull
    protected static String serializeValue(@Nullable Object value) throws DBException {
        try {
            return CopilotUtils.GSON.toJson(value);
        } catch (Exception e) {
            throw new DBException("Error serializing value", e);
        }
    }

    @NotNull
    protected CopilotClientChat createLegacyBackupClient() {
        return new CopilotClientChat(baseAuthURL);
    }

    @Override
    protected boolean processErrors(
        @NotNull MonitoredHttpClient.ErrorMapper mapper,
        @NotNull Consumer<Throwable> errorHandler,
        @NotNull HttpResponse<Stream<String>> response,
        @NotNull AtomicBoolean suppressCompletion,
        @Nullable Consumer<String> backupOption,
        int statusCode
    ) {
        return OpenAiUtils.processErrors(mapper, errorHandler, response, suppressCompletion, backupOption, statusCode);
    }
}
