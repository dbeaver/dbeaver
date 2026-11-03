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
package org.jkiss.dbeaver.model.datadam.auth.desktop;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Locale;

/** Checks expiry and prevents the credential from being sent outside its allowed service endpoints. */
public class DDDesktopTokenValidator {
    private static final int EXPIRY_MARGIN_SECONDS = 30;

    public boolean isExpired(@NotNull DDDesktopUserToken token) {
        return !expiresAt(token).isAfter(Instant.now());
    }

    @NotNull
    Instant expiresAt(@NotNull DDDesktopUserToken token) {
        try {
            var claims = JSONUtils.parseJwtClaims(token.accessToken());
            if (claims == null) {
                return Instant.EPOCH;
            }
            long seconds = claims.get("exp").getAsLong();
            return Instant.ofEpochSecond(seconds).minusSeconds(EXPIRY_MARGIN_SECONDS);
        } catch (RuntimeException e) {
            return Instant.EPOCH;
        }
    }

    public boolean matchesResource(@NotNull DDDesktopUserToken token, @NotNull URI resource) {
        try {
            if ("ws".equalsIgnoreCase(resource.getScheme()) || "wss".equalsIgnoreCase(resource.getScheme())) {
                DDEndpoints.requireSecureWebSocketTransport(resource);
                URI remoteDam = URI.create(DDEndpoints.getRemoteDamWebSocketUrl());
                DDEndpoints.requireSecureWebSocketTransport(remoteDam);
                return originOf(resource).equals(originOf(remoteDam))
                    && matchesEndpoint(token, URI.create(DDEndpoints.getAccountBaseUrl()))
                    && matchesEndpoint(token, URI.create(DDEndpoints.getStorageBaseUrl()));
            }
            return matchesEndpoint(token, resource);
        } catch (DBException | RuntimeException e) {
            return false;
        }
    }

    private boolean matchesEndpoint(@NotNull DDDesktopUserToken token, @NotNull URI resource) {
        URI requestedOrigin = originOf(resource);
        String path = normalizedPath(resource);
        return token.endpoints().stream().map(URI::create).anyMatch(endpoint -> {
            if (!requestedOrigin.equals(originOf(endpoint))) {
                return false;
            }
            String basePath = normalizedPath(endpoint);
            return basePath == null || basePath.isEmpty() || "/".equals(basePath)
                || path.equals(basePath) || path.startsWith(basePath.endsWith("/") ? basePath : basePath + "/");
        });
    }

    @NotNull
    private String normalizedPath(@NotNull URI uri) {
        String path = uri.getPath();
        if (path == null || path.contains("\\")) {
            throw new IllegalArgumentException("Invalid DataDam resource path");
        }
        try {
            // Decode before normalizing so encoded dot segments cannot escape an allowed endpoint.
            return new URI(null, null, path, null, null).normalize().getPath();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid DataDam resource path", e);
        }
    }

    @NotNull
    private URI originOf(@NotNull URI url) {
        if (url.getScheme() == null || url.getHost() == null || url.getUserInfo() != null) {
            throw new IllegalArgumentException("Invalid DataDam URL");
        }
        String scheme = switch (url.getScheme().toLowerCase(Locale.ROOT)) {
            case "https", "wss" -> "https";
            case "http", "ws" -> "http";
            default -> throw new IllegalArgumentException("Invalid DataDam URL scheme");
        };
        try {
            int port = url.getPort();
            if (port == 443 && "https".equals(scheme) || port == 80 && "http".equals(scheme)) {
                port = -1;
            }
            return new URI(scheme, null, url.getHost(), port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid DataDam URL", e);
        }
    }
}
