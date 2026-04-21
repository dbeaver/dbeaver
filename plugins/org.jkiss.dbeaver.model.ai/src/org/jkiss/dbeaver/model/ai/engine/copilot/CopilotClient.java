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

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseChunk;
import org.jkiss.dbeaver.model.ai.engine.AIEngineResponseConsumer;
import org.jkiss.dbeaver.model.ai.engine.AbstractHttpAIClient;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.*;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class CopilotClient extends AbstractHttpAIClient {
    private static final Log log = Log.getLog(CopilotClient.class);
    private static final String DATA_EVENT = "data: ";
    private static final String DONE_EVENT = "[DONE]";
    protected static final Duration TIMEOUT = Duration.ofSeconds(30);


    private static final String EDITOR_VERSION = "Neovim/0.6.1"; // TODO replace after partnership
    private static final String EDITOR_PLUGIN_VERSION = "copilot.vim/1.16.0"; // TODO replace after partnership
    private static final String USER_AGENT = "GithubCopilot/1.155.0";
    protected static final String CHAT_EDITOR_VERSION = "vscode/1.80.1"; // TODO replace after partnership
    private static final String DBEAVER_OAUTH_APP = "Iv1.b507a08c87ecfe98";

    private static final String COPILOT_CHAT_MODELS_URL = "https://api.githubcopilot.com/models";
    private static final String CHAT_REQUEST_URL = "https://api.githubcopilot.com/chat/completions";
    private static final String COPILOT_SESSION_TOKEN_URL = "/copilot_internal/v2/token";

    @NotNull
    private final String baseAuthURL;

    public CopilotClient(@NotNull String authProviderBaseURL) {
        this.baseAuthURL = authProviderBaseURL;
    }

    /**
     * Request access to the user's account
     */
    @NotNull
    public DeviceCodeResponse requestDeviceCode(@NotNull DBRProgressMonitor monitor) throws DBException {
        DeviceCodeRequest deviceCodeRequest = new DeviceCodeRequest(DBEAVER_OAUTH_APP, "read:user");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve("https://github.com/login/device/code"))
            .header("accept", HttpConstants.CONTENT_TYPE_JSON)
            .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON)
            .timeout(Duration.ofSeconds(10)) // Set timeout
            .POST(HttpRequest.BodyPublishers.ofString(CopilotUtils.GSON.toJson(deviceCodeRequest)))
            .build();

        return CopilotUtils.GSON.fromJson(client.send(monitor, request), DeviceCodeResponse.class);
    }

    /**
     * Request access token
     */
    @NotNull
    public String requestAccessToken(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DeviceCodeResponse deviceCodeResponse,
        @NotNull Future<?> cancellationToken
    ) throws DBException, InterruptedException {
        AccessTokenRequest accessTokenRequest = new AccessTokenRequest(
            DBEAVER_OAUTH_APP,
            deviceCodeResponse.deviceCode(),
            "urn:ietf:params:oauth:grant-type:device_code"
        );
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve("https://github.com/login/oauth/access_token"))
            .header("accept", HttpConstants.CONTENT_TYPE_JSON)
            .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON)
            .timeout(Duration.ofSeconds(5)) // Set timeout
            .POST(HttpRequest.BodyPublishers.ofString(CopilotUtils.GSON.toJson(accessTokenRequest)))
            .build();

        Duration expiresIn = Duration.ofSeconds(deviceCodeResponse.expiresIn());
        Duration interval = Duration.ofSeconds(deviceCodeResponse.interval());
        Instant start = Instant.now();

        while (Instant.now().isBefore(start.plus(expiresIn)) &&
            !monitor.isCanceled() && !cancellationToken.isCancelled()) {
            String responseString = client.send(monitor, request);
            var body = CopilotUtils.GSON.fromJson(responseString, AccessTokenResponse.class);
            if (CommonUtils.isNotEmpty(body.accessToken())) {
                return body.accessToken();
            }
            switch (body.error()) {
                // https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#error-codes-for-the-device-flow
                case "authorization_pending" -> Thread.sleep(interval.toMillis());
                case "slow_down" -> Thread.sleep(interval.plusSeconds(5).toMillis());
                default -> throw new DBException("Error requesting access token: " + body.error());
            }
        }

        if (monitor.isCanceled() || cancellationToken.isCancelled()) {
            throw new DBException("Access token request was canceled by the user");
        } else {
            throw new DBException("Access token request timed out");
        }
    }

    /**
     * Request session token
     */
    @NotNull
    public CopilotSessionToken requestSessionToken(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String accessToken
    ) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(baseAuthURL + COPILOT_SESSION_TOKEN_URL))
            .header(HttpConstants.HEADER_AUTHORIZATION, "token " + accessToken)
            .header("editor-version", EDITOR_VERSION)
            .header("editor-plugin-version", EDITOR_PLUGIN_VERSION)
            .header(HttpConstants.HEADER_USER_AGENT, USER_AGENT)
            .GET()
            .timeout(TIMEOUT)
            .build();

        return CopilotUtils.GSON.fromJson(client.send(monitor, request), CopilotSessionToken.class);
    }

    /**
     * Chat with Copilot
     */
    @NotNull
    public CopilotChatResponse chat(
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
        return CopilotUtils.GSON.fromJson(responseJson, CopilotChatResponse.class);
    }

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

        private void accumulateToolCalls(@Nullable List<CopilotChatResponse.ToolCall> deltaToolCalls) {
            if (CommonUtils.isEmpty(deltaToolCalls)) {
                return;
            }

            for (CopilotChatResponse.ToolCall toolCall : deltaToolCalls) {
                CopilotToolCallAccumulator accumulator = toolCalls.computeIfAbsent(
                    toolCall.index(),
                    key -> new CopilotToolCallAccumulator()
                );
                if (toolCall.id() != null) {
                    accumulator.setId(toolCall.id());
                }

                CopilotChatResponse.Function function = toolCall.function();
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

    @NotNull
    @Override
    protected DBException mapHttpError(int statusCode, @NotNull String body) {
        log.debug("Copilot request failed: " + statusCode + ", " + body);
        return new DBException("Copilot request failed: " + AIHttpUtils.parseOpenAIStyleErrorMessage(body));
    }

    /**
     * Loads a list of available Copilot models from the server.
     *
     * @param monitor the progress monitor to track the request's progress and handle cancellation
     * @param token the authorization token used to authenticate the request
     * @return a list of {@code CopilotModel} objects representing the enabled models
     * @throws DBException if the request fails
     */
    public List<CopilotModel> loadModels(@NotNull DBRProgressMonitor monitor, @NotNull String token) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(COPILOT_CHAT_MODELS_URL))
            .header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON)
            .header(HttpConstants.HEADER_AUTHORIZATION, "Bearer " + token)
            .header("Editor-Version", CHAT_EDITOR_VERSION)
            .GET()
            .timeout(TIMEOUT)
            .build();

        var response = client.send(monitor, request);
        var models = CopilotUtils.GSON.fromJson(response, CopilotModelList.class);
        return models.data().stream()
            .filter(CopilotModel::isEnabled)
            .toList();
    }

    private record DeviceCodeRequest(
        @SerializedName("client_id") String clientId,
        @SerializedName("scope") String scope
    ) {
    }

    public record DeviceCodeResponse(
        @SerializedName("device_code") String deviceCode,
        @SerializedName("user_code") String userCode,
        @SerializedName("verification_uri") String verificationUri,
        @SerializedName("expires_in") int expiresIn,
        @SerializedName("interval") int interval
    ) {
    }

    private record AccessTokenRequest(
        @SerializedName("client_id") String clientId,
        @SerializedName("device_code") String deviceCode,
        @SerializedName("grant_type") String grantType
    ) {
    }

    private record AccessTokenResponse(
        @SerializedName("error") String error,
        @SerializedName("access_token") String accessToken
    ) {
    }

}
