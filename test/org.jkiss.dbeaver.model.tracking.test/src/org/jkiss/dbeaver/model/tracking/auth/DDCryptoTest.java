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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

class DDCryptoTest {

    @Test
    void encryptDecryptRoundTrip() throws Exception {
        SecretKey key = generateKey();
        byte[] plaintext = "encrypted tracking data".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = DDCrypto.encrypt(key, plaintext);

        Assertions.assertArrayEquals(plaintext, DDCrypto.decrypt(key, encrypted));
    }

    @Test
    void decryptRejectsTamperedCiphertext() throws Exception {
        SecretKey key = generateKey();
        byte[] encrypted = DDCrypto.encrypt(key, "tracking data".getBytes(StandardCharsets.UTF_8));
        encrypted[encrypted.length - 1] ^= 1;

        Assertions.assertThrows(DBException.class, () -> DDCrypto.decrypt(key, encrypted));
    }

    @NotNull
    private static SecretKey generateKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }
}
