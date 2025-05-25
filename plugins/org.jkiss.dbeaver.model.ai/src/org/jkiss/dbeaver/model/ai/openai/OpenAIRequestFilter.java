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
package org.jkiss.dbeaver.model.ai.openai;

import org.jkiss.code.NotNull;

import java.net.http.HttpRequest;

public class OpenAIRequestFilter implements OpenAIClient.HttpRequestFilter {
    private final String token;

    public OpenAIRequestFilter(String token) {
        this.token = token;
    }

    @NotNull
    @Override
    public HttpRequest filter(@NotNull HttpRequest request) {
        return HttpRequest.newBuilder(request.uri())
            .uri(request.uri())
            .method(request.method(), request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()))
            .header("Content-Type", "application/json")
            .headers("Authorization", "Bearer " + token)
            .build();
    }
}
