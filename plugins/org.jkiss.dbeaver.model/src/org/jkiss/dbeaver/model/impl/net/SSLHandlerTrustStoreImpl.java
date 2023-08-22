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
package org.jkiss.dbeaver.model.impl.net;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.app.DBACertificateStorage;
import org.jkiss.dbeaver.model.impl.app.CertificateGenHelper;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default Java SSL Handler. Saves certificate in local trust store
 */
public class SSLHandlerTrustStoreImpl extends SSLHandlerImpl {

    public static final String CERT_VALUE_SUFFIX = ".value";

    public static final String PROP_SSL_CA_CERT = "ssl.ca.cert";
    public static final String PROP_SSL_CA_CERT_VALUE = PROP_SSL_CA_CERT + CERT_VALUE_SUFFIX;
    public static final String PROP_SSL_CLIENT_CERT = "ssl.client.cert";
    public static final String PROP_SSL_CLIENT_CERT_VALUE = PROP_SSL_CLIENT_CERT + CERT_VALUE_SUFFIX;
    public static final String PROP_SSL_CLIENT_KEY = "ssl.client.key";
    public static final String PROP_SSL_CLIENT_KEY_VALUE = PROP_SSL_CLIENT_KEY + CERT_VALUE_SUFFIX;

    public static final String PROP_SSL_KEYSTORE = "ssl.keystore";
    public static final String PROP_SSL_KEYSTORE_VALUE = PROP_SSL_KEYSTORE + CERT_VALUE_SUFFIX;

    public static final String PROP_SSL_SELF_SIGNED_CERT = "ssl.self-signed-cert";
    public static final String PROP_SSL_METHOD = "ssl.method";
    public static final String PROP_SSL_FORCE_TLS12 = "ssl.forceTls12";
    public static final String CERT_TYPE = "ssl";

    public static final String TLS_PROTOCOL_VAR_NAME = "jdk.tls.client.protocols";
    public static final String TLS_1_2_VERSION = "TLSv1.2";

    /**
     * Creates certificates and adds them into trust store
     */
    public static void initializeTrustStore(DBRProgressMonitor monitor, DBPDataSource dataSource, DBWHandlerConfiguration sslConfig) throws DBException, IOException {
        final DBACertificateStorage securityManager = DBWorkbench.getPlatform().getCertificateStorage();

        final String selfSignedCert = sslConfig.getStringProperty(PROP_SSL_SELF_SIGNED_CERT);
        final String keyStore = sslConfig.getStringProperty(PROP_SSL_KEYSTORE);

        final SSLConfigurationMethod method = CommonUtils.valueOf(
            SSLConfigurationMethod.class,
            sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD),
            SSLConfigurationMethod.CERTIFICATES);

