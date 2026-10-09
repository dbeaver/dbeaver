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
package org.jkiss.dbeaver.model.datadam;

import com.dbeaver.rest.client.AbstractRestClient;
import com.dbeaver.rest.client.MediaType;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoSession;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSyncCredentials;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class DDTrackingClient implements DDTrackingService {

    private static final Log log = Log.getLog(DDTrackingClient.class);

    private static final int START_TIMEOUT_MS = 10000;
    private static final int STOP_TIMEOUT_MS = 3000;
    private static final byte[] EMPTY_BODY = new byte[0];

    private final Transport startTransport;
    private final Transport stopTransport;

    public DDTrackingClient(@NotNull String url) {
        this(url, TRACKING_ENDPOINT);
    }

    protected DDTrackingClient(@NotNull String url, @NotNull String rootPath) {
        this.startTransport = new Transport(url, rootPath, START_TIMEOUT_MS);
        this.stopTransport = new Transport(url, rootPath, STOP_TIMEOUT_MS);
    }

    @Nullable
    @Override
    public DDTracking start(@Nullable String authorization, @NotNull DDClientInfo client) {
        return startTransport.post(TRACK_START_ENDPOINT, authorization, client);
    }

    @Nullable
    public DDTracking start(@NotNull DDSyncCredentials credentials, @NotNull DDClientInfo client) {
        return startTransport.post(TRACK_START_ENDPOINT, credentials, client);
    }

    @Nullable
    @Override
    public DDTracking stop(@Nullable String authorization, @NotNull String trackingId) {
        return stopTransport.post(TRACK_STOP_ENDPOINT.replace("{trackingId}", trackingId), authorization);
    }

    @Nullable
    public DDTracking stop(@NotNull DDSyncCredentials credentials, @NotNull String trackingId) {
        return stopTransport.post(TRACK_STOP_ENDPOINT.replace("{trackingId}", trackingId), credentials);
    }

    private static final class Transport extends AbstractRestClient {

        private final String rootPath;

        Transport(@NotNull String url, @NotNull String rootPath, int readTimeoutMs) {
            super(url, DEFAULT_CONNECT_TIMEOUT, readTimeoutMs, List.of(), HttpClient.Redirect.NEVER);
            this.rootPath = rootPath;
        }

        @Nullable
        DDTracking post(@NotNull String path, @Nullable String authorization, @NotNull Object body) {
            try {
                URI uri = buildUri(CommonUtils.removeLeadingSlash(rootPath + path), Map.of());
                HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .header(HttpConstants.HEADER_CONTENT_TYPE, MediaType.JSON.toString())
                    .POST(createBodyPublisher(body, MediaType.JSON));
                applyAuth(builder, authorization);
                return execute(builder, DDTracking.class);
            } catch (DBException e) {
                log.debug("DataDam tracking request failed", e);
                return null;
            }
        }

        @Nullable
        DDTracking post(@NotNull String path, @Nullable String authorization) {
            try {
                URI uri = buildUri(CommonUtils.removeLeadingSlash(rootPath + path), Map.of());
                HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.noBody());
                applyAuth(builder, authorization);
                return execute(builder, DDTracking.class);
            } catch (DBException e) {
                log.debug("DataDam tracking request failed", e);
                return null;
            }
        }

        @Nullable
        DDTracking post(@NotNull String path, @NotNull DDSyncCredentials credentials, @NotNull Object body) {
            byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
            return postWithDesktopSession(path, bytes, true);
        }

        @Nullable
        DDTracking post(@NotNull String path, @NotNull DDSyncCredentials credentials) {
            return postWithDesktopSession(path, EMPTY_BODY, false);
        }

        @Nullable
        private DDTracking postWithDesktopSession(
            @NotNull String path,
            @NotNull byte[] body,
            boolean json
        ) {
            try {
                URI uri = buildUri(CommonUtils.removeLeadingSlash(rootPath + path), Map.of());
                HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body));
                if (json) {
                    builder.header(HttpConstants.HEADER_CONTENT_TYPE, MediaType.JSON.toString());
                }
                builder.header(HttpConstants.HEADER_AUTHORIZATION,
                    HttpConstants.BEARER_PREFIX + DDDesktopSsoSession.accessFor(uri));
                return execute(builder, DDTracking.class);
            } catch (DBException e) {
                log.debug("DataDam tracking request failed", e);
                return null;
            }
        }

        private static void applyAuth(
            @NotNull HttpRequest.Builder builder,
            @Nullable String authorization
        ) {
            if (authorization != null) {
                builder.header(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.BEARER_PREFIX + authorization);
            }
        }

        @Override
        protected void logDebug(@NotNull String message) {
            log.debug(message);
        }

        @Override
        protected void logError(@NotNull String message) {
            log.debug(message);
        }
    }
}
