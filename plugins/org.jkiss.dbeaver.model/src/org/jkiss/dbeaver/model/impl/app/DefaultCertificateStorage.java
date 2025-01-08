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

import org.bouncycastle.util.io.pem.PemReader;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBACertificateStorage;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.Base64;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DefaultCertificateStorage
 */
public class DefaultCertificateStorage implements DBACertificateStorage {

    private static final Log log = Log.getLog(DefaultCertificateStorage.class);
    private static final char[] DEFAULT_PASSWORD = "".toCharArray();
    public static final String JKS_EXTENSION = ".jks";

    public static final String CA_CERT_ALIAS = "ca-cert";
    public static final String CLIENT_CERT_ALIAS = "client-cert";
    public static final String KEY_CERT_ALIAS = "key-cert";

    private final Path localPath;
    private final Map<String, UserDefinedKeystore> userDefinedKeystores;

    public DefaultCertificateStorage(Path localPath) {
        this.localPath = localPath;
        this.userDefinedKeystores = new HashMap<>();
        if (Files.exists(localPath)) {
            // Cleanup old keystores
            // We do not cleanup key stores in non-primary instances
            // Because they may be used by another instances of the application (pro/#2998)
            if (DBWorkbench.getPlatform().getApplication().isPrimaryInstance()) {
                final File[] ksFiles = localPath.toFile().listFiles();
                if (ksFiles != null) {
                    for (File ksFile : ksFiles) {
                        if (!ksFile.delete()) {
                            log.warn("Can't delete old keystore '" + ksFile.getAbsolutePath() + "'");
                        }
                    }
                }
            }
        }
    }

