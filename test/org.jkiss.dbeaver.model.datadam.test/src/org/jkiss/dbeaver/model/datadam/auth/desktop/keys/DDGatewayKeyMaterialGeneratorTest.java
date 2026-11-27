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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.auth.DDCryptoState;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

class DDGatewayKeyMaterialGeneratorTest {
    private static final String PHRASE = "abandon ".repeat(11) + "about";

    @Test
    void producesAKeyBundleCompatibleWithTheExistingDesktopImporter() throws Exception {
        DDGatewayKeyMaterial material = new DDGatewayKeyMaterialGenerator().generate(PHRASE);
        Base64.Decoder base64 = Base64.getDecoder();
        byte[] salt = base64.decode(material.salt());
        byte[] ciphertext = base64.decode(material.encryptedBundle());
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(base64.decode(material.publicKey())));

        Assertions.assertEquals(16, salt.length);
        Assertions.assertTrue(ciphertext.length > 12 + 16);
        Assertions.assertEquals(600_000, material.iterations());
        Assertions.assertEquals("PBKDF2-HMAC-SHA256", material.kdfAlgorithm());
        Assertions.assertEquals(3072, publicKey.getModulus().bitLength());
        Assertions.assertEquals(RSAKeyGenParameterSpec.F4, publicKey.getPublicExponent());

        UUID accountId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        DDKeyBundle unpacked = DDKeyStore.unpack(new DDCryptoState(accountId, true, material.encryptedBundle(),
            1L, material.salt(), material.iterations(), keyId), PHRASE);
        RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
            .generatePrivate(new PKCS8EncodedKeySpec(base64.decode(unpacked.signingKey())));

        Assertions.assertEquals(accountId, unpacked.accountId());
        Assertions.assertEquals(keyId, unpacked.apiKeyId());
        Assertions.assertEquals(publicKey.getModulus(), privateKey.getModulus());
        Assertions.assertEquals(32, base64.decode(unpacked.dataKey()).length);
    }

    @Test
    void rejectsAnInvalidRecoveryPhrase() {
        Assertions.assertThrows(DBException.class, () -> new DDGatewayKeyMaterialGenerator().generate("invalid phrase"));
    }
}
