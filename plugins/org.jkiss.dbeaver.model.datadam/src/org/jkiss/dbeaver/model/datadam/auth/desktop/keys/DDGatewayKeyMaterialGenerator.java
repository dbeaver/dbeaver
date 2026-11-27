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
package org.jkiss.dbeaver.model.datadam.auth.desktop.keys;

import com.dbeaver.datadam.dam.remote.api.model.DDGatewayKeyMaterial;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCrypto;
import org.jkiss.dbeaver.model.datadam.auth.DDEncryptionKey;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKey;

/** Creates independent signing and data keys, then encrypts their bundle using the supplied phrase. */
public class DDGatewayKeyMaterialGenerator {
    private static final int RSA_KEY_BITS = 3072;
    private static final int DATA_KEY_BYTES = 32;
    private static final int SALT_BYTES = 16;
    private static final int KDF_ITERATIONS = 600_000;
    private static final String KDF_ALGORITHM = "PBKDF2-HMAC-SHA256";

    private final SecureRandom random = new SecureRandom();

    @NotNull
    public DDGatewayKeyMaterial generate(@NotNull String phrase) throws DBException {
        String normalizedPhrase = DDEncryptionKey.normalizeAndValidate(phrase);
        byte[] salt = new byte[SALT_BYTES];
        byte[] dataKey = new byte[DATA_KEY_BYTES];
        byte[] privateKey = null;
        byte[] encodedPrivateKey = null;
        byte[] encodedDataKey = null;
        byte[] bundle = null;
        byte[] encryptedBundle = null;
        try {
            random.nextBytes(salt);
            random.nextBytes(dataKey);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(new RSAKeyGenParameterSpec(RSA_KEY_BITS, RSAKeyGenParameterSpec.F4), random);
            KeyPair signingKeys = generator.generateKeyPair();
            privateKey = signingKeys.getPrivate().getEncoded();

            Base64.Encoder base64 = Base64.getEncoder();
            encodedPrivateKey = base64.encode(privateKey);
            encodedDataKey = base64.encode(dataKey);
            bundle = new byte[encodedPrivateKey.length + 1 + encodedDataKey.length];
            System.arraycopy(encodedPrivateKey, 0, bundle, 0, encodedPrivateKey.length);
            bundle[encodedPrivateKey.length] = '.';
            System.arraycopy(encodedDataKey, 0, bundle, encodedPrivateKey.length + 1, encodedDataKey.length);

            SecretKey wrappingKey = DDCrypto.deriveKek(normalizedPhrase, salt, KDF_ITERATIONS);
            encryptedBundle = DDCrypto.encrypt(wrappingKey, bundle);
            return new DDGatewayKeyMaterial(
                base64.encodeToString(signingKeys.getPublic().getEncoded()),
                base64.encodeToString(encryptedBundle),
                base64.encodeToString(salt),
                KDF_ITERATIONS,
                KDF_ALGORITHM
            );
        } catch (GeneralSecurityException e) {
            throw new DBException("Could not generate DataDam encryption keys", e);
        } finally {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(dataKey, (byte) 0);
            if (privateKey != null) {
                Arrays.fill(privateKey, (byte) 0);
            }
            if (encodedPrivateKey != null) {
                Arrays.fill(encodedPrivateKey, (byte) 0);
            }
            if (encodedDataKey != null) {
                Arrays.fill(encodedDataKey, (byte) 0);
            }
            if (bundle != null) {
                Arrays.fill(bundle, (byte) 0);
            }
            if (encryptedBundle != null) {
                Arrays.fill(encryptedBundle, (byte) 0);
            }
        }
    }
}
