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
package org.jkiss.dbeaver.model.tracking.auth;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DDKeyStoreTest {
    private static final String ACCOUNT_ID = "8decb064-2709-4914-b6b6-68eaef98cac3";

    @Test
    void unpackDecryptsValidBundle() throws Exception {
        BundleFixture fixture = createFixture(7);

        DDKeyBundle bundle = DDKeyStore.unpack(fixture.state(), fixture.accessKey());

        assertEquals(ACCOUNT_ID, bundle.accountId());
        assertEquals(fixture.signingKey(), bundle.signingKey());
        assertEquals(fixture.dataKey(), bundle.dataKey());
        assertEquals(7, bundle.generation());
    }

    @Test
    void unpackRejectsWrongAccessKey() throws Exception {
        BundleFixture fixture = createFixture(1);
        String wrongAccessKey = Base64.getUrlEncoder().withoutPadding().encodeToString(generateAesKey().getEncoded());

        assertThrows(DBException.class, () -> DDKeyStore.unpack(fixture.state(), wrongAccessKey));
    }

    @Test
    void unpackRejectsTruncatedCiphertext() throws Exception {
        BundleFixture fixture = createFixture(1);
        byte[] encrypted = Base64.getDecoder().decode(fixture.state().encryptedBundle());
        String truncated = Base64.getEncoder().encodeToString(Arrays.copyOf(encrypted, 12));
        DDCryptoState state = new DDCryptoState(ACCOUNT_ID, true, truncated, 1L);

        assertThrows(DBException.class, () -> DDKeyStore.unpack(state, fixture.accessKey()));
    }

    @Test
    void unpackRejectsBundleWithoutSeparator() throws Exception {
        SecretKey accessKey = generateAesKey();
        byte[] encrypted = DDCrypto.encrypt(accessKey, "incomplete".getBytes(StandardCharsets.UTF_8));
        DDCryptoState state = new DDCryptoState(
            ACCOUNT_ID,
            true,
            Base64.getEncoder().encodeToString(encrypted),
            1L
        );

        assertThrows(DBException.class, () -> DDKeyStore.unpack(state, accessKeyValue(accessKey)));
    }

    @Test
    void saveRejectsBundleFromDifferentAccount() throws Exception {
        DDKeyBundle stored = new DDKeyBundle(ACCOUNT_ID, "signing-key", "data-key", 2);
        DDKeyBundle replacement = new DDKeyBundle("another-account", "new-signing-key", "new-data-key", 3);

        DBException exception = assertRejectedSave(stored, replacement);

        assertEquals("The keys belong to another account", exception.getMessage());
    }

    @Test
    void saveRejectsGenerationRollback() throws Exception {
        DDKeyBundle stored = new DDKeyBundle(ACCOUNT_ID, "signing-key", "data-key", 3);
        DDKeyBundle replacement = new DDKeyBundle(ACCOUNT_ID, "old-signing-key", "old-data-key", 2);

        DBException exception = assertRejectedSave(stored, replacement);

        assertEquals("The keys are older than the stored ones", exception.getMessage());
    }

    private static DBException assertRejectedSave(DDKeyBundle stored, DDKeyBundle replacement) throws Exception {
        DBSSecretController controller = mock(DBSSecretController.class);
        when(controller.getPrivateSecretValue(anyString())).thenReturn(JSONUtils.GSON.toJson(stored));
        try (MockedStatic<DBSSecretController> controllers = mockStatic(DBSSecretController.class)) {
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);

            DBException exception = assertThrows(DBException.class, () -> DDKeyStore.save(replacement));

            verify(controller, never()).setPrivateSecretValue(anyString(), any());
            return exception;
        }
    }

    private static BundleFixture createFixture(long generation) throws Exception {
        SecretKey accessKey = generateAesKey();
        String signingKey = Base64.getEncoder().encodeToString(
            KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate().getEncoded()
        );
        String dataKey = Base64.getEncoder().encodeToString(generateAesKey().getEncoded());
        byte[] encrypted = DDCrypto.encrypt(
            accessKey,
            (signingKey + "." + dataKey).getBytes(StandardCharsets.UTF_8)
        );
        return new BundleFixture(
            new DDCryptoState(ACCOUNT_ID, true, Base64.getEncoder().encodeToString(encrypted), generation),
            accessKeyValue(accessKey),
            signingKey,
            dataKey
        );
    }

    private static SecretKey generateAesKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    private static String accessKeyValue(SecretKey accessKey) {
        return DDKeyStore.ACCESS_KEY_PREFIX + ACCOUNT_ID + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(accessKey.getEncoded());
    }

    private record BundleFixture(
        DDCryptoState state,
        String accessKey,
        String signingKey,
        String dataKey
    ) {
    }
}
