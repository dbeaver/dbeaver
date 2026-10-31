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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

class CloudflareD1DiscoveryTest {
    @Test
    void deletingFromAnUnfilteredConnectionDoesNotIntroduceADatabaseScope() {
        var configuration = new DBPConnectionConfiguration();
        configuration.setDatabaseName("initial-id");

        Assertions.assertFalse(CloudflareD1ConnectionSettings.removeDatabaseFromScope(configuration, "deleted-id"));
        Assertions.assertFalse(configuration.getProperties().containsKey(CloudflareD1ConnectionSettings.DATABASE_IDS));
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
    private static CloudflareD1Client client(@NotNull HttpClient httpClient) {
        return new CloudflareD1Client("account", "test-token", URI.create("https://example.com/client/v4"),
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
