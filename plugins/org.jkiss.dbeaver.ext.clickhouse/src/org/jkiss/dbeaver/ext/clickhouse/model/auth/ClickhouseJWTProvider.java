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
package org.jkiss.dbeaver.ext.clickhouse.model.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * OAuth 2.0 device authorization grant provider.
 * <p>
 * Mirrors the flow implemented by {@code clickhouse-client --login} (see {@code src/Client/JWTProvider.cpp}):
 * the identity provider access token is used as the ClickHouse JWT as-is. Subclasses may post-process it,
 * see {@link ClickhouseCloudJWTProvider}.
 */
public class ClickhouseJWTProvider {
    private static final Log log = Log.getLog(ClickhouseJWTProvider.class);

    /*
     * A provider is shared by all connections of a data source, so the token lifecycle is
     * serialized: a second connection opened in parallel waits for the login in progress and
     * then reuses its token instead of starting another one.
     */

    protected static final String SCOPE = "openid profile email offline_access";
    protected static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    /** Tokens expiring within this interval are treated as already expired */
    protected static final long EXPIRATION_BUFFER_SECONDS = 15;

    private static final String DEVICE_CODE_PATH = "/oauth/device/code";
    private static final String TOKEN_PATH = "/oauth/token";
    protected static final String DEVICE_CODE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";
    /** Seconds added to the polling interval when the provider asks us to slow down */
    static final int SLOW_DOWN_INCREMENT_SECONDS = 5;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

    protected final String oauthUrl;
    protected final String clientId;
    private final String oauthAudience;

    private ClickhouseAuthPrompt prompt = ClickhouseAuthPrompt.DEFAULT;

    private String idpAccessToken;
    private String idpRefreshToken;
    private long idpAccessTokenExpiresAt;

    public ClickhouseJWTProvider(@NotNull String oauthUrl, @NotNull String clientId, @Nullable String oauthAudience) {
        this.oauthUrl = CommonUtils.removeTrailingSlash(oauthUrl);
        this.clientId = clientId;
        this.oauthAudience = oauthAudience;
    }

    /**
     * Scopes requested during an interactive login.
     */
    @NotNull
    protected String getScopes() {
        return SCOPE;
    }

    /**
     * Adds client authentication to a token endpoint request.
     * Public clients are identified by the client id alone, confidential ones add a secret.
     */
    protected void addClientAuthentication(@NotNull Map<String, String> parameters) {
        // Nothing to add for a public client
    }

    /**
     * Endpoint issuing device codes. Defaults to the layout used by the ClickHouse Cloud provider.
     */
    @NotNull
    protected String getDeviceCodeEndpoint() throws DBException {
        return oauthUrl + DEVICE_CODE_PATH;
    }

    /**
     * Endpoint issuing and refreshing tokens.
     */
    @NotNull
    protected String getTokenEndpoint() throws DBException {
        return oauthUrl + TOKEN_PATH;
    }

    /**
     * Performs an interactive login. Subclasses may use a different grant.
     */
    protected void interactiveLogin(@NotNull DBRProgressMonitor monitor) throws DBException {
        deviceCodeLogin(monitor);
    }

    /**
     * Returns a valid JWT, performing an interactive login or a silent refresh when needed.
     */
    @NotNull
    public synchronized String getJWT(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (isTokenValid(idpAccessToken, idpAccessTokenExpiresAt)) {
            return idpAccessToken;
        }
        if (!CommonUtils.isEmpty(idpRefreshToken)) {
            refreshIdPAccessToken();
            return idpAccessToken;
        }
        interactiveLogin(monitor);
        return idpAccessToken;
    }

    /**
     * Prompt used to interact with the user during an interactive login.
     */
    @NotNull
    protected ClickhouseAuthPrompt getPrompt() {
        return prompt;
    }

    /**
     * Drops the token derived for a particular service (if the provider derives one), keeping the
     * identity provider session. The next {@link #getJWT} then mints a fresh one.
     */
    public synchronized void invalidateServiceToken() {
        // Nothing to do, the identity provider token is used as-is
    }

    /**
     * Overrides the way the device authorization code is shown to the user.
     */
    public void setPrompt(@NotNull ClickhouseAuthPrompt prompt) {
        this.prompt = prompt;
    }

    /**
     * Drops all cached tokens, so the next {@link #getJWT} performs an interactive login.
     */
    public synchronized void reset() {
        idpAccessToken = null;
        idpRefreshToken = null;
        idpAccessTokenExpiresAt = 0;
    }

    /**
     * Returns the identity provider refresh token, if any. It may be persisted in a secure storage
     * to avoid an interactive login on the next application start.
     */
    @Nullable
    public synchronized String getRefreshToken() {
        return idpRefreshToken;
    }

    /**
     * Restores a previously persisted refresh token.
     */
    public synchronized void setRefreshToken(@Nullable String refreshToken) {
        this.idpRefreshToken = refreshToken;
    }

