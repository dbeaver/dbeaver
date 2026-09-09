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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class DDCrypto {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final String KDF_MAC_ALGORITHM = "HmacSHA256";
    private static final String KEK_ALGORITHM = "AES";
    // The gateway key protocol always derives one AES-256 key, which is exactly one
    // HmacSHA256 output block - so this is PBKDF2's single-block case (RFC 8018 5.2, one
    // "T_1"), not a general multi-block PBKDF2. The assertion in deriveKek keeps that
    // fact explicit instead of an implicit consequence of the constant below.
    private static final int KEK_LENGTH_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private DDCrypto() {
    }

    @NotNull
    public static byte[] encrypt(@NotNull SecretKey key, @NotNull byte[] data) throws DBException {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(data);
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return result;
        } catch (GeneralSecurityException e) {
            throw new DBException("Error encrypting data", e);
        }
    }

    @NotNull
    public static byte[] decrypt(@NotNull SecretKey key, @NotNull byte[] input) throws DBException {
        if (input.length <= IV_LENGTH) {
            throw new DBException("Encrypted data is too short");
        }
        try {
            byte[] iv = Arrays.copyOfRange(input, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(input, IV_LENGTH, input.length - IV_LENGTH);
        } catch (GeneralSecurityException e) {
            throw new DBException("Error decrypting data", e);
        }
    }

    /**
     * PBKDF2-HMAC-SHA256, implemented directly over the phrase's UTF-8 bytes rather than
     * through {@code PBEKeySpec} - the JDK does not document how PBEKeySpec's char[] password
     * is turned into bytes, and getting that wrong would silently derive a different key than
     * the browser vault's WebCrypto-based derivation from the same phrase.
     */
    @NotNull
    public static SecretKey deriveKek(@NotNull String phrase, @NotNull byte[] salt, int iterations) throws DBException {
        if (iterations <= 0) {
            throw new DBException("Invalid gateway key iteration count");
        }
        byte[] password = phrase.getBytes(StandardCharsets.UTF_8);
        byte[] initialInput = Arrays.copyOf(salt, salt.length + Integer.BYTES);
        initialInput[initialInput.length - 1] = 1;
        byte[] current = null;
        byte[] result = new byte[KEK_LENGTH_BYTES];
        try {
            Mac mac = Mac.getInstance(KDF_MAC_ALGORITHM);
            mac.init(new SecretKeySpec(password, KDF_MAC_ALGORITHM));
            if (mac.getMacLength() != KEK_LENGTH_BYTES) {
                throw new DBException("Unexpected MAC length for gateway key derivation");
            }
            current = mac.doFinal(initialInput);
            System.arraycopy(current, 0, result, 0, result.length);
            for (int iteration = 1; iteration < iterations; iteration++) {
                byte[] next = mac.doFinal(current);
                Arrays.fill(current, (byte) 0);
                current = next;
                for (int i = 0; i < result.length; i++) {
                    result[i] ^= current[i];
                }
            }
            // SecretKeySpec keeps its own copy of result, so the local array can be wiped right after
            SecretKey kek = new SecretKeySpec(result, KEK_ALGORITHM);
            Arrays.fill(result, (byte) 0);
            return kek;
        } catch (GeneralSecurityException e) {
            throw new DBException("Error deriving key from recovery phrase", e);
        } finally {
            Arrays.fill(password, (byte) 0);
            Arrays.fill(initialInput, (byte) 0);
            Arrays.fill(result, (byte) 0);
            if (current != null) {
                Arrays.fill(current, (byte) 0);
            }
        }
    }
}
