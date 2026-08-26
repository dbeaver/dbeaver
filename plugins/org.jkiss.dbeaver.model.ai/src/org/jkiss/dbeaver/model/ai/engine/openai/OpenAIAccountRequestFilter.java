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
import org.jkiss.dbeaver.DBException;
import org.jkiss.utils.HttpConstants;

import java.net.http.HttpRequest;

public class OpenAIAccountRequestFilter implements OpenAiClientBase.HttpRequestFilter {
    private final OpenAIProperties properties;
    private final OpenAIAccountAuthenticator authenticator;

    public OpenAIAccountRequestFilter(@NotNull OpenAIProperties properties) {
        this.properties = properties;
        authenticator = new OpenAIAccountAuthenticator(properties.getTimeout());
    }

    @NotNull
    @Override
    public HttpRequest filter(@NotNull HttpRequest request, boolean setContentType) throws DBException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
            .method(request.method(), request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()))
            .header(HttpConstants.HEADER_AUTHORIZATION, "Bearer " + properties.getValidAccessToken(authenticator))
            .header("originator", "dbeaver");
        String accountId = properties.getAccountId();
        if (accountId != null) {
            builder.header("ChatGPT-Account-Id", accountId);
        }
        request.headers().map().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        if (setContentType) {
            builder.header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
        }
        request.timeout().ifPresent(builder::timeout);
        return builder.build();
    }
}
