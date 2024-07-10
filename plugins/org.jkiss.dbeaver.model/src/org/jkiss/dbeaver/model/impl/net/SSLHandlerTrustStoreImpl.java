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
package org.jkiss.dbeaver.model.impl.net;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.app.DBACertificateStorage;
import org.jkiss.dbeaver.model.impl.app.CertificateGenHelper;
import org.jkiss.dbeaver.model.impl.app.DefaultCertificateStorage;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Base64;
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
    public static final String PROP_SSL_KEYSTORE_PASSWORD = "ssl.keystore.password";

    public static final String PROP_SSL_SELF_SIGNED_CERT = "ssl.self-signed-cert";
    public static final String PROP_SSL_METHOD = "ssl.method";
    public static final String PROP_SSL_FORCE_TLS12 = "ssl.forceTls12";

    public static final String TLS_PROTOCOL_VAR_NAME = "jdk.tls.client.protocols";

    /**
     * Creates certificates and adds them into trust store
     */
    public static void initializeTrustStore(DBRProgressMonitor monitor, DBPDataSource dataSource, DBWHandlerConfiguration sslConfig) throws DBException, IOException {
        final DBACertificateStorage securityManager = DBWorkbench.getPlatform().getCertificateStorage();

        final String selfSignedCert = sslConfig.getStringProperty(PROP_SSL_SELF_SIGNED_CERT);
        final String keyStore = sslConfig.getStringProperty(PROP_SSL_KEYSTORE);
        final String keyStoreData = sslConfig.getSecureProperty(PROP_SSL_KEYSTORE_VALUE);

        final SSLConfigurationMethod method = CommonUtils.valueOf(
            SSLConfigurationMethod.class,
            sslConfig.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD),
            SSLConfigurationMethod.CERTIFICATES);

        {
            if (method == SSLConfigurationMethod.KEYSTORE) {
                monitor.subTask("Load keystore");
                final String password = sslConfig.getPassword() == null ?
                    sslConfig.getSecureProperty(PROP_SSL_KEYSTORE_PASSWORD) :
                    sslConfig.getPassword();
                char[] keyStorePasswordData = CommonUtils.isEmpty(password) ? new char[0] : password.toCharArray();
                if (keyStore != null) {
                    securityManager.addCertificate(dataSource.getContainer(), SSLConstants.SSL_CERT_TYPE, keyStore, keyStorePasswordData);
                } else if (keyStoreData != null) {
                    securityManager.addCertificate(
                        dataSource.getContainer(),
                        SSLConstants.SSL_CERT_TYPE,
                        Base64.getDecoder().decode(keyStoreData),
                        keyStorePasswordData
                    );
                }
            } else if (CommonUtils.toBoolean(selfSignedCert)) {
                monitor.subTask("Generate self-signed certificate");
                securityManager.addSelfSignedCertificate(dataSource.getContainer(), SSLConstants.SSL_CERT_TYPE, "CN=" + dataSource.getContainer().getActualConnectionConfiguration().getHostName());
            } else {
                byte[] caCertData = SSLHandlerTrustStoreImpl.readCertificate(sslConfig, SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT);
                byte[] clientCertData = SSLHandlerTrustStoreImpl.readCertificate(sslConfig, SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT);
                byte[] keyData = SSLHandlerTrustStoreImpl.readCertificate(sslConfig, SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY);

                if (caCertData != null || clientCertData != null) {
                    monitor.subTask("Load certificates");
                    securityManager.addCertificate(dataSource.getContainer(), SSLConstants.SSL_CERT_TYPE, caCertData, clientCertData, keyData);
                } else {
                    securityManager.deleteCertificate(dataSource.getContainer(), SSLConstants.SSL_CERT_TYPE);
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

        String keyStorePath = securityManager.getKeyStorePath(dataSource.getContainer(), SSLConstants.SSL_CERT_TYPE).toAbsolutePath().toString();
        String keyStoreType = securityManager.getKeyStoreType(dataSource.getContainer());
        char[] keyStorePass = securityManager.getKeyStorePassword(dataSource.getContainer(), SSLConstants.SSL_CERT_TYPE);

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
        KeyStore trustStore = securityManager.getKeyStore(dataSource.getContainer(), SSLConstants.SSL_CERT_TYPE);
        char[] keyStorePass = securityManager.getKeyStorePassword(dataSource.getContainer(), SSLConstants.SSL_CERT_TYPE);

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


        SSLContext sslContext = forceTLS12 ? SSLContext.getInstance(SSLConstants.TLS_1_2_VERSION) : SSLContext.getInstance("SSL");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }

    public static SSLSocketFactory createTrustStoreSslSocketFactory(DBPDataSource dataSource, DBWHandlerConfiguration sslConfig) throws Exception {
        return createTrustStoreSslContext(dataSource, sslConfig).getSocketFactory();
    }

    public static boolean loadDerFromPem(
        final @NotNull DBWHandlerConfiguration handler,
        final @NotNull Path tempDerFile
    ) throws IOException {
        final byte[] key = SSLHandlerTrustStoreImpl.readCertificate(handler, SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY);
        if (key == null) {
            return false;
        }
        final Reader reader = new StringReader(new String(key, StandardCharsets.UTF_8));
        Files.write(tempDerFile, DefaultCertificateStorage.loadDerFromPem(reader));
        String derCertPath = tempDerFile.toAbsolutePath().toString();
        if (DBWorkbench.isDistributed() || DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            handler.setSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, derCertPath);
        } else {
            handler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, derCertPath);
        }
        // Unfortunately, we can't delete the temp file here.
        // The chain is built asynchronously by the driver, and we don't know at which moment in time it will happen.
        // It will still be deleted during shutdown.
        return true;
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

    /**
     * Reads trust store file contents.
     */
    public static byte[] readTrustStoreData(@NotNull DBWHandlerConfiguration configuration, @NotNull String property) throws DBException {
        var propertyValue = configuration.getSecureProperty(property);
        if (!CommonUtils.isEmpty(propertyValue)) {
            try {
                return Files.readAllBytes(Path.of(propertyValue));
            } catch (IOException e) {
                throw new DBException("Error reading file '" + property + "' data", e);
            }
        }
        var valueProperty = configuration.getSecureProperty(property + SSLHandlerTrustStoreImpl.CERT_VALUE_SUFFIX);
        if (!CommonUtils.isEmpty(valueProperty)) {
            return Base64.getDecoder().decode(valueProperty);
        }
        return null;
    }
}
