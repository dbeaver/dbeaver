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
package org.jkiss.dbeaver.model.datadam.sync.tracking;

import com.dbeaver.datadam.share.api.tracking.DDClientInfo;
import com.dbeaver.datadam.share.api.tracking.DDTracking;
import com.dbeaver.datadam.share.api.tracking.DDTrackingEndpoints;
import com.dbeaver.datadam.share.api.tracking.DDTrackingService;
import com.dbeaver.rest.client.AbstractRestClient;
import com.dbeaver.rest.client.MediaType;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
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
        this(url, DDTrackingEndpoints.TRACKING);
    }

    protected DDTrackingClient(@NotNull String url, @NotNull String rootPath) {
        this.startTransport = new Transport(url, rootPath, START_TIMEOUT_MS);
        this.stopTransport = new Transport(url, rootPath, STOP_TIMEOUT_MS);
    }

    @NotNull
    @Override
    public DDTracking start(@NotNull DDClientInfo client) throws DBException {
        return startTransport.postWithDesktopSession(DDTrackingEndpoints.START, client);
    }

    @NotNull
    @Override
    public DDTracking stop(@NotNull String trackingId) throws DBException {
        return stopTransport.postWithDesktopSession(
            DDTrackingEndpoints.STOP.replace("{trackingId}", trackingId), EMPTY_BODY, false);
    }

    private static final class Transport extends AbstractRestClient {

        private final String rootPath;
        private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();

        Transport(@NotNull String url, @NotNull String rootPath, int readTimeoutMs) {
            super(url, DEFAULT_CONNECT_TIMEOUT, readTimeoutMs, List.of(), HttpClient.Redirect.NEVER);
            this.rootPath = rootPath;
        }

        @NotNull
        DDTracking postWithDesktopSession(@NotNull String path, @NotNull Object body) throws DBException {
            byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
            return postWithDesktopSession(path, bytes, true);
        }

        @NotNull
        private DDTracking postWithDesktopSession(
            @NotNull String path,
            @NotNull byte[] body,
            boolean json
        ) throws DBException {
            URI uri = buildUri(CommonUtils.removeLeadingSlash(rootPath + path), Map.of());
            DDEndpoints.requireSecureTransport(uri);
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
            if (json) {
                builder.header(HttpConstants.HEADER_CONTENT_TYPE, MediaType.JSON.toString());
            }
            builder.header(
                HttpConstants.HEADER_AUTHORIZATION,
                HttpConstants.BEARER_PREFIX + accessService.getAccessTokenForResource(uri)
            );
            DDTracking tracking = execute(builder, DDTracking.class);
            if (tracking == null) {
                throw new DBException("Empty DataDam tracking response");
            }
            return tracking;
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
