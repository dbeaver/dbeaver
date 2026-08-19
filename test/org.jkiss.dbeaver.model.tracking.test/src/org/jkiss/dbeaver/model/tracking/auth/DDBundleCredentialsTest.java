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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DDBundleCredentialsTest {
    private static final String ACCOUNT_ID = "73ce9dfa-05ad-40f3-802a-bc32e256b737";
    private static final PSSParameterSpec SIGNATURE_PARAMETER_SPEC = new PSSParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA256,
        32,
        PSSParameterSpec.TRAILER_FIELD_BC
    );

    @Test
    void tokenIsVerifiableWithMatchingPublicKey() throws Exception {
        KeyPair signingKeys = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyGenerator dataKeyGenerator = KeyGenerator.getInstance("AES");
        dataKeyGenerator.init(256);
        SecretKey dataKey = dataKeyGenerator.generateKey();
        DDBundleCredentials credentials = new DDBundleCredentials(new DDKeyBundle(
            ACCOUNT_ID,
            Base64.getEncoder().encodeToString(signingKeys.getPrivate().getEncoded()),
            Base64.getEncoder().encodeToString(dataKey.getEncoded()),
            1
        ));

        byte[] body = "request body".getBytes(StandardCharsets.UTF_8);
        String token = credentials.buildToken("put", "/workspace/abc/data?version=1", body);
        String[] parts = token.split("\\.", 4);
        String canonicalRequest = String.join(
            "\n",
            "DD-SIG-v1",
            ACCOUNT_ID,
            "PUT",
            "/workspace/abc/data?version=1",
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body)),
            parts[1],
            "1"
        );
        Signature verifier = Signature.getInstance("RSASSA-PSS");
        verifier.setParameter(SIGNATURE_PARAMETER_SPEC);
        verifier.initVerify(signingKeys.getPublic());
        verifier.update(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        assertEquals(4, parts.length);
        assertEquals(ACCOUNT_ID, parts[0]);
        assertEquals("1", parts[2]);
        assertTrue(verifier.verify(Base64.getUrlDecoder().decode(parts[3])));
        assertArrayEquals(dataKey.getEncoded(), credentials.getDataKey().getEncoded());
    }
}
