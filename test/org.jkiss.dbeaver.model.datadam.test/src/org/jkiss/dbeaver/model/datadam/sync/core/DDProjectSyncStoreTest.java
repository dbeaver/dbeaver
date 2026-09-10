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

import com.dbeaver.datadam.share.api.model.DDPushProjectConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDSharedProjectConfiguration;
import com.dbeaver.datadam.share.api.model.DDSharedProjectFile;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

class DDProjectSyncStoreTest {

    private static final String PROJECT_ID = UUID.randomUUID().toString();

    private DDProjectSyncTransport transport;
    private DDProjectSyncStore store;
    private SecretKey dataKey;

    @BeforeEach
    void setUp() throws Exception {
        transport = Mockito.mock(DDProjectSyncTransport.class);
        DDSyncCredentials credentials = Mockito.mock(DDSyncCredentials.class);
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        dataKey = keyGenerator.generateKey();
        Mockito.when(credentials.getDataKey()).thenReturn(dataKey);
        store = new DDProjectSyncStore(transport, credentials);
    }

    @Test
    void pullFilesDecryptsFileContents() throws Exception {
        byte[] plaintext = "file contents".getBytes(StandardCharsets.UTF_8);
        String encrypted = encryptToBase64("a.json", plaintext);
        Mockito.when(transport.pullProjectConfiguration(PROJECT_ID)).thenReturn(new DDSharedProjectConfiguration(
            "cfg-fingerprint",
            List.of(new DDSharedProjectFile("a.json", encrypted, "file-fingerprint"))));

        DDProjectPullResult result = store.pullFiles(PROJECT_ID);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("cfg-fingerprint", result.configurationFingerprint());
        Assertions.assertArrayEquals(plaintext, result.files().get("a.json"));
    }

    @Test
    void pullFilesRejectsCiphertextRelabeledToAnotherFileName() throws Exception {
        String encrypted = encryptToBase64("a.json", "content".getBytes(StandardCharsets.UTF_8));
        Mockito.when(transport.pullProjectConfiguration(PROJECT_ID)).thenReturn(new DDSharedProjectConfiguration(
            "cfg-fingerprint",
            List.of(new DDSharedProjectFile("b.json", encrypted, "file-fingerprint"))));

        Assertions.assertThrows(DBException.class, () -> store.pullFiles(PROJECT_ID));
    }

    @Test
    void pushFilesSendsEncryptedFilesWithConfigurationFingerprint() throws Exception {
        Mockito.when(transport.pushProjectConfiguration(Mockito.eq(PROJECT_ID), Mockito.any()))
            .thenReturn(null);
        Map<String, byte[]> files = Map.of("a.json", "content-a".getBytes(StandardCharsets.UTF_8));

        store.pushFiles(PROJECT_ID, files, "last-known-fingerprint");

        ArgumentCaptor<DDPushProjectConfigurationRequest> captor =
            ArgumentCaptor.forClass(DDPushProjectConfigurationRequest.class);
        Mockito.verify(transport).pushProjectConfiguration(Mockito.eq(PROJECT_ID), captor.capture());
        DDPushProjectConfigurationRequest request = captor.getValue();

        Assertions.assertEquals("last-known-fingerprint", request.lastKnownConfigurationFingerprint());
        DDSharedProjectFile pushedFile = request.configuration().files().getFirst();
        Assertions.assertEquals("a.json", pushedFile.fileName());
        Assertions.assertNotNull(pushedFile.fingerprint());
        byte[] decrypted = DDCrypto.decrypt(
            dataKey, Base64.getDecoder().decode(pushedFile.encryptedContents()), DDProjectSyncStore.aad(PROJECT_ID, "a.json"));
        Assertions.assertArrayEquals("content-a".getBytes(StandardCharsets.UTF_8), decrypted);
    }

    @Test
    void pushFilesReturnsNullWhenServerReportsStaleFingerprint() throws Exception {
        Mockito.when(transport.pushProjectConfiguration(Mockito.eq(PROJECT_ID), Mockito.any())).thenReturn(null);

        DDSharedProjectRevision result =
            store.pushFiles(PROJECT_ID, Map.of("a.json", "v1".getBytes(StandardCharsets.UTF_8)), "stale-fingerprint");

        Assertions.assertNull(result);
    }

    @NotNull
    private String encryptToBase64(@NotNull String fileName, @NotNull byte[] plaintext) throws Exception {
        byte[] encrypted = DDCrypto.encrypt(dataKey, plaintext, DDProjectSyncStore.aad(PROJECT_ID, fileName));
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
