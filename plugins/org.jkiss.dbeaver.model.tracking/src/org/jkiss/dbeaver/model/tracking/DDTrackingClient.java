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

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.utils.HttpConstants;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DDTrackingClient implements DDTrackingService {

    private static final Log log = Log.getLog(DDTrackingClient.class);

    private static final int START_TIMEOUT_MS = 10000;
    private static final int STOP_TIMEOUT_MS = 3000;

    private final Gson gson = new Gson();
    private final String url;

    public DDTrackingClient(@NotNull String url) {
        this.url = url;
    }

    @Nullable
    @Override
    public DDTracking start(@Nullable String apiKey, @NotNull DDClientInfo client) {
        return execute(TRACK_START_ENDPOINT, apiKey, gson.toJson(client), START_TIMEOUT_MS);
    }

    @Nullable
    @Override
    public DDTracking stop(@Nullable String apiKey, @NotNull DDTrackStop request) {
        return execute(TRACK_STOP_ENDPOINT, apiKey, gson.toJson(request), STOP_TIMEOUT_MS);
    }

    @Nullable
    private DDTracking execute(@NotNull String path, @Nullable String apiKey, @NotNull String body, int timeoutMs) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
            if (apiKey != null) {
                headers.put(API_KEY_HEADER, apiKey);
            }

            URLConnection connection = WebUtils
                .openURLConnection(url + METERING_ENDPOINT + path, null, null, "POST", 0, timeoutMs, headers);

            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (connection instanceof HttpURLConnection httpConnection && httpConnection.getResponseCode() >= 300) {
                String error;
                try (InputStream err = httpConnection.getErrorStream()) {
                    error = err == null ? "" : new String(err.readAllBytes(), StandardCharsets.UTF_8);
                }
                log.debug("DataDam tracking request failed: HTTP " + httpConnection.getResponseCode() + " " + error);
                return null;
            }

            try (InputStream in = connection.getInputStream()) {
                return gson.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8), DDTracking.class);
            }
        } catch (Exception e) {
            log.debug("DataDam tracking request failed", e);
            return null;
        }
    }
}
