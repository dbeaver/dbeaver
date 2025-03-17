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
package org.jkiss.dbeaver.model.ai.completion;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class HttpClientCompletionEngine extends AbstractAICompletionEngine<HttpClient, HttpRequest> {
    private static final Map<String, HttpClient> clientInstances = new HashMap<>();

    @Override
    public Map<String, HttpClient> getServiceMap() {
        return clientInstances;
    }

    @Override
    protected HttpClient getServiceInstance(@NotNull DBCExecutionContext executionContext) throws DBException {
        DBPDataSourceContainer container = executionContext.getDataSource().getContainer();
        HttpClient service = clientInstances.get(container.getId());
        if (service == null) {
            service = initHttpClientInstance();
            clientInstances.put(container.getId(), service);
        }
        return service;
    }

    private HttpClient initHttpClientInstance() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        builder.connectTimeout(Duration.ofSeconds(30));
        return builder.build();
    }

    @NotNull
    protected static HttpResponse<String> sendRequest(
        @NotNull DBRProgressMonitor monitor,
        @NotNull HttpClient client,
        @NotNull HttpRequest completionRequest
    ) throws InterruptedException, DBException {
        CompletableFuture<HttpResponse<String>> responseAsync = client.sendAsync(completionRequest, HttpResponse.BodyHandlers.ofString());
        while (!responseAsync.isDone()) {
            if (monitor.isCanceled()) {
                responseAsync.cancel(true);
                throw new InterruptedException();
            }
        }
        HttpResponse<String> response = responseAsync.getNow(null);
        if (response == null) {
            throw new DBException("No response received");
        }
        if (response.statusCode() != 200) {
            throw new DBException("Error requesting completion " + response.body());
        }
        return response;
    }

}
