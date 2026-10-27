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
package org.jkiss.dbeaver.ext.cloudflare.d1.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cloudflare.d1.CloudflareD1ConnectionSettings;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class CloudflareD1DiscoveryTest {
    @Test
    void deletionRemovesTheDatabaseFromSavedAndResolvedScopesBeforeRefreshing() throws Exception {
        var source = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        var saved = scopedConfiguration();
        try (var server = new AdministrationServer(200, 2)) {
            server.configure(saved);
            var resolved = new DBPConnectionConfiguration(saved);
            Mockito.when(source.getContainer().getConnectionConfiguration()).thenReturn(saved);
            Mockito.when(source.getContainer().getActualConnectionConfiguration()).thenReturn(resolved);
            Mockito.doCallRealMethod().when(source).removeDatabaseScope("deleted-id");
            var catalog = new CloudflareD1Catalog(source,
                new CloudflareD1Database("deleted-id", "deleted", null, null, null, null, null, null));
            var monitor = new VoidProgressMonitor();
            Mockito.when(source.refreshObject(monitor)).thenAnswer(invocation -> {
                Assertions.assertEquals("initial-id", saved.getProperty(CloudflareD1ConnectionSettings.DATABASE_IDS));
                Assertions.assertEquals("initial-id", resolved.getProperty(CloudflareD1ConnectionSettings.DATABASE_IDS));
                Assertions.assertEquals(List.of("initial-id"), CloudflareD1ConnectionAdapter.list(monitor, source).stream()
                    .map(CloudflareD1Database::id).toList());
                return source;
            });

            CloudflareD1ConnectionAdapter.delete(monitor, catalog);

            Assertions.assertEquals(List.of(
                "DELETE /client/v4/accounts/account/d1/database/deleted-id HTTP/1.1",
                "GET /client/v4/accounts/account/d1/database/initial-id HTTP/1.1"
            ), server.requests.get(5, TimeUnit.SECONDS));
            var order = Mockito.inOrder(source, source.getContainer());
            order.verify(source).removeDatabaseScope("deleted-id");
            order.verify(source.getContainer()).persistConfiguration(false);
            order.verify(source).refreshObject(monitor);
        }
    }

    @Test
    void failedDeletionDoesNotChangeEitherDatabaseScope() throws Exception {
        var source = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        var saved = scopedConfiguration();
        try (var server = new AdministrationServer(403, 1)) {
            server.configure(saved);
            var resolved = new DBPConnectionConfiguration(saved);
            Mockito.when(source.getContainer().getConnectionConfiguration()).thenReturn(saved);
            Mockito.when(source.getContainer().getActualConnectionConfiguration()).thenReturn(resolved);
            var catalog = new CloudflareD1Catalog(source,
                new CloudflareD1Database("deleted-id", "deleted", null, null, null, null, null, null));

            Assertions.assertThrows(DBException.class, () -> CloudflareD1ConnectionAdapter.delete(new VoidProgressMonitor(), catalog));

            Assertions.assertEquals(1, server.requests.get(5, TimeUnit.SECONDS).size());
            Assertions.assertEquals("initial-id,deleted-id", saved.getProperty(CloudflareD1ConnectionSettings.DATABASE_IDS));
            Assertions.assertEquals("initial-id,deleted-id", resolved.getProperty(CloudflareD1ConnectionSettings.DATABASE_IDS));
            Mockito.verify(source, Mockito.never()).removeDatabaseScope(Mockito.anyString());
            Mockito.verify(source.getContainer(), Mockito.never()).persistConfiguration(false);
            Mockito.verify(source, Mockito.never()).refreshObject(Mockito.any());
        }
    }

    @Test
    void deletingFromAnUnfilteredConnectionDoesNotIntroduceADatabaseScope() {
        var configuration = new DBPConnectionConfiguration();
        configuration.setDatabaseName("initial-id");

        Assertions.assertFalse(CloudflareD1ConnectionSettings.removeDatabaseFromScope(configuration, "deleted-id"));
        Assertions.assertFalse(configuration.getProperties().containsKey(CloudflareD1ConnectionSettings.DATABASE_IDS));
    }

    @NotNull
    private static DBPConnectionConfiguration scopedConfiguration() {
        var configuration = new DBPConnectionConfiguration();
        configuration.setDatabaseName("initial-id");
        configuration.setProperty(CloudflareD1ConnectionSettings.DATABASE_IDS, "initial-id,deleted-id");
        return configuration;
    }

    private static final class AdministrationServer implements AutoCloseable {
        private final ServerSocket server;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Future<List<String>> requests;

        private AdministrationServer(int deleteStatus, int requestCount) throws IOException {
            server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            server.setSoTimeout(5000);
            requests = executor.submit(() -> {
                List<String> received = new ArrayList<>();
                for (int index = 0; index < requestCount; index++) {
                    try (var socket = server.accept()) {
                        socket.setSoTimeout(5000);
                        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                        String request = reader.readLine();
                        received.add(request);
                        String header;
                        while ((header = reader.readLine()) != null && !header.isEmpty()) {
                            // GET and DELETE requests in these tests have no body.
                        }
                        int status = request.startsWith("DELETE ") ? deleteStatus : 200;
                        String body = status == 200
                            ? "{\"success\":true,\"result\":{\"uuid\":\"initial-id\",\"name\":\"initial\"}}"
                            : "{\"success\":false}";
                        byte[] content = body.getBytes(StandardCharsets.UTF_8);
                        var output = socket.getOutputStream();
                        output.write(("HTTP/1.1 " + status + " Response\r\nContent-Type: application/json\r\nContent-Length: "
                            + content.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                        output.write(content);
                        output.flush();
                    }
                }
                return received;
            });
        }

        private void configure(@NotNull DBPConnectionConfiguration configuration) {
            configuration.setUserName("account");
            configuration.setUserPassword("test-token");
            configuration.setProperty("apiUrl", "http://127.0.0.1:" + server.getLocalPort() + "/client/v4");
            configuration.setProperty("queryTimeout", "5000");
        }

        @Override
        public void close() throws IOException {
            try {
                server.close();
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void scopedCatalogsLoadOnlyConfiguredDatabasesWithoutAccountEnumeration() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        var first = response("{\"uuid\":\"first-id\",\"name\":\"first\"}", "");
        var second = response("{\"uuid\":\"second-id\",\"name\":\"second\"}", "");
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(first, second);
        var configuration = new DBPConnectionConfiguration();
        configuration.setDatabaseName("first-id");
        configuration.setProperty("databaseIds", "second-id,first-id,second-id");

        var databases = CloudflareD1ConnectionAdapter.listDatabases(client(httpClient), configuration);

        Assertions.assertEquals(List.of("first-id", "second-id"), databases.stream().map(CloudflareD1Database::id).toList());
        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        Mockito.verify(httpClient, Mockito.times(2))
            .send(requests.capture(), Mockito.<HttpResponse.BodyHandler<String>>any());
        Assertions.assertEquals(List.of("/client/v4/accounts/account/d1/database/first-id",
                "/client/v4/accounts/account/d1/database/second-id"),
            requests.getAllValues().stream().map(request -> request.uri().getPath()).toList());
    }

    @Test
    void unscopedCatalogsStillEnumerateAllAccessibleDatabases() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        var response = response("[{\"uuid\":\"first-id\",\"name\":\"first\"},{\"uuid\":\"other-id\",\"name\":\"other\"}]",
            ",\"result_info\":{\"total_count\":2}");
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);

        var databases = CloudflareD1ConnectionAdapter.listDatabases(client(httpClient), new DBPConnectionConfiguration());

        Assertions.assertEquals(List.of("first-id", "other-id"), databases.stream().map(CloudflareD1Database::id).toList());
    }

    @Test
    void administrationReadsResolvedCredentialsRatherThanSavedConfiguration() {
        var source = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        var saved = Mockito.mock(DBPConnectionConfiguration.class);
        var resolved = Mockito.mock(DBPConnectionConfiguration.class);
        Mockito.when(source.getContainer().getConnectionConfiguration()).thenReturn(saved);
        Mockito.when(source.getContainer().getActualConnectionConfiguration()).thenReturn(resolved);
        Mockito.when(resolved.getUserName()).thenReturn("resolved-account");
        // Stop before networking, after verifying which configuration supplies the credentials.
        Mockito.when(resolved.getUserPassword()).thenReturn(null);

        Assertions.assertThrows(DBException.class,
            () -> CloudflareD1ConnectionAdapter.list(Mockito.mock(DBRProgressMonitor.class), source));

        Mockito.verify(resolved).getUserName();
        Mockito.verify(resolved).getUserPassword();
        Mockito.verifyNoInteractions(saved);
    }

    @Test
    void readsAllPagesWhenServerClampsPageSizeAndOmitsTotals() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        var first = response("[{\"uuid\":\"first-id\",\"name\":\"first database\"}]", "");
        var second = response("[{\"uuid\":\"second-id\",\"name\":\"second database\"}]", "");
        var empty = response("[]", "");
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(first, second, empty);

        var databases = client(httpClient).listDatabases();

        Assertions.assertEquals(List.of("first-id", "second-id"), databases.stream().map(CloudflareD1Database::id).toList());
        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        Mockito.verify(httpClient, Mockito.times(3))
            .send(requests.capture(), Mockito.<HttpResponse.BodyHandler<String>>any());
        Assertions.assertEquals(List.of("page=1&per_page=10000", "page=2&per_page=10000", "page=3&per_page=10000"),
            requests.getAllValues().stream().map(request -> request.uri().getQuery()).toList());
    }

    @Test
    void honorsTotalPagesWithoutAssumingRequestedPageSize() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        var first = response("[{\"uuid\":\"first-id\",\"name\":\"first\"}]", ",\"result_info\":{\"total_pages\":2}");
        var second = response("[{\"uuid\":\"second-id\",\"name\":\"second\"}]", ",\"result_info\":{\"total_pages\":2}");
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(first, second);

        Assertions.assertEquals(2, client(httpClient).listDatabases().size());
        Mockito.verify(httpClient, Mockito.times(2))
            .send(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    void emptyAccountReturnsAnEmptyList() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        var empty = response("[]", "");
        Mockito.when(httpClient.send(Mockito.any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
            .thenReturn(empty);

        Assertions.assertEquals(List.of(), client(httpClient).listDatabases());
    }

    @NotNull
    private static CloudflareD1AdminClient client(@NotNull HttpClient httpClient) {
        return new CloudflareD1AdminClient("account", "test-token", URI.create("https://example.com/client/v4"),
            Duration.ofSeconds(30), httpClient);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static HttpResponse<String> response(@NotNull String databases, @NotNull String pageInfo) {
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.statusCode()).thenReturn(200);
        Mockito.when(response.body()).thenReturn("{\"success\":true,\"result\":" + databases + pageInfo + "}");
        return response;
    }
}
