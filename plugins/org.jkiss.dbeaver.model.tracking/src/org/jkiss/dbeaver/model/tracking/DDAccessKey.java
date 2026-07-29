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
package org.jkiss.dbeaver.model.tracking;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.PSSParameterSpec;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

public record DDAccessKey(
    @NotNull UUID accountId,
    @NotNull PrivateKey privateKey
) {

    public static final String PREFIX = "ddgk_";

    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "RSASSA-PSS";
    private static final PSSParameterSpec SIGNATURE_PARAMETER_SPEC = new PSSParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA256,
        32,
        PSSParameterSpec.TRAILER_FIELD_BC
    );

    private static final String KEY_TRANSFORMATION = "RSA/ECB/OAEPPadding";

    private static final OAEPParameterSpec OAEP_PARAMETER_SPEC = new OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA256,
        PSource.PSpecified.DEFAULT
    );

    @NotNull
    public static DDAccessKey parse(@NotNull String value) throws DBException {
        if (!value.startsWith(PREFIX)) {
            throw new DBException("Invalid access key format");
        }
        String[] parts = value.substring(PREFIX.length()).split("\\.", 2);
        if (parts.length != 2) {
            throw new DBException("Invalid access key format");
        }
        try {
            PrivateKey privateKey = KeyFactory.getInstance(KEY_ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getUrlDecoder().decode(parts[1])));
            return new DDAccessKey(
                UUID.fromString(parts[0]),
                privateKey
            );
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new DBException("Invalid access key: " + e.getMessage(), e);
        }
    }

    @Nullable
    public static DDAccessKey parseOrNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        try {
            return parse(value);
        } catch (DBException e) {
            return null;
        }
    }

    @NotNull
    public String buildToken() throws DBException {
        String payload = accountId + "." + System.currentTimeMillis();
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.setParameter(SIGNATURE_PARAMETER_SPEC);
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            String signatureValue = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
            return payload + "." + signatureValue;
        } catch (GeneralSecurityException e) {
            throw new DBException("Error signing tracking request", e);
        }
    }

    @NotNull
    public SecretKey decryptDataKey(@NotNull byte[] encryptedDataKey) throws DBException {
        try {
            Cipher cipher = Cipher.getInstance(KEY_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_PARAMETER_SPEC);
            return new SecretKeySpec(cipher.doFinal(encryptedDataKey), "AES");
        } catch (GeneralSecurityException e) {
            throw new DBException("Error decrypting data key", e);
        }
    }
}
