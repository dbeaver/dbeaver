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
package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractHttpAIClient implements AutoCloseable {
    protected final MonitoredHttpClient client;

    protected Duration timeout = Duration.ofSeconds(AIEngineProperties.DEFAULT_TIMEOUT);

    public AbstractHttpAIClient() {
        this.client = new MonitoredHttpClient(
            // Force HTTP/1.1: over plaintext http:// the default HTTP_2 client attempts an h2c upgrade,
            // and OpenAI-compatible servers like uvicorn/httptools then drop the request body
            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
            this::mapHttpError,
            this::processErrors
        );
    }

    public void setTimeout(int timeoutSeconds) {
        if (timeoutSeconds > 0) {
            this.timeout = Duration.ofSeconds(timeoutSeconds);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    /**
     * Maps an HTTP error response to a corresponding {@link DBException}.
     *
     * @param statusCode the HTTP status code of the error response
     * @param body the body of the HTTP error response, not null
     * @return a {@link DBException} that represents the mapped error, not null
     */
    @NotNull
    protected abstract DBException mapHttpError(int statusCode, @NotNull String body);

    protected boolean processErrors(
        @NotNull MonitoredHttpClient.ErrorMapper mapper,
        @NotNull Consumer<Throwable> errorHandler,
        @NotNull HttpResponse<Stream<String>> response,
        @NotNull AtomicBoolean suppressCompletion,
        @Nullable Consumer<String> backupOption,
        int statusCode
    ) {
        if (statusCode != 200) {
            String responseBody = response.body().collect(Collectors.joining());
            errorHandler.accept(mapper.map(statusCode, responseBody));
            return true;
        }
        return false;
    }
}
