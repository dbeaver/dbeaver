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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.access.DBAuthUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.oauth.OAuthConstants;
import org.jkiss.utils.oauth.OAuthUtils;
import org.jkiss.utils.oauth.code.OAuthCodeResponseHandler;
import org.jkiss.utils.oauth.code.OAuthRequestURLBuilder;

import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

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

        String verifier = OAuthUtils.generateCodeVerifier();
        String state = OAuthUtils.generateRandomUrlSafeValue(32);
        String redirectUri = "http://localhost:" + callbackPort + CALLBACK_PATH;

        OAuthCodeResponseHandler responseHandler = new OAuthCodeResponseHandler(
            callbackPort,
            CALLBACK_PATH,
            state,
            DBAuthUtils.getExternalBrowserSuccessResponse("ClickHouse")
        );
        try {
            responseHandler.initServer();
        } catch (IOException e) {
            responseHandler.close();
            if (e instanceof BindException || e.getCause() instanceof BindException) {
                throw new DBException(
                    "Port " + callbackPort + " is already in use. Change the redirect port in the connection settings.", e);
            }
            throw new DBException("Cannot start the local authentication callback server", e);
        }

        try {
            OAuthRequestURLBuilder urlBuilder = new OAuthRequestURLBuilder(authorizationEndpoint)
                .withClientId(clientId)
                .withRedirectURI(redirectUri)
                .withScope(getScopes())
                .withCodeChallenge(OAuthUtils.generateCodeChallenge(verifier))
                .withState(state);
            String audience = getAudience();
            if (!CommonUtils.isEmpty(audience)) {
                urlBuilder.withParam("audience", audience);
            }
            if (!CommonUtils.isEmpty(loginHint)) {
                // Pre-fills the account on the provider's sign-in page
                urlBuilder.withParam("login_hint", loginHint);
            }
            URI authorizationUri = URI.create(urlBuilder.build());

            CompletableFuture<Void> browserCompletion = new CompletableFuture<>();
            try {
                monitor.subTask("Waiting for browser authentication");
                getPrompt().openBrowser(authorizationUri, browserCompletion);
                String code = awaitCode(monitor, responseHandler.requestCode(), browserCompletion);
                exchangeAuthorizationCode(code, verifier, redirectUri);
            } finally {
                browserCompletion.complete(null);
            }
        } catch (IOException e) {
            throw new DBException("Cannot create the authorization request", e);
        } finally {
            responseHandler.close();
        }
    }

    @NotNull
    private String awaitCode(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Future<String> authorizationCode,
        @NotNull CompletableFuture<Void> cancellation
    ) throws DBException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(LOGIN_TIMEOUT_SECONDS);
        while (true) {
            if (monitor.isCanceled() || cancellation.isCancelled()) {
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
        parameters.put(OAuthConstants.PARAM_GRANT_TYPE, OAuthConstants.GRANT_TYPE_AUTH_CODE);
        parameters.put(OAuthConstants.PARAM_CODE, code);
        parameters.put(OAuthConstants.AUTH_PROP_CLIENT_ID, clientId);
        parameters.put(OAuthConstants.PARAM_REDIRECT_URI, redirectUri);
        parameters.put(OAuthConstants.PARAM_CODE_VERIFIER, verifier);
        addClientAuthentication(parameters);
        acceptTokenResponse(sendForm(getTokenEndpoint(), parameters, null));
    }
}