        {
            if (method == SSLConfigurationMethod.KEYSTORE && keyStore != null) {
                monitor.subTask("Load keystore");
                final String password = sslConfig.getPassword();

                char[] keyStorePasswordData = CommonUtils.isEmpty(password) ? new char[0] : password.toCharArray();
                securityManager.addCertificate(dataSource.getContainer(), CERT_TYPE, keyStore, keyStorePasswordData);
            } else if (CommonUtils.toBoolean(selfSignedCert)) {
                monitor.subTask("Generate self-signed certificate");
                securityManager.addSelfSignedCertificate(dataSource.getContainer(), CERT_TYPE, "CN=" + dataSource.getContainer().getActualConnectionConfiguration().getHostName());
            } else {
                byte[] caCertData = SSLHandlerTrustStoreImpl.readCertificate(sslConfig, SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT);
                byte[] clientCertData = SSLHandlerTrustStoreImpl.readCertificate(sslConfig, SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT);
                byte[] keyData = SSLHandlerTrustStoreImpl.readCertificate(sslConfig, SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY);

                if (caCertData != null || clientCertData != null) {
                    monitor.subTask("Load certificates");
                    securityManager.addCertificate(dataSource.getContainer(), CERT_TYPE, caCertData, clientCertData, keyData);
                } else {
                    securityManager.deleteCertificate(dataSource.getContainer(), CERT_TYPE);
                }
            }
        }
    }

    public static byte[] readCertificate(DBWHandlerConfiguration configuration, String basePropName) throws IOException {
        return readCertificate(configuration, basePropName, null);
    }

    public static byte[] readCertificate(DBWHandlerConfiguration configuration, String basePropName, String altPropName) throws IOException {
        String filePath = configuration.getStringProperty(basePropName);
        if (CommonUtils.isEmpty(filePath) && altPropName != null) {
            filePath = configuration.getStringProperty(altPropName);
        }
        if (!CommonUtils.isEmpty(filePath)) {
            return Files.readAllBytes(Path.of(filePath));
        }
        String certValue = configuration.getSecureProperty(basePropName + SSLHandlerTrustStoreImpl.CERT_VALUE_SUFFIX);
        if (!CommonUtils.isEmpty(certValue)) {
            return certValue.getBytes(StandardCharsets.UTF_8);
        }
        return null;
    }

    public static Map<String, String> setGlobalTrustStore(DBPDataSource dataSource) {
        final DBACertificateStorage securityManager = DBWorkbench.getPlatform().getCertificateStorage();

        String keyStorePath = securityManager.getKeyStorePath(dataSource.getContainer(), CERT_TYPE).toAbsolutePath().toString();
        String keyStoreType = securityManager.getKeyStoreType(dataSource.getContainer());
        char[] keyStorePass = securityManager.getKeyStorePassword(dataSource.getContainer(), CERT_TYPE);

        Map<String, String> oldProps = new LinkedHashMap<>();
        setSystemProperty("javax.net.ssl.trustStore", keyStorePath, oldProps);
        setSystemProperty("javax.net.ssl.trustStoreType", keyStoreType, oldProps);
        setSystemProperty("javax.net.ssl.trustStorePassword", String.valueOf(keyStorePass), oldProps);
        setSystemProperty("javax.net.ssl.keyStore", keyStorePath, oldProps);
        setSystemProperty("javax.net.ssl.keyStoreType", keyStoreType, oldProps);
        setSystemProperty("javax.net.ssl.keyStorePassword", String.valueOf(keyStorePass), oldProps);

        return oldProps;
    }

    public static void resetGlobalTrustStore(Map<String, String> oldProps) {
        for (Map.Entry<String, String> pe : oldProps.entrySet()) {
            if (pe.getValue() == null) {
                System.clearProperty(pe.getKey());
            } else {
                System.setProperty(pe.getKey(), pe.getValue());
            }
        }
    }

    private static void setSystemProperty(String propName, String propValue, Map<String, String> oldProps) {
        String oldValue = System.setProperty(propName, propValue);
        oldProps.put(propName, oldValue);
    }

    public static SSLContext createTrustStoreSslContext(DBPDataSource dataSource, DBWHandlerConfiguration sslConfig) throws Exception {
        final DBACertificateStorage securityManager = DBWorkbench.getPlatform().getCertificateStorage();
        KeyStore trustStore = securityManager.getKeyStore(dataSource.getContainer(), CERT_TYPE);
        char[] keyStorePass = securityManager.getKeyStorePassword(dataSource.getContainer(), CERT_TYPE);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(trustStore, keyStorePass);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers;
        if (sslConfig.getBooleanProperty(PROP_SSL_SELF_SIGNED_CERT)) {
            trustManagers = CertificateGenHelper.NON_VALIDATING_TRUST_MANAGERS;
        } else {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        }

        final boolean forceTLS12 = sslConfig.getBooleanProperty(PROP_SSL_FORCE_TLS12);


        SSLContext sslContext = forceTLS12 ? SSLContext.getInstance(TLS_1_2_VERSION) : SSLContext.getInstance("SSL");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }

    public static SSLSocketFactory createTrustStoreSslSocketFactory(DBPDataSource dataSource, DBWHandlerConfiguration sslConfig) throws Exception {
        return createTrustStoreSslContext(dataSource, sslConfig).getSocketFactory();
    }

    /**
     * Creates a non-validating SSL socket factory.
     */
    @NotNull
    public static SSLSocketFactory createNonValidatingSslSocketFactory() throws Exception {
        final SSLContext context = SSLContext.getInstance("SSL");
        context.init(null, CertificateGenHelper.NON_VALIDATING_TRUST_MANAGERS, new SecureRandom());

        return context.getSocketFactory();
    }
}
