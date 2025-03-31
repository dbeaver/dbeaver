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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MonitoredHttpClient implements AutoCloseable {
    private final HttpClient client;

    public MonitoredHttpClient(HttpClient client) {
        this.client = client;
    }

    /**
     * Send an HTTP request and return the response body as an object.
     * The request is sent asynchronously and the method will block until the response is received.
     * The method will also check if the progress monitor is cancelled and cancel the request if it is.
     */
    public <T> HttpResponse<T> send(
        DBRProgressMonitor monitor,
        HttpRequest request,
        HttpResponse.BodyHandler<T> responseBodyHandler
    ) throws DBException {
        monitor.beginTask("Request AI completion", 1);

        CompletableFuture<HttpResponse<T>> responseCompletableFuture = client.sendAsync(request, responseBodyHandler);

        try {
            monitor.subTask("Sending request to " + request.uri());
            while (true) {
                if (monitor.isCanceled()) {
                    responseCompletableFuture.cancel(true);
                    throw new InterruptedException();
                }

                if (responseCompletableFuture.isDone()) {
                    break;
                }

                TimeUnit.MILLISECONDS.sleep(100);
            }

            return responseCompletableFuture.get();
        } catch (InterruptedException e) {
            throw new DBException("Request was cancelled", e);
        } catch (ExecutionException e) {
            throw new DBException("Request failed", e);
        } finally {
            monitor.done();
        }
    }


    @Override
    public void close() {
        //TODO should be uncommented when transition to java 21 is complete
        //client.close();
    }
}
