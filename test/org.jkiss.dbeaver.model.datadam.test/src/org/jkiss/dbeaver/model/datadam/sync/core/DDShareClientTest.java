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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
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

    @NotNull
    private static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }
}
