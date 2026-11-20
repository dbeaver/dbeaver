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
package org.jkiss.dbeaver.model.ai.utils;

import org.jkiss.dbeaver.DBException;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MonitoredHttpClientTest extends DBeaverUnitTest {
    @Test
    public void closesFromResponseCallbackWithoutWaitingForBody() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            try (StreamingServer server = new StreamingServer(); MonitoredHttpClient client = createClient()) {
                CountDownLatch closeReturned = new CountDownLatch(1);
                var future = client.sendAsync(
                    server.request(),
                    event -> {
                        client.close();
                        closeReturned.countDown();
                    },
                    error -> { },
                    () -> { }
                );
                try {
                    Assertions.assertTrue(closeReturned.await(2, TimeUnit.SECONDS), "Closing an active stream callback blocked");
                    future.handle((result, error) -> null).get(5, TimeUnit.SECONDS);
                    Assertions.assertTrue(client.getHttpClient().awaitTermination(Duration.ofSeconds(5)));
                } finally {
                    client.getHttpClient().shutdownNow();
                }
            }
        }
    }

    @Test
    public void receivesCompleteResponseBeforeClosing() throws Exception {
        try (StreamingServer server = new StreamingServer(); MonitoredHttpClient client = createClient()) {
            List<String> events = new ArrayList<>();
            AtomicInteger completionCount = new AtomicInteger();
            server.finishResponse.countDown();
            client.sendAsync(
                server.request(), events::add, Assertions::fail, () -> {
                    client.close();
                    completionCount.incrementAndGet();
                }
            ).get(5, TimeUnit.SECONDS);

            Assertions.assertEquals(List.of("first", "last"), events);
            Assertions.assertEquals(1, completionCount.get());
        }
    }

    @Test
    public void preservesAnotherResponseDuringCallbackClose() throws Exception {
        try (StreamingServer canceledServer = new StreamingServer(); StreamingServer otherServer = new StreamingServer();
            MonitoredHttpClient client = createClient()) {
            CountDownLatch otherResponseStarted = new CountDownLatch(1);
            CountDownLatch closeStarted = new CountDownLatch(1);
            CountDownLatch closeReturned = new CountDownLatch(1);
            List<String> events = new ArrayList<>();
            var otherFuture = client.sendAsync(otherServer.request(), event -> {
                events.add(event);
                otherResponseStarted.countDown();
            }, Assertions::fail, () -> { });
            try {
                Assertions.assertTrue(otherResponseStarted.await(2, TimeUnit.SECONDS));
                var canceledFuture = client.sendAsync(canceledServer.request(), event -> {
                    closeStarted.countDown();
                    client.close();
                    closeReturned.countDown();
                }, error -> { }, () -> { });

                Assertions.assertTrue(closeStarted.await(2, TimeUnit.SECONDS));
                Assertions.assertFalse(closeReturned.await(200, TimeUnit.MILLISECONDS), "Another response was interrupted");
                otherServer.finishResponse.countDown();
                otherFuture.get(5, TimeUnit.SECONDS);
                Assertions.assertTrue(closeReturned.await(5, TimeUnit.SECONDS));
                canceledFuture.handle((result, error) -> null).get(5, TimeUnit.SECONDS);
                Assertions.assertEquals(List.of("first", "last"), events);
            } finally {
                otherServer.finishResponse.countDown();
                client.getHttpClient().shutdownNow();
            }
        }
    }

    @Test
    public void closesFromErrorCallbackAfterReleasingResponse() throws Exception {
        try (StreamingServer server = new StreamingServer(); MonitoredHttpClient client = createClient()) {
            AtomicReference<Throwable> error = new AtomicReference<>();
            CountDownLatch closeReturned = new CountDownLatch(1);
            var future = client.sendAsync(server.request(), event -> {
                throw new IllegalStateException("Invalid response event");
            }, failure -> {
                error.set(failure);
                client.close();
                closeReturned.countDown();
            }, Assertions::fail);
            try {
                Assertions.assertTrue(closeReturned.await(2, TimeUnit.SECONDS), "Error callback waited for unread response data");
                future.handle((result, failure) -> null).get(5, TimeUnit.SECONDS);
                Assertions.assertInstanceOf(IllegalStateException.class, error.get().getCause());
            } finally {
                client.getHttpClient().shutdownNow();
            }
        }
    }

    private static MonitoredHttpClient createClient() {
        return new MonitoredHttpClient(
            HttpClient.newHttpClient(),
            (status, body) -> new DBException(body),
            (mapper, errorHandler, response, suppressCompletion, backupOption, status) -> false
        );
    }

    private static class StreamingServer implements AutoCloseable {
        private final ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        private final CountDownLatch finishResponse = new CountDownLatch(1);
        private final Thread thread;

        private StreamingServer() throws IOException {
            thread = Thread.ofVirtual().start(() -> {
                try (var socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        // consume request headers before returning the streaming response
                    }
                    var output = socket.getOutputStream();
                    output.write(("HTTP/1.1 200 OK\r\nContent-Length: 11\r\nConnection: close\r\n\r\nfirst\n")
                        .getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    if (finishResponse.await(5, TimeUnit.SECONDS)) {
                        output.write("last\n".getBytes(StandardCharsets.UTF_8));
                        output.flush();
                    }
                } catch (IOException expected) {
                    // cancellation closes the connection before the body finishes
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        private HttpRequest request() {
            return HttpRequest.newBuilder(URI.create("http://localhost:" + server.getLocalPort())).build();
        }

        @Override
        public void close() throws Exception {
            finishResponse.countDown();
            server.close();
            thread.join(Duration.ofSeconds(5));
        }
    }
}
