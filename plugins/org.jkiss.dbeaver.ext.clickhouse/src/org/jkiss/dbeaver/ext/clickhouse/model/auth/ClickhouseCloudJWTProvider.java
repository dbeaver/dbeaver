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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ClickHouse Cloud JWT provider.
 * <p>
 * Signs the user in with the ClickHouse Cloud identity provider (device authorization grant) and exchanges
 * the resulting token for a service-scoped ClickHouse JWT. Mirrors {@code src/Client/CloudJWTProvider.cpp}
 * of the ClickHouse client, including the well-known endpoints of the managed services.
 */
public class ClickhouseCloudJWTProvider extends ClickhouseJWTProvider {
    private static final Log log = Log.getLog(ClickhouseCloudJWTProvider.class);

    /** Audience of the identity provider token that may be exchanged for a ClickHouse JWT */
    private static final String TOKEN_EXCHANGE_AUDIENCE = "token-exchange";
    private static final String TOKEN_EXCHANGE_PATH = "/.api/auth/tokenExchange";
    /** ClickHouse JWTs are refreshed this many seconds before they actually expire */
    private static final long JWT_EXPIRATION_BUFFER_SECONDS = 30;

    private static final Map<String, AuthEndpoints> MANAGED_SERVICE_ENDPOINTS = Map.of(
        ".clickhouse.cloud", new AuthEndpoints(
            "https://auth.clickhouse.cloud",
            "9Wf1YpSocOg5sp7GOcCjtrt6DWRAJ19S",
            "https://console-api-internal.clickhouse.cloud"),
        ".clickhouse-staging.com", new AuthEndpoints(
            "https://auth.control-plane.clickhouse-staging.com",
            "rpEkizLMmAU95MP4JL8ERefbVXtUQSFs",
            "https://console-api-internal.clickhouse-staging.com"),
        ".clickhouse-dev.com", new AuthEndpoints(
            "https://auth.control-plane.clickhouse-dev.com",
            "dKv0XkTAw7rghGiAa5sjPFYGQUVtjzuz",
            "https://console-api-internal.clickhouse-dev.com")
    );

    private final String hostName;
    private final String apiHost;

    private String clickhouseJWT;
    private long clickhouseJWTExpiresAt;

    private ClickhouseCloudJWTProvider(@NotNull String hostName, @NotNull AuthEndpoints endpoints) {
        super(endpoints.authUrl(), endpoints.clientId(), TOKEN_EXCHANGE_AUDIENCE);
        this.hostName = hostName;
        this.apiHost = endpoints.apiHost();
    }

    /**
     * Creates a provider for the given host, or returns null if the host is not
     * served by a known ClickHouse Cloud control plane.
     */
    @Nullable
    public static ClickhouseCloudJWTProvider create(@Nullable String hostName) {
        if (hostName == null) {
            return null;
        }
        AuthEndpoints endpoints = getAuthEndpoints(hostName);
        return endpoints == null ? null : new ClickhouseCloudJWTProvider(hostName, endpoints);
    }

    @NotNull
    @Override
    public synchronized String getJWT(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (isTokenValid(clickhouseJWT, clickhouseJWTExpiresAt, JWT_EXPIRATION_BUFFER_SECONDS)) {
            return clickhouseJWT;
        }
        if (!isIdPAccessTokenValid() && hasRefreshToken()) {
            refreshIdPAccessToken();
        }
        if (!isIdPAccessTokenValid()) {
            deviceCodeLogin(monitor);
        }
        try {
            exchangeIdPTokenForClickHouseJWT(monitor);
        } catch (ClickhouseTokenRejectedException e) {
            // The stored identity provider token is no longer accepted - sign in again once
            log.debug("ClickHouse Cloud rejected the identity provider token, signing in again", e);
            reset();
            deviceCodeLogin(monitor);
            exchangeIdPTokenForClickHouseJWT(monitor);
        }
        return clickhouseJWT;
    }

    @Override
    public synchronized void invalidateServiceToken() {
        clickhouseJWT = null;
        clickhouseJWTExpiresAt = 0;
    }

    @Override
    public synchronized void reset() {
        super.reset();
        clickhouseJWT = null;
        clickhouseJWTExpiresAt = 0;
    }

    /**
     * Exchanges the identity provider token for a JWT scoped to the target ClickHouse service.
     */
    private void exchangeIdPTokenForClickHouseJWT(@NotNull DBRProgressMonitor monitor) throws DBException {
        monitor.subTask("Authenticating access to " + hostName);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hostname", hostName);

        HttpRequest request = HttpRequest.newBuilder(URI.create(apiHost + TOKEN_EXCHANGE_PATH))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Authorization", "Bearer " + getIdPAccessToken())
            .POST(HttpRequest.BodyPublishers.ofString(JSONUtils.GSON.toJson(payload)))
            .build();

        HttpResponse<String> response = sendRequest(request);
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new ClickhouseTokenRejectedException(
                "ClickHouse Cloud rejected the sign-in token: HTTP " + response.statusCode());
        }
        if (response.statusCode() != 200) {
            throw new DBException(
                "Error exchanging token for a ClickHouse JWT: HTTP " + response.statusCode() + "\n" + response.body());
        }
        JsonObject exchangeResult = parseJson(response.body());
        clickhouseJWT = getRequiredString(exchangeResult, "token");
        clickhouseJWTExpiresAt = getTokenExpiration(clickhouseJWT);
    }

    @Nullable
    private static AuthEndpoints getAuthEndpoints(@Nullable String hostName) {
        if (hostName == null) {
            return null;
        }
        String host = hostName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, AuthEndpoints> endpoints : MANAGED_SERVICE_ENDPOINTS.entrySet()) {
            if (host.endsWith(endpoints.getKey())) {
                return endpoints.getValue();
            }
        }
        return null;
    }

    private record AuthEndpoints(@NotNull String authUrl, @NotNull String clientId, @NotNull String apiHost) {
    }
}
