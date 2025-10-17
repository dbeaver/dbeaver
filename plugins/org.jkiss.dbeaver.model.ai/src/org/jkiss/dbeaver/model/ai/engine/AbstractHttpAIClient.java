/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;
import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.net.http.HttpClient;

public abstract class AbstractHttpAIClient implements AutoCloseable {
    private static final Log log = Log.getLog(AbstractHttpAIClient.class);
    protected final MonitoredHttpClient client;

    public AbstractHttpAIClient() {
        this.client = new MonitoredHttpClient(
            HttpClient.newHttpClient(),
            this::mapHttpError
        );
    }

    @Override
    public void close() {
        client.close();
    }

    @NotNull
    protected DBException mapHttpError(int statusCode, @NotNull String body) {
        return new DBException("AI request failed: " + statusCode + ", " + parseErrorMessage(body));
    }

    @NotNull
    private static String parseErrorMessage(@NotNull String body) {
        try {
            JsonElement errorResponse = JSONUtils.GSON.fromJson(body, JsonElement.class);
            if (errorResponse != null && errorResponse.isJsonObject()) {
                if (errorResponse.getAsJsonObject().has("error")) {
                    JsonElement errorElement = errorResponse.getAsJsonObject().get("error");
                    if (errorElement.isJsonObject() && errorElement.getAsJsonObject().has("message")) {
                        JsonElement messageElement = errorElement.getAsJsonObject().get("message");
                        if (messageElement.isJsonPrimitive() && messageElement.getAsJsonPrimitive().isString()) {
                            return messageElement.getAsString();
                        }
                    }
                }
                if (errorResponse.getAsJsonObject().has("message")) {
                    JsonElement messageElement = errorResponse.getAsJsonObject().get("message");
                    if (messageElement.isJsonPrimitive() && messageElement.getAsJsonPrimitive().isString()) {
                        return messageElement.getAsString();
                    }
                }
            }
            return body;
        } catch (JsonSyntaxException e) {
            log.debug("Failed to parse error response: " + e.getMessage());
            return body;
        }
    }

}
