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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.access.DBAuthUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Generic OpenID Connect provider: signs the user in with an external identity provider
 * (Microsoft Entra ID, Okta, etc.) and uses the resulting access token as the ClickHouse JWT.
 * <p>
 * The target ClickHouse service must be configured to trust the same issuer, audience and JWKS.
 * By default the authorization code flow with PKCE is used, with the device authorization grant
 * as a fallback for environments where a loopback redirect is not possible.
 */
public class ClickhouseOIDCProvider extends ClickhouseJWTProvider {
    public static final int DEFAULT_CALLBACK_PORT = 18923;
    public static final String DEFAULT_SCOPES = "openid profile email offline_access";

    private static final String CALLBACK_PATH = "/auth/callback";
    private static final String DISCOVERY_PATH = "/.well-known/openid-configuration";
    private static final long LOGIN_TIMEOUT_SECONDS = 300;

    private final String scopes;
    private final int callbackPort;
    private final boolean useDeviceCode;
    private final String clientSecret;
    private final String loginHint;

    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String deviceCodeEndpoint;

    public ClickhouseOIDCProvider(@NotNull ClickhouseOIDCSettings settings) throws DBException {
        super(settings.resolveIssuer(), settings.clientId(), settings.audience());
        this.clientSecret = settings.clientSecret();
        this.scopes = CommonUtils.isEmpty(settings.scopes()) ? DEFAULT_SCOPES : settings.scopes();
        this.callbackPort = settings.callbackPort() <= 0 ? DEFAULT_CALLBACK_PORT : settings.callbackPort();
        this.useDeviceCode = settings.useDeviceCode();
        this.loginHint = settings.email();
    }

    @NotNull
    @Override
    protected String getScopes() {
        return scopes;
    }

    @Override
    protected void addClientAuthentication(@NotNull Map<String, String> parameters) {
        if (!CommonUtils.isEmpty(clientSecret)) {
            parameters.put("client_secret", clientSecret);
        }
    }

    @NotNull
    @Override
    protected String getTokenEndpoint() throws DBException {
        discoverEndpoints();
        return tokenEndpoint;
    }

    @NotNull
    @Override
    protected String getDeviceCodeEndpoint() throws DBException {
        discoverEndpoints();
        if (CommonUtils.isEmpty(deviceCodeEndpoint)) {
            throw new DBException("Identity provider does not support the device authorization grant");
        }
        return deviceCodeEndpoint;
    }

    @Override
    protected void interactiveLogin(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (useDeviceCode) {
            deviceCodeLogin(monitor);
        } else {
            authorizationCodeLogin(monitor);
        }
    }

    /**
     * Reads the endpoints from the OpenID Connect discovery document.
     */
    private synchronized void discoverEndpoints() throws DBException {
        if (!CommonUtils.isEmpty(tokenEndpoint)) {
            return;
        }
        String discoveryUrl = oauthUrl.endsWith(DISCOVERY_PATH) ? oauthUrl : oauthUrl + DISCOVERY_PATH;
        JsonObject document = getJson(discoveryUrl);
        authorizationEndpoint = getRequiredString(document, "authorization_endpoint");
        tokenEndpoint = getRequiredString(document, "token_endpoint");
        deviceCodeEndpoint = document.has("device_authorization_endpoint")
            ? document.get("device_authorization_endpoint").getAsString()
            : null;
    }

