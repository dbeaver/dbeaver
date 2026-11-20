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

import com.dbeaver.datadam.share.api.exception.DDShareException;
import com.dbeaver.datadam.share.api.model.DDSharedProjectConfiguration;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.utils.Pair;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

class DDShareClientTest {

    private static final String PROJECT_A = UUID.randomUUID().toString();
    private static final String PROJECT_B = UUID.randomUUID().toString();

    @Test
    void projectCreationSendsTheImportedCredentialId() throws Exception {
        AtomicReference<JsonObject> request = new AtomicReference<>();
        AtomicReference<String> credentialId = new AtomicReference<>();
        UUID keyId = UUID.randomUUID();
        JsonObject project = JsonParser.parseString("""
            {"id":"%s", "name":"Проект <test>", "description":"Plain description",
             "projectOwner":"%s", "createTime":"2026-09-29T08:00:00Z", "updateTime":"2026-09-29T08:00:00Z"}
            """.formatted(PROJECT_A, PROJECT_B)).getAsJsonObject();
        AtomicReference<String> response = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/graphql", exchange -> {
            credentialId.set(exchange.getRequestHeaders().getFirst("X-DataDam-Credential-Id"));
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
        try (var session = Mockito.mockStatic(DDDesktopAccessService.class)) {
            var manager = Mockito.mock(DDDesktopAccessService.class);
            session.when(DDDesktopAccessService::getInstance).thenReturn(manager);
            Mockito.when(manager.getAccessTokenForResource(Mockito.any())).thenReturn("test-token");
            DDShareClient client = client("http://127.0.0.1:" + server.getAddress().getPort(), manager);

            Mockito.when(manager.getKeyBundle()).thenReturn(new DDKeyBundle(UUID.fromString(PROJECT_B), "unused", "unused", 1, keyId));
            response.set("{\"data\":{\"project\":" + project + "}}");
            var created = client.createProject(UUID.fromString(PROJECT_A), "Проект <test>", "Plain description");
            Assertions.assertEquals("Проект <test>", request.get().getAsJsonObject("variables").get("name").getAsString());
            Assertions.assertEquals("Plain description", request.get().getAsJsonObject("variables").get("description").getAsString());
            Assertions.assertNull(credentialId.get());
            Assertions.assertEquals(keyId.toString(), request.get().getAsJsonObject("variables").get("apiKeyId").getAsString());
            Assertions.assertEquals("Проект <test>", created.name());
            Assertions.assertEquals("Plain description", created.description());

            project.addProperty("name", "Renamed");
            project.add("description", null);
            response.set("{\"data\":{\"project\":" + project + "}}");
            var updated = client.updateProject(UUID.fromString(PROJECT_A), "Renamed", null);
            Assertions.assertEquals("Renamed", request.get().getAsJsonObject("variables").get("name").getAsString());
            Assertions.assertEquals("Renamed", updated.name());
            Assertions.assertNull(updated.description());

            response.set("{\"data\":{\"projects\":[" + project + "]}}");
            Assertions.assertEquals(updated, client.listProjects().getFirst());
            Mockito.verify(manager).getKeyBundle();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void uploadUsesTheSameBundleForEncryptionAndCredentialParameter() throws Exception {
        SecretKey key = KeyGenerator.getInstance("AES").generateKey();
        UUID keyId = UUID.randomUUID();
        var bundle = new DDKeyBundle(UUID.randomUUID(), "unused", Base64.getEncoder().encodeToString(key.getEncoded()), 1, keyId);
        var request = new AtomicReference<JsonObject>();
        var credentialId = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/graphql", exchange -> {
            credentialId.set(exchange.getRequestHeaders().getFirst("X-DataDam-Credential-Id"));
            request.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8))
                .getAsJsonObject());
            byte[] body = "{\"data\":{\"revision\":{}}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            var access = Mockito.mock(DDDesktopAccessService.class);
            Mockito.when(access.getAccessTokenForResource(Mockito.any())).thenReturn("token");
            Mockito.when(access.getKeyBundle())
                .thenReturn(bundle, new DDKeyBundle(bundle.accountId(), "other", "other", 1, UUID.randomUUID()));
            var client = client("http://127.0.0.1:" + server.getAddress().getPort(), access);
            byte[] contents = "project configuration".getBytes(StandardCharsets.UTF_8);
            client.pushFiles(UUID.fromString(PROJECT_A),
                new PreparedFiles(Map.of("data-sources.json", new Pair<>("file", contents)), "next"), "old");
            Assertions.assertNull(credentialId.get());
            Assertions.assertEquals(keyId.toString(), request.get().getAsJsonObject("variables").get("apiKeyId").getAsString());
            String encrypted = request.get().getAsJsonObject("variables").getAsJsonObject("projectContent")
                .getAsJsonArray("files").get(0).getAsJsonObject().get("encryptedContents").getAsString();
            Assertions.assertArrayEquals(contents, DDCrypto.decrypt(key, Base64.getDecoder().decode(encrypted),
                DDShareClient.aad(PROJECT_A, "data-sources.json")));
            Mockito.verify(access).getKeyBundle();
            Mockito.when(access.getKeyBundle()).thenReturn(new DDKeyBundle(bundle.accountId(), "old", "old", 1));
            Assertions.assertThrows(DDShareException.class, () -> client.createProject(UUID.randomUUID(), "Legacy", null));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void revisionPullPushAndDeleteResponsesUseTheirGraphQlAliases() throws Exception {
        AtomicReference<JsonObject> request = new AtomicReference<>();
        AtomicReference<String> response = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/graphql", exchange -> {
            request.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8))
                .getAsJsonObject());
            byte[] body = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            var manager = Mockito.mock(DDDesktopAccessService.class);
            Mockito.when(manager.getAccessTokenForResource(Mockito.any())).thenReturn("test-token");
            Mockito.when(manager.getKeyBundle())
                .thenReturn(new DDKeyBundle(UUID.fromString(PROJECT_B), "unused", "unused", 1, UUID.randomUUID()));
            var client = client("http://127.0.0.1:" + server.getAddress().getPort(), manager);
            String revision = """
                {"id":"%s","userId":"%s","updateTime":"2026-09-29T08:00:00Z","configurationFingerprint":"fp"}
                """.formatted(PROJECT_A, PROJECT_B).trim();

            response.set("{\"data\":{\"revision\":" + revision + "}}");
            var current = client.getCurrentProjectRevision(UUID.fromString(PROJECT_A));
            Assertions.assertEquals(UUID.fromString(PROJECT_A), current.id());
            Assertions.assertEquals(LocalDateTime.parse("2026-09-29T08:00:00"), current.updateTime());
            Assertions.assertTrue(request.get().get("query").getAsString().contains("revision: projectCurrentRevision"));

            response.set("{\"data\":{\"projectPullConfiguration\":{\"files\":[{\"fileName\":\"prefs.json\","
                + "\"encryptedContents\":\"AA==\",\"fingerprint\":\"file-fp\"}],\"currentRevision\":" + revision + "}}}");
            var pulled = client.pullProjectConfiguration(UUID.fromString(PROJECT_A));
            Assertions.assertEquals(current, pulled.currentRevision());
            Assertions.assertEquals("prefs.json", pulled.files().getFirst().fileName());

            response.set("{\"data\":{\"revision\":" + revision + "}}");
            var pushed = client.pushProjectConfiguration(UUID.fromString(PROJECT_A),
                new DDSharedProjectConfiguration("fp", List.of()), "previous-fp");
            Assertions.assertEquals(current, pushed);
            Assertions.assertEquals("fp", request.get().getAsJsonObject("variables")
                .getAsJsonObject("projectContent").get("configurationFingerprint").getAsString());

            response.set("{\"data\":{\"revisions\":[" + revision + "]}}");
            var revisions = client.getProjectRevisions(UUID.fromString(PROJECT_A), null, null);
            Assertions.assertEquals(List.of(current), revisions);
            Assertions.assertTrue(request.get().getAsJsonObject("variables").get("startTime").isJsonNull());

            response.set("{\"data\":{\"deleteProject\":true}}");
            Assertions.assertTrue(client.deleteProject(UUID.fromString(PROJECT_A)));
            response.set("{\"data\":{\"deleteProject\":false}}");
            Assertions.assertFalse(client.deleteProject(UUID.fromString(PROJECT_A)));

            response.set("{\"data\":{\"revision\":null}}");
            Assertions.assertThrows(DDShareException.class,
                () -> client.pushProjectConfiguration(UUID.fromString(PROJECT_A),
                    new DDSharedProjectConfiguration("fp", List.of()), "previous-fp"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void storageRequestsDoNotFollowRedirects() throws Exception {
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
        try (var session = Mockito.mockStatic(DDDesktopAccessService.class)) {
            var manager = Mockito.mock(DDDesktopAccessService.class);
            session.when(DDDesktopAccessService::getInstance).thenReturn(manager);
            Mockito.when(manager.getAccessTokenForResource(Mockito.any())).thenReturn("test-token");
            Assertions.assertThrows(DDShareException.class, () -> client(base, manager).listProjects());
            Assertions.assertEquals(0, redirected.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void storageCallsRequireSsoInsteadOfFallingBackToBundleSignatures() throws Exception {
        DDDesktopAccessService accessService = Mockito.mock(DDDesktopAccessService.class);
        Mockito.when(accessService.getAccessTokenForResource(Mockito.any()))
            .thenThrow(new DBException("Sign in to DataDam first"));
        DDShareClient client = client("http://127.0.0.1:1", accessService);

        DDShareException error = Assertions.assertThrows(DDShareException.class, client::listProjects);
        Assertions.assertTrue(error.getCause().getMessage().contains("Sign in to DataDam"));
        Mockito.verify(accessService, Mockito.never()).getKeyBundle();
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

    @NotNull
    private DDShareClient client(@NotNull String url, @NotNull DDDesktopAccessService accessService) {
        return new DDShareClient(url, accessService) {
            @NotNull
            @Override
            protected String getUserAgent() {
                return "DataDam Share test";
            }
        };
    }
}
