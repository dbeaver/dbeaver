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
package org.jkiss.dbeaver.model.datadam.sync.core;

import com.dbeaver.datadam.share.api.model.DDConfigurationPart;
import com.dbeaver.datadam.share.api.model.DDConfigurationPartKind;
import com.dbeaver.datadam.share.api.model.DDCreateConfigurationPartRequest;
import com.dbeaver.datadam.share.api.model.DDCreateConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationPartRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationRequest;
import com.dbeaver.rest.client.MediaType;
import com.google.gson.JsonObject;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.KeyGenerator;

class DDConfigurationClientTest {

    private static final UUID CONFIGURATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String PART_KEY = "mcp-server";
    private static final String CONFIGURATION = """
        {
            "configurationId": "%s",
            "name": "Desktop settings",
            "version": 8,
            "createTime": "2026-11-06T12:00:00+02:00",
            "lastSyncTime": null,
            "parts": [{"key": "%s", "kind": "ACCOUNT", "projectId": null, "version": 4, "encryptedValue": "encrypted"}]
        }""".formatted(CONFIGURATION_ID, PART_KEY);

    @Test
    void configurationMetadataUsesSsoWithoutEncryptionKeys() throws Exception {
        DDDesktopAccessService accessService = accessService();
        StubClient client = new StubClient(accessService);
        client.respond("configurations", "[" + CONFIGURATION + "]");

        var summary = client.listConfigurations().getFirst();

        Assertions.assertEquals(CONFIGURATION_ID, summary.configurationId());
        Assertions.assertEquals(LocalDateTime.of(2026, 11, 6, 10, 0), summary.createTime());
        Assertions.assertNull(summary.lastSyncTime());
        Assertions.assertEquals("Bearer test-token", client.request.headers().firstValue("Authorization").orElseThrow());
        Assertions.assertTrue(client.body.get("query").getAsString().contains("createTime: createdAt"));
        Assertions.assertTrue(client.body.get("query").getAsString().contains("lastSyncTime: lastSyncAt"));

        client.respond("configuration", CONFIGURATION);
        var configuration = client.getConfiguration(CONFIGURATION_ID);
        Assertions.assertNotNull(configuration);
        Assertions.assertEquals(4, configuration.parts().getFirst().version());

        var input = new DDCreateConfigurationRequest("Desktop settings", List.of(
            new DDCreateConfigurationPartRequest(PART_KEY, DDConfigurationPartKind.ACCOUNT, null, "encrypted")));
        client.respond("createConfiguration", CONFIGURATION);
        Assertions.assertEquals(configuration, client.createConfiguration(input));
        JsonObject part = client.body.getAsJsonObject("variables").getAsJsonObject("input")
            .getAsJsonArray("parts").get(0).getAsJsonObject();
        Assertions.assertEquals("ACCOUNT", part.get("kind").getAsString());
        Assertions.assertEquals("encrypted", part.get("encryptedValue").getAsString());
        Mockito.verify(accessService, Mockito.never()).getKeyBundle();
    }

    @Test
    void updatePreservesExpectedVersionsAndReturnsConflictsWithoutRetry() throws Exception {
        StubClient client = new StubClient(accessService());
        client.respond("updateConfiguration", "{\"configuration\":" + CONFIGURATION + ",\"conflictingKeys\":[\"" + PART_KEY + "\"]}");
        long expectedVersion = 4_294_967_296L;

        var result = client.updateConfiguration(CONFIGURATION_ID, new DDUpdateConfigurationRequest(expectedVersion, List.of(
            new DDUpdateConfigurationPartRequest(PART_KEY, 3, "new encrypted value"))));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(List.of(PART_KEY), result.conflictingKeys());
        Assertions.assertEquals(8, result.configuration().version());
        JsonObject variables = client.body.getAsJsonObject("variables");
        Assertions.assertEquals(CONFIGURATION_ID.toString(), variables.get("configurationId").getAsString());
        JsonObject input = variables.getAsJsonObject("input");
        Assertions.assertEquals(expectedVersion, input.get("expectedConfigurationVersion").getAsLong());
        Assertions.assertEquals(3, input.getAsJsonArray("parts").get(0).getAsJsonObject().get("expectedVersion").getAsLong());
        Assertions.assertEquals(1, client.calls);
    }

