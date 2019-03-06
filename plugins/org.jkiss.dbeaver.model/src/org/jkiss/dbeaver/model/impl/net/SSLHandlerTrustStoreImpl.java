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
package org.jkiss.dbeaver.model.impl.net;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.app.DBACertificateStorage;
import org.jkiss.dbeaver.model.impl.app.DefaultCertificateStorage;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;

/**
 * Default Java SSL Handler. Saves certificate in local trust store
 */
public class SSLHandlerTrustStoreImpl extends SSLHandlerImpl {

    public static final String PROP_SSL_CA_CERT = "ssl.ca.cert";
    public static final String PROP_SSL_CLIENT_CERT = "ssl.client.cert";
    public static final String PROP_SSL_CLIENT_KEY = "ssl.client.key";
    public static final String CERT_TYPE = "ssl";

    /**
     * Creates certificates and adds them into trust store
     */
    public static void initializeTrustStore(DBRProgressMonitor monitor, DBPDataSource dataSource, DBWHandlerConfiguration sslConfig) throws DBException, IOException {
        final DBACertificateStorage securityManager = dataSource.getContainer().getPlatform().getCertificateStorage();

        final String caCertProp = sslConfig.getProperties().get(PROP_SSL_CA_CERT);
        final String clientCertProp = sslConfig.getProperties().get(PROP_SSL_CLIENT_CERT);
        final String clientCertKeyProp = sslConfig.getProperties().get(PROP_SSL_CLIENT_KEY);

        {
            // Trust keystore
            if (!CommonUtils.isEmpty(caCertProp) || !CommonUtils.isEmpty(clientCertProp)) {
                byte[] caCertData = CommonUtils.isEmpty(caCertProp) ? null : IOUtils.readFileToBuffer(new File(caCertProp));
                byte[] clientCertData = CommonUtils.isEmpty(clientCertProp) ? null : IOUtils.readFileToBuffer(new File(clientCertProp));
                byte[] keyData = CommonUtils.isEmpty(clientCertKeyProp) ? null : IOUtils.readFileToBuffer(new File(clientCertKeyProp));
                securityManager.addCertificate(dataSource.getContainer(), CERT_TYPE, caCertData, clientCertData, keyData);
            } else {
                securityManager.deleteCertificate(dataSource.getContainer(), CERT_TYPE);
            }
        }

    }

    public static void setGlobalTrustStore(DBPDataSource dataSource) {
        final DBACertificateStorage securityManager = dataSource.getContainer().getPlatform().getCertificateStorage();

        String keyStorePath = securityManager.getKeyStorePath(dataSource.getContainer(), CERT_TYPE).getAbsolutePath();
        String keyStoreType = securityManager.getKeyStoreType(dataSource.getContainer());

        System.setProperty("javax.net.ssl.trustStore", keyStorePath);
        System.setProperty("javax.net.ssl.trustStoreType", keyStoreType);
        System.setProperty("javax.net.ssl.trustStorePassword", String.valueOf(DefaultCertificateStorage.DEFAULT_PASSWORD));
        System.setProperty("javax.net.ssl.keyStore", keyStorePath);
        System.setProperty("javax.net.ssl.keyStoreType", keyStoreType);
        System.setProperty("javax.net.ssl.keyStorePassword", String.valueOf(DefaultCertificateStorage.DEFAULT_PASSWORD));
    }

    public static SSLContext createTrustStoreSslContext(DBPDataSource dataSource) throws Exception {
        final DBACertificateStorage securityManager = dataSource.getContainer().getPlatform().getCertificateStorage();
        KeyStore trustStore = securityManager.getKeyStore(dataSource.getContainer(), CERT_TYPE);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(trustStore, DefaultCertificateStorage.DEFAULT_PASSWORD);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
        trustManagerFactory.init(trustStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }

    public static SSLSocketFactory createTrustStoreSslSocketFactory(DBPDataSource dataSource) throws Exception {
        return createTrustStoreSslContext(dataSource).getSocketFactory();
    }

}
