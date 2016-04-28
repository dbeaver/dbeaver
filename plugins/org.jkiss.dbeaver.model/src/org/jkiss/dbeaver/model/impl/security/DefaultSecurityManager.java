/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.security;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPSecurityManager;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * DefaultSecurityManager
 */
public class DefaultSecurityManager implements DBPSecurityManager {

    private static final Log log = Log.getLog(DefaultSecurityManager.class);
    private static final char[] DEFAULT_PASSWORD = "".toCharArray();
    public static final String JKS_EXTENSION = ".jks";

    private final File localPath;

    public DefaultSecurityManager(File localPath) {
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