    @Test
    void missingConfigurationRemainsDistinctFromAConflict() throws Exception {
        StubClient client = new StubClient(accessService());
        client.respond("configuration", "null");
        Assertions.assertNull(client.getConfiguration(CONFIGURATION_ID));

        client.respond("updateConfiguration", "null");
        Assertions.assertNull(client.updateConfiguration(CONFIGURATION_ID, new DDUpdateConfigurationRequest(0, List.of(
            new DDUpdateConfigurationPartRequest(PART_KEY, 1, "encrypted")))));
    }

    @Test
    void graphQlFailureIsNotReportedAsAConflictingUpdate() throws Exception {
        StubClient client = new StubClient(accessService());
        client.response = """
            {"data":{"updateConfiguration":null},"errors":[
                {"message":"Storage unavailable","extensions":{"classification":"INTERNAL_ERROR"}}
            ]}""";

        Assertions.assertThrows(DDTransportException.class, () -> client.updateConfiguration(
            CONFIGURATION_ID, new DDUpdateConfigurationRequest(0, List.of(
                new DDUpdateConfigurationPartRequest(PART_KEY, 1, "encrypted")))));
        Assertions.assertEquals(1, client.calls);
    }

    @Test
    void encryptedPartsAreBoundToTheirAccountAndPartKey() throws Exception {
        DDDesktopAccessService accessService = accessService();
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        String key = Base64.getEncoder().encodeToString(generator.generateKey().getEncoded());
        Mockito.when(accessService.getKeyBundle()).thenReturn(new DDKeyBundle(ACCOUNT_ID, "unused", key, 1));
        StubClient client = new StubClient(accessService);
        byte[] content = "configuration content".getBytes(StandardCharsets.UTF_8);
        String encrypted = client.encryptPart(PART_KEY, content);
        DDConfigurationPart part = new DDConfigurationPart(PART_KEY, DDConfigurationPartKind.ACCOUNT, null, 1, encrypted);

        Assertions.assertArrayEquals(content, client.decryptPart(part));
        Assertions.assertThrows(DBException.class, () -> client.decryptPart(
            new DDConfigurationPart("another-part", DDConfigurationPartKind.ACCOUNT, null, 1, encrypted)));

        Mockito.when(accessService.getKeyBundle()).thenReturn(new DDKeyBundle(CONFIGURATION_ID, "unused", key, 1));
        Assertions.assertThrows(DBException.class, () -> client.decryptPart(part));

        Mockito.when(accessService.getKeyBundle()).thenReturn(null);
        Assertions.assertThrows(DBException.class, () -> client.encryptPart(PART_KEY, content));
    }

    @NotNull
    private static DDDesktopAccessService accessService() throws DBException {
        DDDesktopAccessService accessService = Mockito.mock(DDDesktopAccessService.class);
        Mockito.when(accessService.getAccessTokenForResource(Mockito.any())).thenReturn("test-token");
        return accessService;
    }

    private static final class StubClient extends DDConfigurationClient {
        private String response;
        private JsonObject body;
        private HttpRequest request;
        private int calls;

        private StubClient(@NotNull DDDesktopAccessService accessService) {
            super("https://storage.example.test", accessService);
        }

        private void respond(@NotNull String field, @NotNull String value) {
            response = "{\"data\":{\"" + field + "\":" + value + "}}";
        }

        @NotNull
        @Override
        protected HttpRequest.BodyPublisher createBodyPublisher(@Nullable Object value, @NotNull MediaType mediaType) {
            body = gson.toJsonTree(value).getAsJsonObject();
            return super.createBodyPublisher(value, mediaType);
        }

        @NotNull
        @Override
        protected <T> T execute(@NotNull HttpRequest.Builder builder, @NotNull Type type) {
            request = builder.build();
            calls++;
            return gson.fromJson(response, type);
        }
    }
}