    /**
     * Authorization code flow with PKCE. Listens on a loopback port for the redirect.
     */
    private void authorizationCodeLogin(@NotNull DBRProgressMonitor monitor) throws DBException {
        discoverEndpoints();

        String verifier = randomUrlSafe(64);
        String state = randomUrlSafe(32);
        String redirectUri = "http://localhost:" + callbackPort + CALLBACK_PATH;

        CompletableFuture<String> authorizationCode = new CompletableFuture<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HttpServer callbackServer;
        try {
            callbackServer = HttpServer.create(new InetSocketAddress("localhost", callbackPort), 0);
            callbackServer.createContext(CALLBACK_PATH, exchange -> handleCallback(exchange, state, authorizationCode));
            callbackServer.setExecutor(executor);
            callbackServer.start();
        } catch (BindException e) {
            executor.shutdownNow();
            throw new DBException(
                "Port " + callbackPort + " is already in use. Change the redirect port in the connection settings.", e);
        } catch (IOException e) {
            executor.shutdownNow();
            throw new DBException("Cannot start the local authentication callback server", e);
        }

        try {
            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("response_type", "code");
            parameters.put("client_id", clientId);
            parameters.put("redirect_uri", redirectUri);
            parameters.put("scope", getScopes());
            parameters.put("code_challenge", codeChallenge(verifier));
            parameters.put("code_challenge_method", "S256");
            parameters.put("state", state);
            String audience = getAudience();
            if (!CommonUtils.isEmpty(audience)) {
                parameters.put("audience", audience);
            }
            if (!CommonUtils.isEmpty(loginHint)) {
                // Pre-fills the account on the provider's sign-in page
                parameters.put("login_hint", loginHint);
            }
            URI authorizationUri = URI.create(authorizationEndpoint
                + (authorizationEndpoint.indexOf('?') < 0 ? "?" : "&") + toForm(parameters));

            monitor.subTask("Waiting for browser authentication");
            getPrompt().openBrowser(authorizationUri);

            String code = awaitCode(monitor, authorizationCode);
            exchangeAuthorizationCode(code, verifier, redirectUri);
        } finally {
            callbackServer.stop(0);
            executor.shutdownNow();
        }
    }

    @NotNull
    private String awaitCode(
        @NotNull DBRProgressMonitor monitor,
        @NotNull CompletableFuture<String> authorizationCode
    ) throws DBException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(LOGIN_TIMEOUT_SECONDS);
        while (true) {
            if (monitor.isCanceled()) {
                throw new DBException("Authentication was cancelled");
            }
            if (System.nanoTime() > deadline) {
                throw new DBException("Authentication timed out");
            }
            try {
                return authorizationCode.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException ignored) {
                // Keep waiting, checking the monitor in between
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DBException("Authentication was interrupted", e);
            } catch (ExecutionException e) {
                // getCause() is null for an ExecutionException raised without one
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new DBException("Authentication failed: " + cause.getMessage(), e);
            }
        }
    }

    private void exchangeAuthorizationCode(
        @NotNull String code,
        @NotNull String verifier,
        @NotNull String redirectUri
    ) throws DBException {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("grant_type", "authorization_code");
        parameters.put("code", code);
        parameters.put("client_id", clientId);
        parameters.put("redirect_uri", redirectUri);
        parameters.put("code_verifier", verifier);
        addClientAuthentication(parameters);
        acceptTokenResponse(sendForm(getTokenEndpoint(), parameters, null));
    }

    private static void handleCallback(
        @NotNull HttpExchange exchange,
        @NotNull String expectedState,
        @NotNull CompletableFuture<String> authorizationCode
    ) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String response;
        try {
            String error = query.get("error");
            if (error != null) {
                throw new DBException("Identity provider returned an error: "
                    + CommonUtils.notEmpty(query.getOrDefault("error_description", error)));
            }
            if (!expectedState.equals(query.get("state"))) {
                throw new DBException("Authentication state mismatch, the request may have been tampered with");
            }
            String code = query.get("code");
            if (CommonUtils.isEmpty(code)) {
                throw new DBException("Identity provider did not return an authorization code");
            }
            authorizationCode.complete(code);
            response = DBAuthUtils.getExternalBrowserSuccessResponse("ClickHouse");
        } catch (DBException e) {
            authorizationCode.completeExceptionally(e);
            response = "<html><body><h3>Authentication failed</h3><p>"
                + CommonUtils.escapeHtml(e.getMessage()) + "</p></body></html>";
        }
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    @NotNull
    private static Map<String, String> parseQuery(@Nullable String rawQuery) {
        Map<String, String> parameters = new HashMap<>();
        if (CommonUtils.isEmpty(rawQuery)) {
            return parameters;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                parameters.put(
                    URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return parameters;
    }

    @NotNull
    private static String randomUrlSafe(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @NotNull
    private static String codeChallenge(@NotNull String verifier) throws DBException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new DBException("SHA-256 is not available", e);
        }
    }
}
