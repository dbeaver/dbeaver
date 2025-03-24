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
package org.jkiss.dbeaver.model.ai.copilot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.copilot.dto.CopilotChatRequest;
import org.jkiss.dbeaver.model.ai.copilot.dto.CopilotChatResponse;
import org.jkiss.dbeaver.model.ai.copilot.dto.CopilotSessionToken;
import org.jkiss.dbeaver.model.ai.utils.HttpUtils;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CopilotClient implements AutoCloseable {
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
    public ResponseData requestAuth(
        DBRProgressMonitor monitor
    ) throws DBException {
        RequestAccessContent requestAccessContent = new RequestAccessContent(DBEAVER_OAUTH_APP, "read:user");
        HttpRequest post = HttpRequest.newBuilder()
            .uri(HttpUtils.resolve("https://github.com/login/device/code"))
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .header("accept-encoding", "deflate")
            .timeout(java.time.Duration.ofSeconds(10)) // Set timeout
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestAccessContent)))
            .build();

        HttpResponse<String> response = client.send(monitor, post, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), ResponseData.class);
        } else {
            throw mapHttpError(response);
        }
    }

    /**
     * Request access token
     */
    public String requestAccessToken(
        String deviceCode,
        DBRProgressMonitor monitor
    ) throws InterruptedException, TimeoutException, DBException {
        String accessToken;
        long duration = System.currentTimeMillis();
        long timeoutValue = duration + 100000;
        while (duration < timeoutValue) {
            AccessTokenRequestBody requestAccessToken = new AccessTokenRequestBody(
                DBEAVER_OAUTH_APP,
                deviceCode,
                "urn:ietf:params:oauth:grant-type:device_code"
            );
            HttpRequest post = HttpRequest.newBuilder()
                .uri(HttpUtils.resolve("https://github.com/login/oauth/access_token"))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("accept-encoding", "deflate")
                .timeout(java.time.Duration.ofSeconds(10)) // Set timeout
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestAccessToken)))
                .build();

            HttpResponse<String> response = client.send(monitor, post, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                GithubAccessTokenData githubAccessTokenData = GSON.fromJson(response.body(), GithubAccessTokenData.class);
                if (!CommonUtils.isEmpty(githubAccessTokenData.access_token())) {
                    accessToken = githubAccessTokenData.access_token;
                    return accessToken;
                }
            }

            TimeUnit.MILLISECONDS.sleep(10000);
            duration = System.currentTimeMillis();
        }
        throw new TimeoutException("OAuth");
    }

    /**
     * Request session token
     */
    public CopilotSessionToken sessionToken(
        DBRProgressMonitor monitor,
        String accessToken
    ) throws DBException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(HttpUtils.resolve(COPILOT_SESSION_TOKEN_URL))
            .header("authorization", "token " + accessToken)
            .header("editor-version", EDITOR_VERSION)
            .header("editor-plugin-version", EDITOR_PLUGIN_VERSION)
            .header("user-agent", USER_AGENT)
            .GET()
            .timeout(TIMEOUT)
            .build();

        HttpResponse<String> response = client.send(monitor, request, HttpResponse.BodyHandlers.ofString());
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
            .uri(HttpUtils.resolve(CHAT_REQUEST_URL))
            .header("authorization", "Bearer " + token)
            .header("Editor-Version", CHAT_EDITOR_VERSION)
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(chatRequest)))
            .timeout(TIMEOUT)
            .build();

        HttpResponse<String> response = client.send(monitor, request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return GSON.fromJson(response.body(), CopilotChatResponse.class);
        } else {
            throw mapHttpError(response);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    @SuppressWarnings("checkstyle:RecordComponentName")
    public record ResponseData(String device_code, String user_code, String verification_uri) {
    }

    @SuppressWarnings("checkstyle:RecordComponentName")
    protected record RequestAccessContent(String client_id, String scope) {

    }

    @SuppressWarnings("checkstyle:RecordComponentName")
    protected record AccessTokenRequestBody(String client_id, String device_code, String grant_type) {
    }

    @SuppressWarnings("checkstyle:RecordComponentName")
    protected record GithubAccessTokenData(String access_token) {
    }

    private static DBException mapHttpError(HttpResponse<String> response) {
        return new DBException("HTTP error: " + response.statusCode() + " " + response.body());
    }
}
