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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

class DDKeyStoreTest {
    private static final String ACCOUNT_ID = "8decb064-2709-4914-b6b6-68eaef98cac3";
    // canonical BIP-39 test vector (all-zero entropy) - valid checksum, used across the test suite
    private static final String RECOVERY_PHRASE =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
    private static final int KDF_ITERATIONS = 1000;

    @Test
    void deriveKekMatchesWebCryptoVector() throws Exception {
        // cross-checked against the browser vault's PBKDF2 (Web Crypto), independently of this
        // codebase's own round-trip tests - catches a future regression that breaks interop with
        // the browser while staying internally self-consistent.
        byte[] salt = new byte[16];
        for (int i = 0; i < salt.length; i++) {
            salt[i] = (byte) i;
        }

        SecretKey kek = DDCrypto.deriveKek(RECOVERY_PHRASE, salt, 600_000);

        Assertions.assertEquals(
            "726f8606c64cafa3be0ca9659c211b6124b4b469d03a3bc86a9e0b00159dedc3",
            HexFormat.of().formatHex(kek.getEncoded())
        );
    }

    @Test
    void unpackDecryptsValidBundle() throws Exception {
        BundleFixture fixture = createFixture(7);

        DDKeyBundle bundle = DDKeyStore.unpack(fixture.state(), RECOVERY_PHRASE);

        Assertions.assertEquals(ACCOUNT_ID, bundle.accountId());
        Assertions.assertEquals(fixture.signingKey(), bundle.signingKey());
        Assertions.assertEquals(fixture.dataKey(), bundle.dataKey());
        Assertions.assertEquals(7, bundle.generation());
    }

    @Test
    void unpackRejectsInvalidPhrase() throws Exception {
        // rejected by DDRecoveryPhrase's checksum check inside unpack() itself, not by a
        // failed decrypt - see DDRecoveryPhraseTest for validation-specific coverage
        BundleFixture fixture = createFixture(1);
        String invalidPhrase =
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon";

        Assertions.assertThrows(DBException.class, () -> DDKeyStore.unpack(fixture.state(), invalidPhrase));
    }

    @Test
    void unpackRejectsTruncatedCiphertext() throws Exception {
        BundleFixture fixture = createFixture(1);
        byte[] encrypted = Base64.getDecoder().decode(fixture.state().encryptedBundle());
        String truncated = Base64.getEncoder().encodeToString(Arrays.copyOf(encrypted, 12));
        DDCryptoState state = new DDCryptoState(
            ACCOUNT_ID, true, truncated, 1L, fixture.state().salt(), KDF_ITERATIONS);

        Assertions.assertThrows(DBException.class, () -> DDKeyStore.unpack(state, RECOVERY_PHRASE));
    }

    @Test
    void unpackRejectsBundleWithoutSeparator() throws Exception {
        byte[] salt = randomSalt();
        SecretKey kek = DDCrypto.deriveKek(RECOVERY_PHRASE, salt, KDF_ITERATIONS);
        byte[] encrypted = DDCrypto.encrypt(kek, "incomplete".getBytes(StandardCharsets.UTF_8));
        DDCryptoState state = new DDCryptoState(
            ACCOUNT_ID,
            true,
            Base64.getEncoder().encodeToString(encrypted),
            1L,
            Base64.getEncoder().encodeToString(salt),
            KDF_ITERATIONS
        );

        Assertions.assertThrows(DBException.class, () -> DDKeyStore.unpack(state, RECOVERY_PHRASE));
    }

    @Test
    void saveRejectsBundleFromDifferentAccount() throws Exception {
        DDKeyBundle stored = new DDKeyBundle(ACCOUNT_ID, "signing-key", "data-key", 2);
        DDKeyBundle replacement = new DDKeyBundle("another-account", "new-signing-key", "new-data-key", 3);

        DBException exception = assertRejectedSave(stored, replacement);

        Assertions.assertEquals("The keys belong to another account", exception.getMessage());
    }

    @Test
    void saveRejectsGenerationRollback() throws Exception {
        DDKeyBundle stored = new DDKeyBundle(ACCOUNT_ID, "signing-key", "data-key", 3);
        DDKeyBundle replacement = new DDKeyBundle(ACCOUNT_ID, "old-signing-key", "old-data-key", 2);

        DBException exception = assertRejectedSave(stored, replacement);

        Assertions.assertEquals("The keys are older than the stored ones", exception.getMessage());
    }

    @NotNull
    private static DBException assertRejectedSave(@NotNull DDKeyBundle stored, @NotNull DDKeyBundle replacement) throws Exception {
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        Mockito.when(controller.getPrivateSecretValue(ArgumentMatchers.anyString()))
            .thenReturn(JSONUtils.GSON.toJson(stored));
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(DBSSecretController.class)) {
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);

            DBException exception = Assertions.assertThrows(DBException.class, () -> DDKeyStore.save(replacement));

            Mockito.verify(controller, Mockito.never())
                .setPrivateSecretValue(ArgumentMatchers.anyString(), ArgumentMatchers.any());
            return exception;
        }
    }

    @NotNull
    private static BundleFixture createFixture(long generation) throws Exception {
        byte[] salt = randomSalt();
        SecretKey kek = DDCrypto.deriveKek(RECOVERY_PHRASE, salt, KDF_ITERATIONS);
        String signingKey = Base64.getEncoder().encodeToString(
            KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate().getEncoded()
        );
        String dataKey = Base64.getEncoder().encodeToString(generateAesKey().getEncoded());
        byte[] encrypted = DDCrypto.encrypt(
            kek,
            (signingKey + "." + dataKey).getBytes(StandardCharsets.UTF_8)
        );
        return new BundleFixture(
            new DDCryptoState(
                ACCOUNT_ID,
                true,
                Base64.getEncoder().encodeToString(encrypted),
                generation,
                Base64.getEncoder().encodeToString(salt),
                KDF_ITERATIONS
            ),
            signingKey,
            dataKey
        );
    }

    @NotNull
    private static byte[] randomSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    @NotNull
    private static SecretKey generateAesKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    private record BundleFixture(
        @NotNull DDCryptoState state,
        @NotNull String signingKey,
        @NotNull String dataKey
    ) {
    }
}
