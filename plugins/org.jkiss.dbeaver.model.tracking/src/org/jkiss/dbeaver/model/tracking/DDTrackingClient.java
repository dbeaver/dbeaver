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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.utils.HttpConstants;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

class DDTrackingClient {

    private static final Log log = Log.getLog(DDTrackingClient.class);

    private static final String GRAPHQL_PATH = "/graphql";
    private static final int START_TIMEOUT_MS = 10000;
    private static final int STOP_TIMEOUT_MS = 3000;

    private static final String START_MUTATION =
        "mutation($info: ClientInfo!) { start(info: $info) { trackingId } }";
    private static final String STOP_MUTATION =
        "mutation($clientId: ID!, $trackingId: ID!) { stop(clientId: $clientId, trackingId: $trackingId) { trackingId } }";

    private final String endpoint;
    private final String key;

    DDTrackingClient(@NotNull String url, @NotNull String key) {
        this.endpoint = url + GRAPHQL_PATH;
        this.key = key;
    }

    @Nullable
    String start(@NotNull DDClientInfo clientInfo) {
        JsonObject info = new JsonObject();
        info.addProperty("clientId", clientInfo.clientId());
        info.addProperty("workspaceId", clientInfo.workspaceId());
        info.addProperty("product", clientInfo.product());
        info.addProperty("version", clientInfo.version());
        addIfPresent(info, "os", clientInfo.os());
        addIfPresent(info, "macAddress", clientInfo.macAddress());
        addIfPresent(info, "ipAddress", clientInfo.ipAddress());
        JsonObject variables = new JsonObject();
        variables.add("info", info);

        JsonObject data = execute(START_MUTATION, variables, START_TIMEOUT_MS);
        if (data == null) {
            return null;
        }
        return data.getAsJsonObject("start").get("trackingId").getAsString();
    }

    private static void addIfPresent(@NotNull JsonObject json, @NotNull String key, @Nullable String value) {
        if (value != null) {
            json.addProperty(key, value);
        }
    }

    void stop(@NotNull String clientId, @NotNull String trackingId) {
        JsonObject variables = new JsonObject();
        variables.addProperty("clientId", clientId);
        variables.addProperty("trackingId", trackingId);
        execute(STOP_MUTATION, variables, STOP_TIMEOUT_MS);
    }

    @Nullable
    private JsonObject execute(@NotNull String query, @NotNull JsonObject variables, int timeoutMs) {
        JsonObject request = new JsonObject();
        request.addProperty("query", query);
        request.add("variables", variables);

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
            headers.put(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.BEARER_PREFIX + key);

            URLConnection connection = WebUtils.openURLConnection(endpoint, null, null, "POST", 0, timeoutMs, headers);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(new Gson().toJson(request).getBytes(StandardCharsets.UTF_8));
            }

            String response;
            try (InputStream in = connection.getInputStream()) {
                response = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (json.has("errors")) {
                log.debug("DataDam tracking error: " + json.get("errors"));
                return null;
            }
            return json.getAsJsonObject("data");
        } catch (Exception e) {
            log.debug("DataDam tracking request failed", e);
            return null;
        }
    }
}
