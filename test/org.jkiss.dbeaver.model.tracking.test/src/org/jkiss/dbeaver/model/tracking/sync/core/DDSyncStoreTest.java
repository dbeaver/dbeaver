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
package org.jkiss.dbeaver.model.tracking.sync.core;

import com.dbeaver.datadam.gateway.model.DDConfigurationPartKind;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.tracking.auth.DDCrypto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

class DDSyncStoreTest {

    private static final String CONFIGURATION_ID = "cfg-1";

    private DDSyncTransport transport;
    private DDSyncCredentials credentials;
    private DDSyncStore store;
    private SecretKey dataKey;

    @BeforeEach
    void setUp() throws Exception {
        transport = Mockito.mock(DDSyncTransport.class);
        credentials = Mockito.mock(DDSyncCredentials.class);
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        dataKey = keyGenerator.generateKey();
        Mockito.when(credentials.getDataKey()).thenReturn(dataKey);
        store = new DDSyncStore(transport, credentials);
    }

    @Test
    void getConfigurationDecodesPartWithKnownKind() throws Exception {
        com.dbeaver.datadam.gateway.model.DDConfiguration wire = wireConfiguration(DDConfigurationPartKind.ACCOUNT);
        Mockito.when(transport.getConfiguration(CONFIGURATION_ID)).thenReturn(wire);

        DDConfiguration configuration = store.getConfiguration(CONFIGURATION_ID);

        Assertions.assertEquals(DDConfigurationPartKind.ACCOUNT, configuration.parts().get(0).kind());
    }

    @Test
    void getConfigurationRejectsCiphertextRelabeledAsAnotherPart() throws Exception {
        String envelopeJson = "{\"schemaVersion\":1,\"name\":\"test\",\"units\":{}}";
        byte[] encrypted = DDCrypto.encrypt(
            dataKey,
            envelopeJson.getBytes(StandardCharsets.UTF_8),
            DDSyncStore.partAad("k1", DDConfigurationPartKind.ACCOUNT, null));
        String encryptedValue = Base64.getEncoder().encodeToString(encrypted);

        com.dbeaver.datadam.gateway.model.DDConfigurationPart relabeledPart =
            new com.dbeaver.datadam.gateway.model.DDConfigurationPart(
                "k2", DDConfigurationPartKind.PROJECT, "other-project", 1, encryptedValue);
        com.dbeaver.datadam.gateway.model.DDConfiguration wire = new com.dbeaver.datadam.gateway.model.DDConfiguration(
            CONFIGURATION_ID, "test", 0, "2026-01-01T00:00:00Z", null, List.of(relabeledPart));
        Mockito.when(transport.getConfiguration(CONFIGURATION_ID)).thenReturn(wire);

        DBException exception = Assertions.assertThrows(
            DBException.class, () -> store.getConfiguration(CONFIGURATION_ID));
        Assertions.assertTrue(exception.getMessage().contains("Invalid synchronization part"));
    }

    @Test
    void getConfigurationRejectsPartWithUnknownKind() throws Exception {
        com.dbeaver.datadam.gateway.model.DDConfiguration wire = wireConfiguration(null);
        Mockito.when(transport.getConfiguration(CONFIGURATION_ID)).thenReturn(wire);

        DBException exception = Assertions.assertThrows(
            DBException.class, () -> store.getConfiguration(CONFIGURATION_ID));
        Assertions.assertTrue(exception.getMessage().contains("Invalid synchronization part"));
    }

    private com.dbeaver.datadam.gateway.model.DDConfiguration wireConfiguration(
        DDConfigurationPartKind kind
    ) throws Exception {
        String envelopeJson = "{\"schemaVersion\":1,\"name\":\"test\",\"units\":{}}";
        byte[] plaintext = envelopeJson.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = kind == null
            ? DDCrypto.encrypt(dataKey, plaintext)
            : DDCrypto.encrypt(dataKey, plaintext, DDSyncStore.partAad("k1", kind, null));
        String encryptedValue = Base64.getEncoder().encodeToString(encrypted);

        com.dbeaver.datadam.gateway.model.DDConfigurationPart part =
            new com.dbeaver.datadam.gateway.model.DDConfigurationPart("k1", kind, null, 1, encryptedValue);
        return new com.dbeaver.datadam.gateway.model.DDConfiguration(
            CONFIGURATION_ID, "test", 0, "2026-01-01T00:00:00Z", null, List.of(part));
    }
}