    @Override
    public KeyStore getKeyStore(DBPDataSourceContainer container, String certType) throws DBException {
        try {
            Path ksFile = getKeyStorePath(container, certType);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            if (Files.exists(ksFile)) {
                try (InputStream is = Files.newInputStream(ksFile)) {
                    ks.load(is, getKeyStorePassword(container, certType));
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

    @NotNull
    @Override
    public Path getStorageFolder() {
        return this.localPath;
    }

    private void saveKeyStore(DBPDataSourceContainer container, String certType, KeyStore keyStore) throws Exception {
        checkConfigFolderExists();
        final Path ksFile = getKeyStorePath(container, certType);

        try (OutputStream os = Files.newOutputStream(ksFile)) {
            keyStore.store(os, DEFAULT_PASSWORD);
        }
    }

    private void checkConfigFolderExists() {
        if (!Files.exists(localPath)) {
           try {
               Files.createDirectories(localPath);
           } catch (IOException e) {
               log.error("Can't create directory for security manager: " + localPath, e);
           }
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
    public void addCertificate(@NotNull DBPDataSourceContainer dataSource, @NotNull String certType, byte[] caCertData, byte[] clientCertData, byte[] keyData) throws DBException {
        if (userDefinedKeystores.containsKey(getKeyStoreName(dataSource, certType))) {
            throw new DBException("Adding new certificates would override user-specified keystore");
        }
        final KeyStore keyStore = getKeyStore(dataSource, certType);
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            List<Certificate> certChain = new ArrayList<>();
            if (caCertData != null) {
                List<? extends Certificate> certificates =
                    new ArrayList<>(cf.generateCertificates(new ByteArrayInputStream(caCertData)));
                for (int i = 0; i < certificates.size(); i++) {
                    keyStore.setCertificateEntry(i == 0 ? CA_CERT_ALIAS : CA_CERT_ALIAS + i, certificates.get(i));
                }
                //certChain.add(caCert);
            }
            if (clientCertData != null) {
                List<? extends Certificate> certificates
                    = new ArrayList<>(cf.generateCertificates(new ByteArrayInputStream(clientCertData)));
                for (int i = 0; i < certificates.size(); i++) {
                    keyStore.setCertificateEntry(i == 0 ? CLIENT_CERT_ALIAS : CLIENT_CERT_ALIAS + i,
                        certificates.get(i));
                    certChain.add(certificates.get(i));
                }
            }
            if (keyData != null) {
                PrivateKey privateKey = loadPrivateKeyFromPEM(keyData);
                keyStore.setKeyEntry(KEY_CERT_ALIAS, privateKey, DEFAULT_PASSWORD, certChain.toArray(new Certificate[certChain.size()]));
            }

            saveKeyStore(dataSource, certType, keyStore);
        } catch (Throwable e) {
            throw new DBException("Error adding certificate to keystore", e);
        }

    }

    @Override
    public void addCertificate(@NotNull DBPDataSourceContainer dataSource, @NotNull String certType, @NotNull String keyStorePath, @NotNull char[] keyStorePassword) throws DBException {
        userDefinedKeystores.put(
            getKeyStoreName(dataSource, certType),
            new UserDefinedKeystore(new File(keyStorePath), keyStorePassword)
        );
    }

    @Override
    public void addCertificate(@NotNull DBPDataSourceContainer dataSource, @NotNull String certType, @NotNull byte[] keyStoreData, @NotNull char[] keyStorePassword) throws DBException {
        checkConfigFolderExists();
        final Path keyStorePath = getKeyStorePath(dataSource, certType);
        if (!Files.exists(keyStorePath)) {
            try {
                Files.write(keyStorePath, keyStoreData);
            } catch (Throwable e) {
                throw new DBException("Error adding certificate to keystore", e);
            }
        }
        userDefinedKeystores.put(getKeyStoreName(dataSource, certType), new UserDefinedKeystore(keyStorePath.toFile(), keyStorePassword));
    }

    @Override
    public void addSelfSignedCertificate(@NotNull DBPDataSourceContainer dataSource, @NotNull String certType, @NotNull String certDN) throws DBException {
        if (userDefinedKeystores.containsKey(getKeyStoreName(dataSource, certType))) {
            throw new DBException("Adding new certificates would override user-specified keystore");
        }
        final KeyStore keyStore = getKeyStore(dataSource, certType);
        try {
            List<Certificate> certChain = new ArrayList<>();

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            Certificate clientCert = CertificateGenHelper.generateCertificate(certDN, keyPair, 365, "SHA256withRSA");

            keyStore.setCertificateEntry(CLIENT_CERT_ALIAS, clientCert);
            certChain.add(clientCert);

            PrivateKey privateKey = keyPair.getPrivate();
            keyStore.setKeyEntry(KEY_CERT_ALIAS, privateKey, DEFAULT_PASSWORD, certChain.toArray(new Certificate[certChain.size()]));

            saveKeyStore(dataSource, certType, keyStore);
        } catch (Throwable e) {
            throw new DBException("Error adding self-signed certificate to keystore", e);
        }
    }

    @Override
    public void deleteCertificate(@NotNull DBPDataSourceContainer dataSource, @NotNull String certType) throws DBException {
        if (userDefinedKeystores.remove(getKeyStoreName(dataSource, certType)) != null) {
            // We don't want to erase anything from user-defined keystore
            return;
        }
        final KeyStore keyStore = getKeyStore(dataSource, certType);
        try {
            keyStore.deleteEntry(CA_CERT_ALIAS);
            keyStore.deleteEntry(CLIENT_CERT_ALIAS);
            keyStore.deleteEntry(KEY_CERT_ALIAS);
            saveKeyStore(dataSource, certType, keyStore);
        } catch (Exception e) {
            throw new DBException("Error deleting certificate from keystore", e);
        }
    }

    @Override
    public Path getKeyStorePath(DBPDataSourceContainer dataSource, String certType) {
        final UserDefinedKeystore userDefinedKeystore = getUserDefinedKeystore(dataSource, certType);
        if (userDefinedKeystore != null) {
            return userDefinedKeystore.file.toPath();
        } else {
            return localPath.resolve(getKeyStoreName(dataSource, certType) + JKS_EXTENSION);
        }
    }

    @NotNull
    @Override
    public char[] getKeyStorePassword(@NotNull DBPDataSourceContainer dataSource, @NotNull String certType) {
        final UserDefinedKeystore userDefinedKeystore = getUserDefinedKeystore(dataSource, certType);
        if (userDefinedKeystore != null) {
            return userDefinedKeystore.password;
        } else {
            return DEFAULT_PASSWORD;
        }
    }

    @Override
    public String getKeyStoreType(DBPDataSourceContainer dataSource) {
        return KeyStore.getDefaultType();
    }

    @NotNull
    public static byte[] loadDerFromPem(@NotNull Reader reader) throws IOException {
        return new PemReader(reader).readPemObject().getContent();
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

    @Nullable
    private UserDefinedKeystore getUserDefinedKeystore(@NotNull DBPDataSourceContainer dataSource, @NotNull String certType) {
        return userDefinedKeystores.get(getKeyStoreName(dataSource, certType));
    }

    @NotNull
    private String getKeyStoreName(@NotNull DBPDataSourceContainer dataSource, @NotNull String certType) {
        return dataSource.getId() + '-' + certType;
    }

    private static class UserDefinedKeystore {
        private final File file;
        private final char[] password;

        public UserDefinedKeystore(@NotNull File file, @NotNull char[] password) {
            this.file = file;
            this.password = password;
        }
    }
}