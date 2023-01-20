/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Here are some cert gen helpers.
 * Originally taken from https://stackoverflow.com/questions/1615871/creating-an-x509-certificate-in-java-without-bouncycastle
 */
public class CertificateGenHelper {

    // FIXME: Not secure at all!!!
    // However some people need to use self-signed and untrusted server.
    // Crap.
    public static final TrustManager[] NON_VALIDATING_TRUST_MANAGERS = new TrustManager[] {
        new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
            public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType) {
            }
        }
    };

    /**
     * Create a self-signed X.509 Certificate
     * @param dn the X.509 Distinguished Name
     * @param pair the KeyPair
     * @param days how many days from now the Certificate is valid for
     * @param algorithm the signing algorithm, eg "SHA1withRSA"
     */
    public static Certificate generateCertificate(String dn, KeyPair pair, int days, String algorithm)
        throws GeneralSecurityException, OperatorCreationException
    {
        Instant from = Instant.now();
        Instant until = from.plus(days, ChronoUnit.DAYS);
        BigInteger sn = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name(dn);

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(owner, sn, Date.from(from), Date.from(until), owner, pair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder(algorithm).build(pair.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);
        cert.verify(pair.getPublic());

        return cert;
    }

    public static Certificate generateCertificate(String dn)
        throws GeneralSecurityException, OperatorCreationException
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return generateCertificate(dn, keyPair, 365, "SHA256withRSA");
    }

    public static void main (String[] argv) throws Exception {
        Certificate certificate = generateCertificate("CN=Test, L=New York, S=New York, C=US");
        System.out.println("Certificate:" + certificate);
    }

}
