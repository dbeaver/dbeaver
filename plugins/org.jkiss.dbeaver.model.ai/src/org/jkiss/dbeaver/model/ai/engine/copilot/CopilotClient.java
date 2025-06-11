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
package org.jkiss.dbeaver.model.ai.engine.copilot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotChatChunk;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotChatRequest;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotChatResponse;
import org.jkiss.dbeaver.model.ai.engine.copilot.dto.CopilotSessionToken;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Flow;
import java.util.concurrent.Future;
import java.util.concurrent.SubmissionPublisher;

public class CopilotClient implements AutoCloseable {
    private static final String DATA_EVENT = "data: ";
    private static final String DONE_EVENT = "[DONE]";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Gson GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .serializeNulls()
        .create();

    private static final String COPILOT_SESSION_TOKEN_URL = "https://api.github.com/copilot_internal/v2/token";
    private static final String CHAT_REQUEST_URL = "https://api.githubcopilot.com/chat/completions";
    private static final String EDITOR_VERSION = "Neovim/0.6.1"; // TODO replace after partnership
    private static final String EDITOR_PLUGIN_VERSION = "copilot.vim/1.16.0"; // TODO replace after partnership
    private static final String USER_AGENT = "GithubCopilot/1.155.0";
    private static final String CHAT_EDITOR_VERSION = "vscode/1.80.1"; // TODO replace after partnership
    private static final String DBEAVER_OAUTH_APP = "Iv1.b507a08c87ecfe98";

    private final MonitoredHttpClient client = new MonitoredHttpClient(HttpClient.newBuilder().build());

    /**
     * Request access to the user's account
     */
    public DeviceCodeResponse requestDeviceCode(@NotNull DBRProgressMonitor monitor) throws DBException {
        DeviceCodeRequest deviceCodeRequest = new DeviceCodeRequest(DBEAVER_OAUTH_APP, "read:user");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve("https://github.com/login/device/code"))
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .timeout(Duration.ofSeconds(10)) // Set timeout
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(deviceCodeRequest)))
            .build();

        HttpResponse<String> response = client.send(monitor, request);
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), DeviceCodeResponse.class);
        } else {
            throw mapHttpError(response);
        }
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
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .timeout(Duration.ofSeconds(5)) // Set timeout
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(accessTokenRequest)))
            .build();

        Duration expiresIn = Duration.ofSeconds(deviceCodeResponse.expiresIn());
        Duration interval = Duration.ofSeconds(deviceCodeResponse.interval());
        Instant start = Instant.now();

        while (Instant.now().isBefore(start.plus(expiresIn)) && !monitor.isCanceled() && !cancellationToken.isCancelled()) {
            var response = client.send(monitor, request);
            if (response.statusCode() != 200) {
                throw mapHttpError(response);
            }
            var body = GSON.fromJson(response.body(), AccessTokenResponse.class);
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
    public CopilotSessionToken requestSessionToken(
        DBRProgressMonitor monitor,
        String accessToken
    ) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(AIHttpUtils.resolve(COPILOT_SESSION_TOKEN_URL))
            .header("authorization", "token " + accessToken)
            .header("editor-version", EDITOR_VERSION)
            .header("editor-plugin-version", EDITOR_PLUGIN_VERSION)
            .header("user-agent", USER_AGENT)
            .GET()
            .timeout(TIMEOUT)
            .build();

        HttpResponse<String> response = client.send(monitor, request);
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), CopilotSessionToken.class);
        } else {
            throw mapHttpError(response);
        }
    }

    /**
     * Chat with Copilot
     */
    public CopilotChatResponse chat(
        DBRProgressMonitor monitor,
        String token,
        CopilotChatRequest chatRequest
    ) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .header("Content-type", "application/json")
            .uri(AIHttpUtils.resolve(CHAT_REQUEST_URL))
            .header("authorization", "Bearer " + token)
            .header("Editor-Version", CHAT_EDITOR_VERSION)
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(chatRequest)))
            .timeout(TIMEOUT)
            .build();

        HttpResponse<String> response = client.send(monitor, request);
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), CopilotChatResponse.class);
        } else {
            throw mapHttpError(response);
        }
    }

    public Flow.Publisher<CopilotChatChunk> createChatCompletionStream(
        @NotNull DBRProgressMonitor monitor,
        String token,
        @NotNull CopilotChatRequest chatRequest
    ) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .header("Content-type", "application/json")
            .uri(AIHttpUtils.resolve(CHAT_REQUEST_URL))
            .header("authorization", "Bearer " + token)
            .header("Editor-Version", CHAT_EDITOR_VERSION)
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(chatRequest)))
            .timeout(TIMEOUT)
            .build();

        SubmissionPublisher<CopilotChatChunk> publisher = new SubmissionPublisher<>();

        client.sendAsync(
            request,
            line -> {
                if (line.startsWith(DATA_EVENT)) {

                    String data = line.substring(6).trim();
                    if (DONE_EVENT.equals(data)) {
                        publisher.close();
                    } else {
                        try {
                            CopilotChatChunk chunk = GSON.fromJson(data, CopilotChatChunk.class);
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

    private static DBException mapHttpError(HttpResponse<String> response) {
        return new DBException("HTTP error: " + response.statusCode() + " " + response.body());
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
