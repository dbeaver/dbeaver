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
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDTrackingClient;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.jkiss.dbeaver.model.datadam.auth.DDDesktopSsoSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
        JsonObject project = JsonParser.parseString("""
            {"id":"%s", "name":"Проект <test>", "description":"Plain description",
             "projectOwner":"%s", "createTime":"2026-09-29T08:00:00Z", "updateTime":"2026-09-29T08:00:00Z"}
            """.formatted(PROJECT_A, PROJECT_B)).getAsJsonObject();
        AtomicReference<String> response = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/graphql", exchange -> {
            request.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8))
                .getAsJsonObject());
            byte[] body = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try (var session = Mockito.mockStatic(DDDesktopSsoSession.class)) {
            session.when(() -> DDDesktopSsoSession.accessFor(Mockito.any())).thenReturn("test-token");
            DDShareClient client = new DDShareClient("http://127.0.0.1:" + server.getAddress().getPort(), credentials);

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
            Mockito.verifyNoInteractions(credentials);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void storageAndTrackingRequestsDoNotFollowRedirects() throws Exception {
        var redirected = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Location", base + "/redirected");
            exchange.sendResponseHeaders(307, -1);
            exchange.close();
        });
        server.createContext("/redirected", exchange -> {
            redirected.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try (var session = Mockito.mockStatic(DDDesktopSsoSession.class)) {
            session.when(() -> DDDesktopSsoSession.accessFor(Mockito.any())).thenReturn("test-token");
            DDSyncCredentials credentials = Mockito.mock(DDSyncCredentials.class);
            Assertions.assertThrows(DBException.class, () -> new DDShareClient(base, credentials).listConfigurations());
            Assertions.assertNull(new DDTrackingClient(base).stop(credentials, "tracking-id"));
            Assertions.assertEquals(0, redirected.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void storageCallsRequireSsoInsteadOfFallingBackToBundleSignatures() throws Exception {
        DDDesktopSsoSession.logout();
        DDSyncCredentials credentials = Mockito.mock(DDSyncCredentials.class);
        DDShareClient client = new DDShareClient("http://127.0.0.1:1", credentials);

        DBException error = Assertions.assertThrows(DBException.class, client::listConfigurations);
        Assertions.assertTrue(error.getMessage().contains("Sign in to DataDam"));
        Mockito.verifyNoInteractions(credentials);
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
