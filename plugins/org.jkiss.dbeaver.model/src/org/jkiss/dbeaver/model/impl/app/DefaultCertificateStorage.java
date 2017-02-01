/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.app.DBACertificateStorage;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

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
    public KeyStore getKeyStore(String ksId) throws DBException {
        try {
            File ksFile = getKeyStorePath(ksId);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            if (ksFile.exists()) {
                try (InputStream is = new FileInputStream(ksFile)) {
                    ks.load(is, DEFAULT_PASSWORD);
                }
            } else {
                ks.load(null, DEFAULT_PASSWORD);
                saveKeyStore(ksId, ks);
            }

            return ks;
        } catch (Exception e) {
            throw new DBException("Error opening keystore '" + ksId + "'", e);
        }
    }

    @Override
    public void addCertificate(String ksId, String certId, InputStream certStream) throws DBException {
        final KeyStore keyStore = getKeyStore(ksId);
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert =  cf.generateCertificate(certStream);
            keyStore.setCertificateEntry(certId, cert);
            saveKeyStore(ksId, keyStore);
        } catch (Exception e) {
            throw new DBException("Error adding certificate to keystore '" + ksId + "'", e);
        }
    }

    @Override
    public void deleteCertificate(String ksId, String certId) throws DBException {
        final KeyStore keyStore = getKeyStore(ksId);
        try {
            keyStore.deleteEntry(certId);
            saveKeyStore(ksId, keyStore);
        } catch (Exception e) {
            throw new DBException("Error deleting certificate from keystore '" + ksId + "'", e);
        }
    }

    private void saveKeyStore(String ksId, KeyStore keyStore) throws Exception {
        final File ksFile = getKeyStorePath(ksId);

        try (OutputStream os = new FileOutputStream(ksFile)) {
            keyStore.store(os, DEFAULT_PASSWORD);
        }
    }

    @Override
    public File getKeyStorePath(String ksId) {
        return new File(localPath, ksId + JKS_EXTENSION);
    }
}