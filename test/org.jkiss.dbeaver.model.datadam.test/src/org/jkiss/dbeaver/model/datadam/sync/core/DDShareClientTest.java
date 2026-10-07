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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

class DDShareClientTest {

    private static final String PROJECT_A = UUID.randomUUID().toString();
    private static final String PROJECT_B = UUID.randomUUID().toString();

    @Test
    void projectMetadataRoundTripsWithoutAnEncryptionKey() throws Exception {
        DDSyncCredentials credentials = Mockito.mock(DDSyncCredentials.class);
        AtomicReference<JsonObject> request = new AtomicReference<>();
        Mockito.when(credentials.buildToken(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenAnswer(invocation -> {
            byte[] body = invocation.getArgument(2);
            request.set(JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject());
            return "test-token";
        });
        JsonObject project = JsonParser.parseString("""
            {"id":"%s", "name":"Проект <test>", "description":"Plain description",
             "projectOwner":"%s", "createTime":"2026-09-29T08:00:00Z", "updateTime":"2026-09-29T08:00:00Z"}
            """.formatted(PROJECT_A, PROJECT_B)).getAsJsonObject();
        AtomicReference<String> response = new AtomicReference<>();
        DDShareClient client = new DDShareClient("http://localhost", credentials) {
            @NotNull
            @Override
            protected <T> T execute(@NotNull HttpRequest.Builder builder, @NotNull Type type) {
                return gson.fromJson(response.get(), type);
            }
        };

        response.set("{\"data\":{\"createProject\":" + project + "}}");
        var created = client.createProject(UUID.fromString(PROJECT_A), "Проект <test>", "Plain description");
        Assertions.assertEquals("Проект <test>", request.get().getAsJsonObject("variables").get("name").getAsString());
        Assertions.assertEquals("Plain description", request.get().getAsJsonObject("variables").get("description").getAsString());
        Assertions.assertEquals("Проект <test>", created.name());
        Assertions.assertEquals("Plain description", created.description());

        project.addProperty("name", "Renamed");
        project.add("description", null);
        response.set("{\"data\":{\"updateProject\":" + project + "}}");
        var updated = client.updateProject(UUID.fromString(PROJECT_A), "Renamed", null);
        Assertions.assertEquals("Renamed", request.get().getAsJsonObject("variables").get("name").getAsString());
        Assertions.assertEquals("Renamed", updated.name());
        Assertions.assertNull(updated.description());

        response.set("{\"data\":{\"projects\":[" + project + "]}}");
        Assertions.assertEquals(updated, client.listProjects().getFirst());
        Mockito.verify(credentials, Mockito.never()).getDataKey();
    }

    @Test
    void decryptRoundTripsWithMatchingProjectIdAndField() throws Exception {
        SecretKey key = generateKey();
        byte[] plaintext = "content".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = DDCrypto.encrypt(key, plaintext, DDShareClient.aad(PROJECT_A, "data-sources.json"));

        byte[] decrypted = DDCrypto.decrypt(key, encrypted, DDShareClient.aad(PROJECT_A, "data-sources.json"));

        Assertions.assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void decryptRejectsCiphertextRelabeledToAnotherProject() throws Exception {
        SecretKey key = generateKey();
        byte[] encrypted = DDCrypto.encrypt(
            key, "content".getBytes(StandardCharsets.UTF_8), DDShareClient.aad(PROJECT_A, "data-sources.json"));

        Assertions.assertThrows(
            DBException.class, () -> DDCrypto.decrypt(key, encrypted, DDShareClient.aad(PROJECT_B, "data-sources.json")));
    }

    @Test
    void decryptRejectsCiphertextRelabeledToAnotherField() throws Exception {
        SecretKey key = generateKey();
        byte[] encrypted = DDCrypto.encrypt(
            key, "content".getBytes(StandardCharsets.UTF_8), DDShareClient.aad(PROJECT_A, "data-sources.json"));

        Assertions.assertThrows(
            DBException.class,
            () -> DDCrypto.decrypt(key, encrypted, DDShareClient.aad(PROJECT_A, "credentials-config.json")));
    }

    @NotNull
    private static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }
}