    @Nullable
    public synchronized String getIdPAccessToken() {
        return idpAccessToken;
    }

    /**
     * Restores a previously persisted identity provider access token.
     * <p>
     * Some providers (ClickHouse Cloud among them) do not issue refresh tokens, so persisting
     * the access token is the only way to avoid an interactive login on every application start.
     * The token is ignored if it is already expired.
     */
    public synchronized void setIdPAccessToken(@Nullable String accessToken) {
        long expiresAt = getTokenExpiration(accessToken);
        if (isTokenValid(accessToken, expiresAt)) {
            this.idpAccessToken = accessToken;
            this.idpAccessTokenExpiresAt = expiresAt;
        }
    }

    protected boolean isIdPAccessTokenValid() {
        return isTokenValid(idpAccessToken, idpAccessTokenExpiresAt);
    }

    protected boolean hasRefreshToken() {
        return !CommonUtils.isEmpty(idpRefreshToken);
    }

    @NotNull
    protected String getAudience() {
        return CommonUtils.notEmpty(oauthAudience);
    }

    /**
     * Requests a device code, shows it to the user and polls the token endpoint until the login is complete.
     */
    protected void deviceCodeLogin(@NotNull DBRProgressMonitor monitor) throws DBException {
        monitor.subTask("Requesting device code");
        Map<String, String> deviceCodeParams = new LinkedHashMap<>();
        deviceCodeParams.put("client_id", clientId);
        deviceCodeParams.put("scope", getScopes());
        String audience = getAudience();
        if (!CommonUtils.isEmpty(audience)) {
            deviceCodeParams.put("audience", audience);
        }
        JsonObject deviceCode = sendForm(getDeviceCodeEndpoint(), deviceCodeParams, null);

        String deviceCodeValue = getRequiredString(deviceCode, "device_code");
        String userCode = getRequiredString(deviceCode, "user_code");
        String verificationUri = deviceCode.has("verification_uri_complete")
            ? deviceCode.get("verification_uri_complete").getAsString()
            : getRequiredString(deviceCode, "verification_uri");
        int interval = deviceCode.has("interval") ? deviceCode.get("interval").getAsInt() : 5;
        long expiresAt = now() + (deviceCode.has("expires_in") ? deviceCode.get("expires_in").getAsLong() : 900);

        CompletableFuture<Void> cancellation = new CompletableFuture<>();
        prompt.showUserCode(URI.create(verificationUri), userCode, cancellation);
        try {
            monitor.subTask("Waiting for browser authentication");
            pollForToken(monitor, deviceCodeValue, interval, expiresAt, cancellation);
        } finally {
            // Closes the popup
            cancellation.complete(null);
        }
    }

    private void pollForToken(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String deviceCode,
        int interval,
        long expiresAt,
        @NotNull CompletableFuture<Void> cancellation
    ) throws DBException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", DEVICE_CODE_GRANT);
        params.put("device_code", deviceCode);
        params.put("client_id", clientId);
        addClientAuthentication(params);

