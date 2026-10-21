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
package org.jkiss.dbeaver.model.ai.engine.openai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.utils.AIHttpRequestFilter;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.net.http.HttpRequest;
import java.util.Map;

public class OpenAIRequestFilter implements AIHttpRequestFilter {
    @Nullable
    private final String token;
    private final Map<String, String> customHeaders;

    public OpenAIRequestFilter(@Nullable String token) {
        this(token, Map.of());
    }

    public OpenAIRequestFilter(@Nullable String token, @NotNull Map<String, String> customHeaders) {
        this.token = token;
        this.customHeaders = Map.copyOf(customHeaders);
    }

    @NotNull
    @Override
    public HttpRequest filter(@NotNull HttpRequest request, boolean setContentType) throws DBException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
            .method(request.method(), request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));
        // preserve the settings configured on the original request
        request.timeout().ifPresent(builder::timeout);
        request.version().ifPresent(builder::version);
        for (var headerEntry : request.headers().map().entrySet()) {
            for (String value : headerEntry.getValue()) {
                builder.header(headerEntry.getKey(), value);
            }
        }

        if (!CommonUtils.isEmptyTrimmed(token)) {
            builder.setHeader(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.BEARER_PREFIX + token);
        }
        if (setContentType) {
            builder.setHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
        }
        for (var header : customHeaders.entrySet()) {
            try {
                builder.setHeader(header.getKey(), header.getValue());
            } catch (IllegalArgumentException e) {
                // header values may contain credentials, so don't include the original exception
                throw new DBException("Invalid or unsupported HTTP header: " + header.getKey());
            }
        }
        return builder.build();
    }

    @Nullable
    public static String findInvalidHeader(@NotNull Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        for (var header : headers.entrySet()) {
            try {
                builder.setHeader(header.getKey(), header.getValue());
            } catch (IllegalArgumentException e) {
                return header.getKey();
            }
        }
        return null;
    }
}
