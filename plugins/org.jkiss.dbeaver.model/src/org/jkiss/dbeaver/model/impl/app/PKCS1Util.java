/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.app;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.jkiss.code.NotNull;
import org.jkiss.utils.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;

class PKCS1Util {
    private PKCS1Util() {
        // avoid instantiation of utility class
    }

    @NotNull
    public static PrivateKey loadPrivateKeyFromPKCS1(@NotNull String privateKeyPem) throws GeneralSecurityException, IOException {
        try (ASN1InputStream is = new ASN1InputStream(Base64.decode(privateKeyPem))) {
            final ASN1Sequence seq = (ASN1Sequence) is.readObject();
            if (seq.size() < 9) {
                throw new GeneralSecurityException("Could not parse a PKCS1 private key.");
            }
            return KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateCrtKeySpec(
                // skip version at seq.getObjectAt(0);
                getInteger(seq.getObjectAt(1)),
                getInteger(seq.getObjectAt(2)),
                getInteger(seq.getObjectAt(3)),
                getInteger(seq.getObjectAt(4)),
                getInteger(seq.getObjectAt(5)),
                getInteger(seq.getObjectAt(6)),
                getInteger(seq.getObjectAt(7)),
                getInteger(seq.getObjectAt(8))
            ));
        }
    }

    @NotNull
    private static BigInteger getInteger(@NotNull ASN1Encodable encodable) {
        return ((ASN1Integer) encodable).getValue();
    }
}