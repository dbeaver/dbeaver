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
package org.jkiss.dbeaver.model.ai.copilot.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.InflaterInputStream;

public class CopilotAuthService {
    private static final Gson SECURE_GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .serializeNulls()
        .create();
    private static final String DBEAVER_OAUTH_APP = "Iv1.b507a08c87ecfe98";


    /**
     * Request the authentication to the user
     *
     * @param monitor progress monitor
     * @return ResponseDataDTO
     */
    public static ResponseDataDTO requestAuth(DBRProgressMonitor monitor)
            throws IOException, InterruptedException, URISyntaxException, DBException {
        HttpClient client = HttpClient.newBuilder().build();
        RequestAccessContentDTO requestAccessContent = new RequestAccessContentDTO(DBEAVER_OAUTH_APP, "read:user");
        String json = SECURE_GSON.toJson(requestAccessContent);
        HttpRequest post = HttpRequest.newBuilder()
            .uri(new URI("https://github.com/login/device/code"))
            .header("accept", "application/json")
            .header("content-type", "application/json")
            .header("accept-encoding", "deflate")
            .timeout(java.time.Duration.ofSeconds(10)) // Set timeout
            .POST(HttpRequest.BodyPublishers.ofString(json)).build();

        String body = sendRequest(monitor, client, post, true);
        return SECURE_GSON.fromJson(body, ResponseDataDTO.class);
    }

    /**
     * Request the access token
     *
     * @param deviceCode the device code
     * @param monitor progress monitor
     * @return the access token
     */
    public static String requestAccessToken(String deviceCode, DBRProgressMonitor monitor)
                throws URISyntaxException, InterruptedException, IOException, TimeoutException, DBException {
        String accessToken;
        long duration = System.currentTimeMillis();
        long timeoutValue = duration + 100000;
        while (duration < timeoutValue) {
            HttpClient client = HttpClient.newBuilder().build();
            AccessTokenRequestBodyDTO requestAccessToken = new AccessTokenRequestBodyDTO(
                DBEAVER_OAUTH_APP,
                deviceCode,
                "urn:ietf:params:oauth:grant-type:device_code"
            );
            String json = SECURE_GSON.toJson(requestAccessToken);
            HttpRequest post = HttpRequest.newBuilder()
                .uri(new URI("https://github.com/login/oauth/access_token"))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("accept-encoding", "deflate")
                .timeout(java.time.Duration.ofSeconds(10)) // Set timeout
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
            String body = sendRequest(monitor, client, post, true);
            GithubAccessTokenDataDTO githubAccessTokenData = SECURE_GSON.fromJson(body, GithubAccessTokenDataDTO.class);
            if (!CommonUtils.isEmpty(githubAccessTokenData.access_token())) {
                accessToken = githubAccessTokenData.access_token;
                return accessToken;
            }
            TimeUnit.MILLISECONDS.sleep(10000);
            duration = System.currentTimeMillis();
        }
        throw new TimeoutException("OAuth");
    }

    /**
     * Request the copilot session token
     *
     * @param accessToken the access token
     * @param monitor progress monitor
     * @return the copilot session token
     */
    public static String requestCopilotSessionToken(String accessToken, DBRProgressMonitor monitor)
                throws URISyntaxException, IOException, InterruptedException, DBException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest getSessionToken = HttpRequest.newBuilder()
            .uri(new URI("https://api.github.com/copilot_internal/v2/token"))
            .header("authorization", "token " + accessToken)
            .header("editor-version", "Neovim/0.6.1") // TODO replace after partnership
            .header("editor-plugin-version", "copilot.vim/1.16.0") // TODO replace after partnership
            .header("user-agent", "GithubCopilot/1.155.0").GET().build();
        String tokenJson = sendRequest(monitor, client, getSessionToken, false);
        return SECURE_GSON.fromJson(tokenJson, CopilotSessionTokenDTO.class).token();
    }

    @NotNull
    private static String sendRequest(DBRProgressMonitor monitor, HttpClient client, HttpRequest post, boolean decode)
                throws InterruptedException, IOException, DBException {
        CompletableFuture<HttpResponse<String>> responseJsonFuture = client.sendAsync(post, HttpResponse.BodyHandlers.ofString());
        while (!responseJsonFuture.isDone()) {
            if (monitor.isCanceled()) {
                responseJsonFuture.cancel(true);
                throw new InterruptedException();
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
        HttpResponse<String> responseJson = responseJsonFuture.getNow(null);
        String body;
        if (decode) {
            String contentEncoding = responseJson.headers().firstValue("content-encoding").orElse("");
            body = decodeResponseBody(responseJson.body().getBytes(), contentEncoding);
        } else {
            body = responseJson.body();
        }
        if (responseJson.statusCode() != 200) {
            throw new DBException("Error during the request " + body);
        }

        return body;
    }

    private static String decodeResponseBody(byte[] body, String contentEncoding) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(body);
        if ("deflate".equalsIgnoreCase(contentEncoding)) {
            inputStream = new InflaterInputStream(inputStream);
        }
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("checkstyle:RecordComponentName")
    public record ResponseDataDTO(String device_code, String user_code, String verification_uri) {
    }

    @SuppressWarnings("checkstyle:RecordComponentName")
    protected record RequestAccessContentDTO(String client_id, String scope) {

    }

    @SuppressWarnings("checkstyle:RecordComponentName")
    protected record AccessTokenRequestBodyDTO(String client_id, String device_code, String grant_type) {
    }

    @SuppressWarnings("checkstyle:RecordComponentName")
    protected record GithubAccessTokenDataDTO(String access_token) {
    }

    @SuppressWarnings("checkstyle:RecordComponentName")
    protected record CopilotSessionTokenDTO(String token) {

    }
}
