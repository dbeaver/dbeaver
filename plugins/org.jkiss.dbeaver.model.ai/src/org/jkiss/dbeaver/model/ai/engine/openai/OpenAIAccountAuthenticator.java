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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.HttpConstants;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OpenAIAccountAuthenticator implements AIAccountAuthenticator {
    private static final String ISSUER = "https://auth.openai.com";
    // Public OAuth client ID registered by OpenAI for the Codex browser authorization flow.
    private static final String CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann";
    public static final String CODEX_ENDPOINT = "https://chatgpt.com/backend-api/codex/";
    private static final String CODEX_MODELS_ENDPOINT = "https://chatgpt.com/backend-api/codex/models";
    private static final String CALLBACK_PATH = "/auth/callback";
    private static final int CALLBACK_PORT = 1455;
    private static final int POLLING_SAFETY_MARGIN_SECONDS = 3;
    private static final Duration AUTHORIZATION_TIMEOUT = Duration.ofMinutes(15);

    private final Duration requestTimeout;
    private final HttpClient httpClient;
    private static PendingAuthorization pendingAuthorization;

    public OpenAIAccountAuthenticator(int timeoutSeconds) {
        requestTimeout = Duration.ofSeconds(timeoutSeconds);
        httpClient = HttpClient.newBuilder().connectTimeout(requestTimeout).build();
    }

    public static boolean isSupported() {
        return AIAccountAuthenticator.isSupported();
    }

    @Override
    public boolean supportsBrowserAuthorization() {
        return true;
    }

    @NotNull
    public AIAccountAuthenticator.BrowserAuthorization startBrowserAuthorization() throws DBException {
        if (!isSupported()) {
            throw new DBException("ChatGPT account authentication is available only in standalone desktop applications");
        }
        synchronized (OpenAIAccountAuthenticator.class) {
            if (pendingAuthorization != null) {
                pendingAuthorization.authorizationCode().completeExceptionally(
                    new DBException("OpenAI authorization was replaced by a new sign-in attempt")
                );
                stopAuthorization(pendingAuthorization);
                pendingAuthorization = null;
            }

            String verifier = randomUrlSafe(43);
            String state = randomUrlSafe(32);
            String redirectUri = "http://localhost:" + CALLBACK_PORT + CALLBACK_PATH;
            CompletableFuture<String> authorizationCode = new CompletableFuture<>();
            ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
            try {
                HttpServer callbackServer = HttpServer.create(new InetSocketAddress("localhost", CALLBACK_PORT), 0);
                callbackServer.createContext(
                    CALLBACK_PATH,
                    exchange -> handleCallback(exchange, state, authorizationCode)
                );
                callbackServer.setExecutor(callbackExecutor);
                callbackServer.start();
                pendingAuthorization = new PendingAuthorization(
                    callbackServer,
                    callbackExecutor,
                    authorizationCode,
                    verifier,
                    redirectUri
                );
            } catch (IOException e) {
                callbackExecutor.shutdownNow();
                if (e instanceof BindException) {
                    throw new DBException("OpenAI callback port 1455 is in use by another application", e);
                }
                throw new DBException("Unable to start local OpenAI authorization callback server", e);
            }

            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("response_type", "code");
            parameters.put("client_id", CLIENT_ID);
            parameters.put("redirect_uri", redirectUri);
            parameters.put("scope", "openid profile email offline_access");
            parameters.put("code_challenge", codeChallenge(verifier));
            parameters.put("code_challenge_method", "S256");
            parameters.put("id_token_add_organizations", "true");
            parameters.put("codex_cli_simplified_flow", "true");
            parameters.put("state", state);
            parameters.put("originator", "dbeaver");
            return new AIAccountAuthenticator.BrowserAuthorization(URI.create(ISSUER + "/oauth/authorize?" + toForm(parameters)));
        }
    }

    @NotNull
    public AIAccountAuthenticator.DeviceAuthorization startDeviceAuthorization() throws DBException {
        if (!isSupported()) {
            throw new DBException("ChatGPT account authentication is available only in standalone desktop applications");
        }
        JsonObject request = new JsonObject();
        request.addProperty("client_id", CLIENT_ID);
        JsonObject response = sendJson(ISSUER + "/api/accounts/deviceauth/usercode", request);
        return new AIAccountAuthenticator.DeviceAuthorization(
            getRequiredString(response, "device_auth_id"),
            getRequiredString(response, "user_code"),
            URI.create(ISSUER + "/codex/device"),
            Math.max(1, response.has("interval") ? response.get("interval").getAsInt() : 5),
            AUTHORIZATION_TIMEOUT.toSeconds()
        );
    }

    @NotNull
    public AIAccountAuthenticator.Tokens completeDeviceAuthorization(
        @NotNull AIAccountAuthenticator.DeviceAuthorization authorization,
        @NotNull CompletableFuture<Void> cancellation
    ) throws DBException {
        long deadline = System.nanoTime() + AUTHORIZATION_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (cancellation.isCancelled()) {
                throw new DBException("OpenAI device authorization was cancelled");
            }
            JsonObject response = pollDeviceAuthorization(authorization);
            if (response != null) {
                if (cancellation.isCancelled()) {
                    throw new DBException("OpenAI device authorization was cancelled");
                }
                Tokens tokens = exchangeCode(
                    getRequiredString(response, "authorization_code"),
                    getRequiredString(response, "code_verifier"),
                    ISSUER + "/deviceauth/callback"
                );
                if (cancellation.isCancelled()) {
                    throw new DBException("OpenAI device authorization was cancelled");
                }
                return tokens;
            }
            sleep(authorization.intervalSeconds() + POLLING_SAFETY_MARGIN_SECONDS);
        }
        throw new DBException("OpenAI device authorization timed out");
    }

    @NotNull
    public synchronized AIAccountAuthenticator.Tokens completeBrowserAuthorization() throws DBException {
        if (pendingAuthorization == null) {
            throw new DBException("OpenAI authorization has not been started");
        }
        PendingAuthorization authorization = pendingAuthorization;
        try {
            String code = authorization.authorizationCode().get(AUTHORIZATION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return exchangeCode(code, authorization.verifier(), authorization.redirectUri());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DBException("OpenAI authorization was interrupted", e);
        } catch (TimeoutException e) {
            throw new DBException("OpenAI authorization timed out", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof DBException dbException) {
                throw dbException;
            }
            throw new DBException("OpenAI authorization failed", cause);
        } finally {
            stopAuthorization(authorization);
            if (pendingAuthorization == authorization) {
                pendingAuthorization = null;
            }
        }
    }

    public void cancelBrowserAuthorization() {
        synchronized (OpenAIAccountAuthenticator.class) {
            if (pendingAuthorization == null) {
                return;
            }
            pendingAuthorization.authorizationCode().completeExceptionally(
                new DBException("OpenAI authorization was cancelled")
            );
            stopAuthorization(pendingAuthorization);
            pendingAuthorization = null;
        }
    }

    @NotNull
    public AIAccountAuthenticator.Tokens refresh(@NotNull String refreshToken) throws DBException {
        return sendTokenRequest(Map.of(
            "grant_type", "refresh_token",
            "refresh_token", refreshToken,
            "client_id", CLIENT_ID
        ));
    }

    @NotNull
    public List<String> listModels(@NotNull OpenAIProperties properties) throws DBException {
        var productVersion = GeneralUtils.getProductVersion();
        String clientVersion = productVersion.getMajor() + "." + productVersion.getMinor() + "." + productVersion.getMicro();
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(CODEX_MODELS_ENDPOINT + "?client_version=" + clientVersion))
            .timeout(requestTimeout)
            .header(HttpConstants.HEADER_AUTHORIZATION, "Bearer " + properties.getValidAccessToken(this))
            .header(HttpConstants.HEADER_USER_AGENT, GeneralUtils.getProductTitle())
            .header("originator", "dbeaver")
            .GET();
        String accountId = properties.getAccountId();
        if (accountId != null) {
            request.header("ChatGPT-Account-Id", accountId);
        }

        JsonObject response = send(request.build());
        return parseModels(response);
    }

    @NotNull
    static List<String> parseModels(@NotNull JsonObject response) {
        List<CatalogModel> models = new ArrayList<>();
        JsonArray catalog = response.has("models") ? response.getAsJsonArray("models") : new JsonArray();
        for (JsonElement element : catalog) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject model = element.getAsJsonObject();
            if (!model.has("slug") || !model.has("visibility") || !"list".equals(model.get("visibility").getAsString())) {
                continue;
            }
            models.add(new CatalogModel(
                model.get("slug").getAsString(),
                model.has("priority") ? model.get("priority").getAsInt() : Integer.MAX_VALUE
            ));
        }
        return models.stream()
            .sorted(Comparator.comparingInt(CatalogModel::priority))
            .map(CatalogModel::slug)
            .toList();
    }

    @NotNull
    private AIAccountAuthenticator.Tokens exchangeCode(
        @NotNull String code,
        @NotNull String verifier,
        @NotNull String redirectUri
    ) throws DBException {
        return sendTokenRequest(Map.of(
            "grant_type", "authorization_code",
            "code", code,
            "redirect_uri", redirectUri,
            "client_id", CLIENT_ID,
            "code_verifier", verifier
        ));
    }

    @NotNull
    private AIAccountAuthenticator.Tokens sendTokenRequest(@NotNull Map<String, String> parameters) throws DBException {
        JsonObject response = sendForm(ISSUER + "/oauth/token", parameters);
        String idToken = response.has("id_token") ? response.get("id_token").getAsString() : null;
        String accessToken = getRequiredString(response, "access_token");
        return new AIAccountAuthenticator.Tokens(
            accessToken,
            getRequiredString(response, "refresh_token"),
            response.has("expires_in") ? response.get("expires_in").getAsLong() : 3600,
            extractAccountId(idToken, accessToken),
            extractEmail(idToken, accessToken)
        );
    }

    @NotNull
    private JsonObject sendForm(@NotNull String url, @NotNull Map<String, String> parameters) throws DBException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(requestTimeout)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(toForm(parameters)))
            .build();
        return send(request);
    }

    @NotNull
    private JsonObject sendJson(@NotNull String url, @NotNull JsonObject body) throws DBException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(requestTimeout)
            .header("Content-Type", "application/json")
            .header(HttpConstants.HEADER_USER_AGENT, GeneralUtils.getProductTitle())
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        return send(request);
    }

    @Nullable
    private JsonObject pollDeviceAuthorization(@NotNull AIAccountAuthenticator.DeviceAuthorization authorization) throws DBException {
        JsonObject body = new JsonObject();
        body.addProperty("device_auth_id", authorization.deviceCode());
        body.addProperty("user_code", authorization.userCode());
        HttpRequest request = HttpRequest.newBuilder(URI.create(ISSUER + "/api/accounts/deviceauth/token"))
            .timeout(requestTimeout)
            .header("Content-Type", "application/json")
            .header(HttpConstants.HEADER_USER_AGENT, GeneralUtils.getProductTitle())
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 403 || response.statusCode() == 404) {
                return null;
            }
            return parseResponse(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DBException("OpenAI device authorization was interrupted", e);
        } catch (IOException e) {
            throw new DBException("OpenAI device authorization request failed", e);
        }
    }

    @NotNull
    private JsonObject send(@NotNull HttpRequest request) throws DBException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return parseResponse(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DBException("OpenAI authorization was interrupted", e);
        } catch (Exception e) {
            if (e instanceof DBException dbException) {
                throw dbException;
            }
            throw new DBException("OpenAI authorization request failed", e);
        }
    }

    @NotNull
    private static JsonObject parseResponse(@NotNull HttpResponse<String> response) throws DBException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String responseBody = response.body();
            if (responseBody.length() > 1_000) {
                responseBody = responseBody.substring(0, 1_000) + "...";
            }
            throw new DBException("OpenAI request failed: " + response.statusCode() + ": " + responseBody);
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static void sleep(int seconds) throws DBException {
        try {
            Thread.sleep(Duration.ofSeconds(seconds));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DBException("OpenAI device authorization was interrupted", e);
        }
    }

    private static void handleCallback(
        @NotNull HttpExchange exchange,
        @NotNull String expectedState,
        @NotNull CompletableFuture<String> authorizationCode
    ) throws IOException {
        Map<String, String> parameters = parseQuery(exchange.getRequestURI().getRawQuery());
        String error = parameters.get("error");
        if (!expectedState.equals(parameters.get("state"))) {
            writeCallbackResponse(exchange, 400, "OpenAI authorization failed: invalid state.");
        } else if (error != null) {
            String description = parameters.get("error_description");
            authorizationCode.completeExceptionally(new DBException(description == null ? error : description));
            writeCallbackResponse(exchange, 200, "OpenAI authorization was cancelled. You can close this tab.");
        } else if (parameters.get("code") == null) {
            authorizationCode.completeExceptionally(new DBException("OpenAI callback did not contain authorization code"));
            writeCallbackResponse(exchange, 400, "OpenAI authorization failed: authorization code is missing.");
        } else {
            authorizationCode.complete(parameters.get("code"));
            writeCallbackResponse(exchange, 200, "OpenAI authorization succeeded. You can close this tab.");
        }
    }

    private static void writeCallbackResponse(@NotNull HttpExchange exchange, int status, @NotNull String message) throws IOException {
        byte[] body = ("<html><body>" + message + "</body></html>").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(body);
        }
    }

    private static void stopAuthorization(@NotNull PendingAuthorization authorization) {
        authorization.server().stop(0);
        authorization.executor().shutdownNow();
    }

    @NotNull
    private static String getRequiredString(@NotNull JsonObject object, @NotNull String name) throws DBException {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            throw new DBException("OpenAI authorization response does not contain " + name);
        }
        return object.get(name).getAsString();
    }

    @NotNull
    private static String toForm(@NotNull Map<String, String> parameters) {
        return parameters.entrySet().stream()
            .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .reduce((left, right) -> left + "&" + right)
            .orElse("");
    }

    @NotNull
    private static Map<String, String> parseQuery(@Nullable String query) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) {
            return parameters;
        }
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            if (separator > 0) {
                parameters.put(
                    URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8)
                );
            }
        }
        return parameters;
    }

    @NotNull
    private static String randomUrlSafe(int bytesLength) {
        byte[] bytes = new byte[bytesLength];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @NotNull
    private static String codeChallenge(@NotNull String verifier) throws DBException {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new DBException("SHA-256 is not available", e);
        }
    }

    @Nullable
    static String extractAccountId(@Nullable String idToken, @Nullable String accessToken) {
        String accountId = extractAccountId(idToken);
        return accountId != null ? accountId : extractAccountId(accessToken);
    }

    @Nullable
    static String extractAccountId(@Nullable String token) {
        if (token == null) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            JsonObject claims = JsonParser.parseString(new String(
                Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8
            )).getAsJsonObject();
            if (claims.has("chatgpt_account_id")) {
                return claims.get("chatgpt_account_id").getAsString();
            }
            if (claims.has("https://api.openai.com/auth")) {
                JsonObject auth = claims.getAsJsonObject("https://api.openai.com/auth");
                if (auth.has("chatgpt_account_id")) {
                    return auth.get("chatgpt_account_id").getAsString();
                }
            }
            if (claims.has("organizations")) {
                JsonArray organizations = claims.getAsJsonArray("organizations");
                if (organizations.size() > 0 && organizations.get(0).isJsonObject()) {
                    JsonObject organization = organizations.get(0).getAsJsonObject();
                    if (organization.has("id")) {
                        return organization.get("id").getAsString();
                    }
                }
            }
        } catch (Exception ignored) {
            // The access token is still usable if the optional account claim is unavailable.
        }
        return null;
    }

    @Nullable
    public static String extractEmail(@Nullable String idToken, @Nullable String accessToken) {
        String email = extractEmail(idToken);
        return email != null ? email : extractEmail(accessToken);
    }

    @Nullable
    public static String extractEmail(@Nullable String token) {
        if (token == null) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            JsonObject claims = JsonParser.parseString(new String(
                Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8
            )).getAsJsonObject();
            return claims.has("email") ? claims.get("email").getAsString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static long extractExpiresAt(@Nullable String token) {
        if (token == null) {
            return 0;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return 0;
            }
            JsonObject claims = JsonParser.parseString(new String(
                Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8
            )).getAsJsonObject();
            return claims.has("exp") ? claims.get("exp").getAsLong() * 1000 : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private record PendingAuthorization(
        @NotNull HttpServer server,
        @NotNull ExecutorService executor,
        @NotNull CompletableFuture<String> authorizationCode,
        @NotNull String verifier,
        @NotNull String redirectUri
    ) {
    }

    private record CatalogModel(@NotNull String slug, int priority) {
    }

}