        int pollInterval = interval;
        while (now() < expiresAt) {
            if (monitor.isCanceled() || cancellation.isCancelled()) {
                throw new DBException("Authentication was cancelled");
            }
            sleep(pollInterval);

            JsonObject response = sendFormIgnoringStatus(getTokenEndpoint(), params, null);
            switch (classifyPollResponse(response)) {
                case TOKEN_ISSUED -> {
                    acceptTokenResponse(response);
                    return;
                }
                case AUTHORIZATION_PENDING -> { /* keep polling */ }
                case SLOW_DOWN -> pollInterval += SLOW_DOWN_INCREMENT_SECONDS;
                case FAILED -> throw new DBException("Identity provider login failed: " + getPollError(response));
                // A future outcome must abort the login rather than poll until the device code expires
                default -> throw new DBException("Unexpected response while waiting for the device code login");
            }
        }
        throw new DBException("Device login timed out");
    }

    /**
     * Outcome of a single poll of the token endpoint during the device authorization grant.
     */
    enum DeviceCodePollResult {
        /** The user completed the login and the response carries the tokens */
        TOKEN_ISSUED,
        /** The user has not finished yet, keep polling at the same interval */
        AUTHORIZATION_PENDING,
        /** Polling is too frequent, back off */
        SLOW_DOWN,
        /** The login failed and polling must stop */
        FAILED
    }

    /**
     * Classifies a token endpoint response received while polling for a device code.
     */
    @NotNull
    static DeviceCodePollResult classifyPollResponse(@NotNull JsonObject response) {
        if (response.has("access_token")) {
            return DeviceCodePollResult.TOKEN_ISSUED;
        }
        return switch (getPollErrorCode(response)) {
            case "authorization_pending" -> DeviceCodePollResult.AUTHORIZATION_PENDING;
            case "slow_down" -> DeviceCodePollResult.SLOW_DOWN;
            default -> DeviceCodePollResult.FAILED;
        };
    }

    @NotNull
    private static String getPollErrorCode(@NotNull JsonObject response) {
        return response.has("error") ? response.get("error").getAsString() : "unknown_error";
    }

    /**
     * Human readable reason of a failed poll.
     */
    @NotNull
    static String getPollError(@NotNull JsonObject response) {
        return response.has("error_description")
            ? response.get("error_description").getAsString()
            : getPollErrorCode(response);
    }

    protected void refreshIdPAccessToken() throws DBException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("client_id", clientId);
        params.put("refresh_token", idpRefreshToken);
        addClientAuthentication(params);
        try {
            acceptTokenResponse(sendForm(getTokenEndpoint(), params, null));
        } catch (DBException e) {
            // The refresh token is no longer usable - force interactive login next time
            reset();
            throw new DBException("Cannot refresh access token, please sign in again: " + e.getMessage(), e);
        }
    }

    protected void acceptTokenResponse(@NotNull JsonObject response) throws DBException {
        idpAccessToken = getRequiredString(response, "access_token");
        idpAccessTokenExpiresAt = getTokenExpiration(idpAccessToken);
        if (idpAccessTokenExpiresAt == 0 && response.has("expires_in")) {
            idpAccessTokenExpiresAt = now() + response.get("expires_in").getAsLong();
        }
        if (response.has("refresh_token")) {
            idpRefreshToken = response.get("refresh_token").getAsString();
        }
    }

    protected static boolean isTokenValid(@Nullable String token, long expiresAt) {
        return isTokenValid(token, expiresAt, EXPIRATION_BUFFER_SECONDS);
    }

    protected static boolean isTokenValid(@Nullable String token, long expiresAt, long bufferSeconds) {
        return !CommonUtils.isEmpty(token) && now() < expiresAt - bufferSeconds;
    }

    /**
     * Extracts the {@code exp} claim from a JWT. Returns 0 if the token cannot be parsed.
     */
    protected static long getTokenExpiration(@Nullable String token) {
        if (CommonUtils.isEmpty(token)) {
            return 0;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return 0;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonObject claims = JsonParser.parseString(payload).getAsJsonObject();
            return claims.has("exp") ? claims.get("exp").getAsLong() : 0;
        } catch (Exception e) {
            log.debug("Cannot read JWT expiration time", e);
            return 0;
        }
    }

    protected static long now() {
        return System.currentTimeMillis() / 1000;
    }

    private static void sleep(int seconds) throws DBException {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DBException("Authentication was interrupted", e);
        }
    }

    /**
     * Performs a GET request returning JSON (used for the OpenID Connect discovery document).
     */
    @NotNull
    protected JsonObject getJson(@NotNull String url) throws DBException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .GET()
            .build();
        HttpResponse<String> response = sendRequest(request);
        if (response.statusCode() != 200) {
            throw new DBException("Request to " + url + " failed: HTTP " + response.statusCode());
        }
        return parseJson(response.body());
    }

    @NotNull
    protected JsonObject sendForm(
        @NotNull String url,
        @NotNull Map<String, String> parameters,
        @Nullable String bearerToken
    ) throws DBException {
        HttpResponse<String> response = send(url, parameters, bearerToken);
        if (response.statusCode() != 200) {
            throw new DBException("Request to " + url + " failed: HTTP " + response.statusCode() + "\n" + response.body());
        }
        return parseJson(response.body());
    }

    @NotNull
    private JsonObject sendFormIgnoringStatus(
        @NotNull String url,
        @NotNull Map<String, String> parameters,
        @Nullable String bearerToken
    ) throws DBException {
        return parseJson(send(url, parameters, bearerToken).body());
    }

    @NotNull
    private HttpResponse<String> send(
        @NotNull String url,
        @NotNull Map<String, String> parameters,
        @Nullable String bearerToken
    ) throws DBException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(toForm(parameters)));
        if (bearerToken != null) {
            request.header("Authorization", "Bearer " + bearerToken);
        }
        return sendRequest(request.build());
    }

    @NotNull
    protected HttpResponse<String> sendRequest(@NotNull HttpRequest request) throws DBException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DBException("Request to " + request.uri() + " was interrupted", e);
        } catch (IOException e) {
            throw new DBException("Request to " + request.uri() + " failed: " + e.getMessage(), e);
        }
    }

    @NotNull
    protected static JsonObject parseJson(@NotNull String body) throws DBException {
        try {
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            throw new DBException("Cannot parse server response: " + body, e);
        }
    }

    @NotNull
    protected static String getRequiredString(@NotNull JsonObject object, @NotNull String name) throws DBException {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            throw new DBException("Malformed server response, '" + name + "' is missing");
        }
        return object.get(name).getAsString();
    }

    @NotNull
    protected static String toForm(@NotNull Map<String, String> parameters) {
        StringBuilder form = new StringBuilder();
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            if (!form.isEmpty()) {
                form.append('&');
            }
            form.append(URLEncoder.encode(parameter.getKey(), StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(parameter.getValue(), StandardCharsets.UTF_8));
        }
        return form.toString();
    }
}
