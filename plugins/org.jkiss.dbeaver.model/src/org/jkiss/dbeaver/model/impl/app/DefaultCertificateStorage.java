/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBACertificateStorage;
import org.jkiss.utils.Base64;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

/**
 * DefaultCertificateStorage
 */
public class DefaultCertificateStorage implements DBACertificateStorage {

    private static final Log log = Log.getLog(DefaultCertificateStorage.class);
    private static final char[] DEFAULT_PASSWORD = "".toCharArray();
    public static final String JKS_EXTENSION = ".jks";

    private final File localPath;

    public DefaultCertificateStorage(File localPath) {
        this.localPath = localPath;
        if (localPath.exists()) {
            // Cleanup old keystores
            final File[] ksFiles = localPath.listFiles();
            if (ksFiles != null) {
                for (File ksFile : ksFiles) {
                    if (!ksFile.delete()) {
                        log.warn("Can't delete old keystore '" + ksFile.getAbsolutePath() + "'");
                    }
                }
            }
        } else if (!localPath.mkdirs()) {
            log.error("Can't create directory for security manager: " + localPath.getAbsolutePath());
        }
    }

    @Override
    public KeyStore getKeyStore(DBPDataSourceContainer container, String certType) throws DBException {
        try {
            File ksFile = getKeyStorePath(container, certType);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            if (ksFile.exists()) {
                try (InputStream is = new FileInputStream(ksFile)) {
                    ks.load(is, DEFAULT_PASSWORD);
                }
            } else {
                ks.load(null, DEFAULT_PASSWORD);
                saveKeyStore(container, certType, ks);
            }

            return ks;
        } catch (Exception e) {
            throw new DBException("Error opening keystore", e);
        }
    }

    private void saveKeyStore(DBPDataSourceContainer container, String certType, KeyStore keyStore) throws Exception {
        final File ksFile = getKeyStorePath(container, certType);

        try (OutputStream os = new FileOutputStream(ksFile)) {
            keyStore.store(os, DEFAULT_PASSWORD);
        }
    }

    public static byte[] readEncryptedString(InputStream stream) throws IOException {
        try (Reader reader = new InputStreamReader(stream)) {
            return readEncryptedString(reader);
        }
    }

    public static byte[] readEncryptedString(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder(4000);
        try (BufferedReader br = new BufferedReader(reader)) {
            for (; ; ) {
                final String line = br.readLine();
                if (line == null || line.isEmpty()) break;
                if (line.startsWith("-") || line.startsWith("#")) continue;
                result.append(line);
            }
        }
        return Base64.decode(result.toString());
    }

    @Override
    public void addCertificate(DBPDataSourceContainer dataSource, String certType, byte[] caCertData, byte[] clientCertData, byte[] keyData) throws DBException {
        final KeyStore keyStore = getKeyStore(dataSource, certType);
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            List<Certificate> certChain = new ArrayList<>();
            if (caCertData != null) {
                Certificate caCert = cf.generateCertificate(new ByteArrayInputStream(caCertData));
                keyStore.setCertificateEntry("ca-cert", caCert);
                //certChain.add(caCert);
            }
            if (clientCertData != null) {
                Certificate clientCert = cf.generateCertificate(new ByteArrayInputStream(clientCertData));
                keyStore.setCertificateEntry("client-cert", clientCert);
                certChain.add(clientCert);
            }
            if (keyData != null) {
                PrivateKey privateKey = loadPrivateKeyFromPEM(keyData);
               keyStore.setKeyEntry("key-cert", privateKey, DEFAULT_PASSWORD, certChain.toArray(new Certificate[certChain.size()]));
            }

            saveKeyStore(dataSource, certType, keyStore);
        } catch (Throwable e) {
            throw new DBException("Error adding certificate to keystore", e);
        }

    }

    @Override
    public void deleteCertificate(DBPDataSourceContainer dataSource, String certType) throws DBException {
        final KeyStore keyStore = getKeyStore(dataSource, certType);
        try {
            keyStore.deleteEntry("ca-cert");
            keyStore.deleteEntry("client-cert");
            keyStore.deleteEntry("key-cert");
            saveKeyStore(dataSource, certType, keyStore);
        } catch (Exception e) {
            throw new DBException("Error deleting certificate from keystore", e);
        }
    }

    @Override
    public File getKeyStorePath(DBPDataSourceContainer dataSource, String certType) {
        return new File(localPath, dataSource.getId() + "-" + certType + JKS_EXTENSION);
    }

    @Override
    public String getKeyStoreType(DBPDataSourceContainer dataSource) {
        return KeyStore.getDefaultType();
    }

    /**
     * That's tricky.
     * Algorithm got from http://stackoverflow.com/questions/7216969/getting-rsa-private-key-from-pem-base64-encoded-private-key-file
     * Different SSL providers use different pk format. Google cloud uses PKCS1. See #740
     */
    public static PrivateKey loadPrivateKeyFromPEM(byte[] keyData) throws GeneralSecurityException, IOException {
        // PKCS#8 format
        final String PEM_PRIVATE_START = "-----BEGIN PRIVATE KEY-----";
        final String PEM_PRIVATE_END = "-----END PRIVATE KEY-----";

        // PKCS#1 format
        final String PEM_RSA_PRIVATE_START = "-----BEGIN RSA PRIVATE KEY-----";
        final String PEM_RSA_PRIVATE_END = "-----END RSA PRIVATE KEY-----";


        String privateKeyPem = new String(keyData);

        if (privateKeyPem.contains(PEM_PRIVATE_START)) { // PKCS#8 format
            privateKeyPem = privateKeyPem.replace(PEM_PRIVATE_START, "").replace(PEM_PRIVATE_END, "");
            privateKeyPem = privateKeyPem.replaceAll("\\s", "");

            byte[] pkcs8EncodedKey = Base64.decode(privateKeyPem);

            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8EncodedKey));

        } else if (privateKeyPem.contains(PEM_RSA_PRIVATE_START)) {  // PKCS#1 format

            privateKeyPem = privateKeyPem.replace(PEM_RSA_PRIVATE_START, "").replace(PEM_RSA_PRIVATE_END, "");
            privateKeyPem = privateKeyPem.replaceAll("\\s", "");
            return PKCS1Util.loadPrivateKeyFromPKCS1(privateKeyPem);
        } else {
            throw new GeneralSecurityException("Not supported format of a private key");
        }
    }

}