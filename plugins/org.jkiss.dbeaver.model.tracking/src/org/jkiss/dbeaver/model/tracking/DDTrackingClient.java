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
package org.jkiss.dbeaver.model.tracking;

import com.dbeaver.rest.client.AbstractRestClient;
import com.dbeaver.rest.client.MediaType;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

public class DDTrackingClient implements DDTrackingService {

    private static final Log log = Log.getLog(DDTrackingClient.class);

    private static final int START_TIMEOUT_MS = 10000;
    private static final int STOP_TIMEOUT_MS = 3000;

    private final Transport startTransport;
    private final Transport stopTransport;

    public DDTrackingClient(@NotNull String url) {
        this.startTransport = new Transport(url, START_TIMEOUT_MS);
        this.stopTransport = new Transport(url, STOP_TIMEOUT_MS);
    }

    @Nullable
    @Override
    public DDTracking start(@Nullable String apiKey, @NotNull DDClientInfo client) {
        return startTransport.post(TRACK_START_ENDPOINT, apiKey, client);
    }

    @Nullable
    @Override
    public DDTracking stop(@Nullable String apiKey, @NotNull DDTrackStop request) {
        return stopTransport.post(TRACK_STOP_ENDPOINT, apiKey, request);
    }

    private static final class Transport extends AbstractRestClient {

        Transport(@NotNull String url, int readTimeoutMs) {
            super(url, DEFAULT_CONNECT_TIMEOUT, readTimeoutMs, List.of());
        }

        @Nullable
        DDTracking post(@NotNull String path, @Nullable String apiKey, @NotNull Object body) {
            try {
                String endpoint = CommonUtils.removeLeadingSlash(METERING_ENDPOINT + path);
                URI uri = buildUri(endpoint, Map.of());

                HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .header(HttpConstants.HEADER_CONTENT_TYPE, MediaType.JSON.toString())
                    .POST(createBodyPublisher(body, MediaType.JSON));
                if (apiKey != null) {
                    builder.header(API_KEY_HEADER, apiKey);
                }

                return execute(builder, DDTracking.class);
            } catch (DBException e) {
                log.debug("DataDam tracking request failed", e);
                return null;
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
