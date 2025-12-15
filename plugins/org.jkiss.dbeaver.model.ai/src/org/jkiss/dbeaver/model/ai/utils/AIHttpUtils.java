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
package org.jkiss.dbeaver.model.ai.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.net.URI;
import java.net.URISyntaxException;

public final class AIHttpUtils {
    private static final Log log = Log.getLog(AIHttpUtils.class);

    private AIHttpUtils() {
    }

    /**
     * Resolves URI from base and paths
     */
    public static URI resolve(String base, String... paths) throws DBException {
        try {
            URI uri = new URI(base);
            for (String path : paths) {
                uri = uri.resolve(path);
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new DBException("Incorrect URI", e);
        }
    }

    /**
     * Parses an OpenAI-style error message from a JSON string.
     * Extracts the "message" field from the "error" or root object of the JSON structure.
     * If the parsing fails or no suitable message field is found, the original input string is returned.
     *
     * @param body the JSON string containing an OpenAI-style error message
     * @return the extracted error message if present, otherwise the original input string
     */
    @NotNull
    public static String parseOpenAIStyleErrorMessage(@NotNull String body) {
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
