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
import org.jkiss.dbeaver.model.tracking.sync.core.DDSyncCredentials;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.PSSParameterSpec;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Signs requests and encrypts data with the keys of the account bundle.
 */
public class DDBundleCredentials implements DDSyncCredentials {

    private static final String KEY_ALGORITHM = "RSA";
    private static final String DATA_KEY_ALGORITHM = "AES";
    private static final String SIGNATURE_ALGORITHM = "RSASSA-PSS";
    private static final PSSParameterSpec SIGNATURE_PARAMETER_SPEC = new PSSParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA256,
        32,
        PSSParameterSpec.TRAILER_FIELD_BC
    );

    private final DDKeyBundle bundle;

    public DDBundleCredentials(@NotNull DDKeyBundle bundle) {
        this.bundle = bundle;
    }

    @NotNull
    @Override
    public String buildToken() throws DBException {
        String payload = bundle.accountId() + "." + System.currentTimeMillis();
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.setParameter(SIGNATURE_PARAMETER_SPEC);
            signature.initSign(signingKey());
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return payload + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new DBException("Error signing request", e);
        }
    }

    @NotNull
    @Override
    public SecretKey getDataKey() throws DBException {
        try {
            return new SecretKeySpec(Base64.getDecoder().decode(bundle.dataKey()), DATA_KEY_ALGORITHM);
        } catch (IllegalArgumentException e) {
            throw new DBException("Invalid data key in the bundle", e);
        }
    }

    @NotNull
    private PrivateKey signingKey() throws DBException {
        try {
            return KeyFactory.getInstance(KEY_ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(bundle.signingKey())));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new DBException("Invalid signing key in the bundle", e);
        }
    }
}
