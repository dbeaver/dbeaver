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
import com.dbeaver.datadam.share.api.model.DDSharedProjectFile;
import com.dbeaver.datadam.share.api.utils.DDFingerprintUtils;
import com.google.gson.JsonObject;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

class DDShareClientTest {

    private static final String PROJECT_A = UUID.randomUUID().toString();
    private static final String PROJECT_B = UUID.randomUUID().toString();

    @Test
    void decryptRoundTripsWithMatchingProjectIdAndField() throws Exception {
        SecretKey key = generateKey();
        byte[] plaintext = "content".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = DDCrypto.encrypt(key, plaintext, DDShareClient.aad(PROJECT_A, "name"));

        byte[] decrypted = DDCrypto.decrypt(key, encrypted, DDShareClient.aad(PROJECT_A, "name"));

        Assertions.assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void decryptRejectsCiphertextRelabeledToAnotherProject() throws Exception {
        SecretKey key = generateKey();
        byte[] encrypted = DDCrypto.encrypt(
            key, "content".getBytes(StandardCharsets.UTF_8), DDShareClient.aad(PROJECT_A, "name"));

        Assertions.assertThrows(
            DBException.class, () -> DDCrypto.decrypt(key, encrypted, DDShareClient.aad(PROJECT_B, "name")));
    }

    @Test
    void decryptRejectsCiphertextRelabeledToAnotherField() throws Exception {
        SecretKey key = generateKey();
        byte[] encrypted = DDCrypto.encrypt(
            key, "content".getBytes(StandardCharsets.UTF_8), DDShareClient.aad(PROJECT_A, "name"));

        Assertions.assertThrows(
            DBException.class,
            () -> DDCrypto.decrypt(key, encrypted, DDShareClient.aad(PROJECT_A, "description")));
    }

    @Test
    void verifiesPlaintextAndConfigurationFingerprintsBeforeReturningFiles() throws Exception {
        UUID projectId = UUID.randomUUID();
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        SecretKey key = generator.generateKey();
        DDSyncCredentials credentials = Mockito.mock(DDSyncCredentials.class);
        Mockito.when(credentials.getDataKey()).thenReturn(key);
        byte[] plaintext = new byte[] { '{', '}' };
        String name = "data-sources.json";
        String ciphertext = Base64.getEncoder().encodeToString(
            DDCrypto.encrypt(key, plaintext, DDShareClient.aad(projectId.toString(), name)));
        DDSharedProjectFile file = new DDSharedProjectFile(name, ciphertext,
            DDFingerprintUtils.calculateFileFingerprint(projectId, name, plaintext));
        String checksum = DDFingerprintUtils.calculateConfigurationFingerprint(projectId, List.of(file));
        Client client = new Client(credentials);
        client.configuration(checksum, file);
        Assertions.assertArrayEquals(plaintext, client.pullFiles(projectId).files().get(name));
        client.configuration("incorrect", file);
        Assertions.assertThrows(DDShareException.class, () -> client.pullFiles(projectId));
        client.configuration(checksum, new DDSharedProjectFile(name, ciphertext,
            DDFingerprintUtils.calculateFileFingerprint(projectId, name, new byte[0])));
        Assertions.assertThrows(DDShareException.class, () -> client.pullFiles(projectId));
        client.configuration(checksum, new DDSharedProjectFile("renamed.json", ciphertext, file.fingerprint()));
        Assertions.assertThrows(DDShareException.class, () -> client.pullFiles(projectId));
    }

    @Test
    void statisticsAndWorkspaceTimestampsRoundTripWithOffsets() throws Exception {
        Client client = new Client(Mockito.mock(DDSyncCredentials.class));
        UUID id = UUID.randomUUID();
        OffsetDateTime time = OffsetDateTime.parse("2026-09-15T12:34:56+02:00");
        client.response = """
            {"data":{"getProjectStatistics":[{"id":"%s","userId":"%s","time":"%s","type":"ProjectPull",
                "ipAddress":"127.0.0.1","configurationFingerprint":"checksum"}]}}
            """.formatted(id, id, time);
        Assertions.assertEquals(time, client.getProjectStatistics(id, time, time, 0, 1).getFirst().time());
        client.response = """
            {"data":{"listWorkspaces":[{"id":"%s","name":"workspace","encryptedKey":"key",
                "createTime":"%s","updateTime":"%s"}]}}
            """.formatted(id, time, time);
        Assertions.assertEquals(time, client.listWorkspaces().getFirst().updateTime());
    }

    @NotNull
    private static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    private static class Client extends DDShareClient {
        private String response;

        private Client(@NotNull DDSyncCredentials credentials) {
            super("https://example.invalid", credentials);
        }

        private void configuration(@NotNull String checksum, @NotNull DDSharedProjectFile file) {
            response = gson.toJson(Map.of("data", Map.of("pullProjectConfiguration",
                Map.of("configurationFingerprint", checksum, "files", List.of(file)))));
        }

        @NotNull
        @Override
        protected <T> T execute(@NotNull HttpRequest.Builder builder, @NotNull Type type) {
            Assertions.assertEquals(JsonObject.class, type);
            return gson.fromJson(response, type);
        }
    }
}
