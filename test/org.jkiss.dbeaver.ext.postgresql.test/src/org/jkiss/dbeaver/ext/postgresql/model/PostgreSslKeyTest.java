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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.net.PostgreSSLClientKeyManager;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceProvider;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.sql.Driver;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostgreSslKeyTest extends DBeaverUnitTest {
    private static Path constructorKey;

    @Test
    public void preparesKeyDuringSuperclassConstruction() throws Exception {
        DBPDataSourceContainer container = mock(DBPDataSourceContainer.class);
        when(container.getDriver()).thenReturn(mock(DBPDriver.class));
        when(container.getConnectionConfiguration()).thenReturn(new DBPConnectionConfiguration());
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        Path source = Files.createTempFile("dbeaver-test-ssl-", ".key");
        Path converted;
        try {
            Files.writeString(source, makePkcs1Key());
            constructorKey = source;
            InitializingPostgreDataSource dataSource = new InitializingPostgreDataSource(monitor, container);
            try {
                converted = dataSource.convertedDuringInitialization;
                Assertions.assertNotNull(converted);
                Assertions.assertTrue(Files.exists(converted));
            } finally {
                dataSource.shutdown(monitor);
            }
        } finally {
            constructorKey = null;
            Files.deleteIfExists(source);
        }
        Assertions.assertFalse(Files.exists(converted));
    }

    private static final class InitializingPostgreDataSource extends PostgreDataSource {
        private Path convertedDuringInitialization;

        private InitializingPostgreDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
            super(monitor, container);
        }

        @Override
        protected void initializeRemoteInstance(@NotNull DBRProgressMonitor monitor) throws DBException {
            try {
                this.convertedDuringInitialization = this.getSSLClientKeyManager().prepareKeyFileIfNeeded(monitor, constructorKey);
            } catch (IOException e) {
                throw new DBException("Could not prepare test key", e);
            }
        }

        @NotNull
        @Override
        public List<PostgreDatabase> getAvailableInstances() {
            return List.of();
        }
    }

    @Test
    public void reusesConvertedKeyUntilSourceChangesAndDeletesItOnShutdown() throws Exception {
        DBPDataSourceContainer container = mock(DBPDataSourceContainer.class);
        when(container.getDriver()).thenReturn(mock(DBPDriver.class));
        TestPostgreDataSource dataSource = new TestPostgreDataSource(container);
        PostgreSSLClientKeyManager keyManager = dataSource.getSSLClientKeyManager();
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        Path source = Files.createTempFile("dbeaver-test-ssl-", ".key");
        Path first;
        Path second;
        Path encryptedFile;
        try {
            Files.writeString(source, makePkcs1Key());
            first = keyManager.prepareKeyFileIfNeeded(monitor, source);
            Assertions.assertNotNull(first);
            Assertions.assertTrue(Files.exists(first));
            Assertions.assertEquals(first, keyManager.prepareKeyFileIfNeeded(monitor, source));

            Files.writeString(source, makePkcs1Key());
            second = keyManager.prepareKeyFileIfNeeded(monitor, source);
            Assertions.assertNotNull(second);
            Assertions.assertNotEquals(first, second);
            Assertions.assertTrue(Files.exists(first)); // an existing handshake may still use it
            Assertions.assertTrue(Files.exists(second));

            byte[] pkcs8 = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate().getEncoded();
            String algorithm = "PBEWithSHA1AndDESede";
            var passwordKey = SecretKeyFactory.getInstance(algorithm).generateSecret(new PBEKeySpec("password".toCharArray()));
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, passwordKey);
            byte[] encryptedDer = new EncryptedPrivateKeyInfo(cipher.getParameters(), cipher.doFinal(pkcs8)).getEncoded();
            Files.writeString(source, "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(encryptedDer) + "\n-----END ENCRYPTED PRIVATE KEY-----\n");
            encryptedFile = keyManager.prepareKeyFileIfNeeded(monitor, source);
            Assertions.assertNotNull(encryptedFile);
            Assertions.assertArrayEquals(encryptedDer, Files.readAllBytes(encryptedFile));
            Assertions.assertEquals(encryptedFile, keyManager.prepareKeyFileIfNeeded(monitor, source));

            Files.writeString(source, "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----\n");
            Assertions.assertNull(keyManager.prepareKeyFileIfNeeded(monitor, source));
        } finally {
            dataSource.shutdown(monitor);
            Files.deleteIfExists(source);
        }
        Assertions.assertFalse(Files.exists(first));
        Assertions.assertFalse(Files.exists(second));
        Assertions.assertFalse(Files.exists(encryptedFile));
    }

    @Test
    public void legacyRetryCanConvertCopyWithoutChangingSavedConfiguration() throws Exception {
        Path source = Files.createTempFile("dbeaver-test-ssl-", ".pem");
        List<Path> trackedFiles = new ArrayList<>();
        try {
            byte[] pkcs8 = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate().getEncoded();
            Files.writeString(source, "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(pkcs8)
                + "\n-----END PRIVATE KEY-----\n");
            DBWHandlerDescriptor descriptor = mock(DBWHandlerDescriptor.class);
            when(descriptor.getId()).thenReturn(PostgreConstants.HANDLER_SSL);
            DBWHandlerConfiguration original = new DBWHandlerConfiguration(descriptor, null);
            original.setEnabled(true);
            original.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD, "CERTIFICATES");
            original.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, source.toString());
            DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
            connectionInfo.updateHandler(original);
            PostgreSSLClientKeyManager keyManager = new PostgreSSLClientKeyManager(trackedFiles::add);
            DBPConnectionConfiguration retryConfig = keyManager.prepareRetryConfiguration(
                new VoidProgressMonitor(), connectionInfo, new DBCException("Could not read SSL key")
            );

            Assertions.assertNotNull(retryConfig);
            DBWHandlerConfiguration retry = retryConfig.getHandler(PostgreConstants.HANDLER_SSL);
            Assertions.assertNotNull(retry);
            Path converted = Path.of(retry.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY));
            Assertions.assertEquals(source.toString(), original.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY));
            Assertions.assertEquals(converted.toString(), retry.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY));
            Assertions.assertArrayEquals(pkcs8, Files.readAllBytes(converted));
            Assertions.assertEquals(List.of(converted), trackedFiles);
        } finally {
            Files.deleteIfExists(source);
            for (Path tracked : trackedFiles) {
                Files.deleteIfExists(tracked);
            }
        }
    }

    @Test
    public void preparesConnectionPropertiesOnlyForDefaultSslFactory() throws Exception {
        List<Path> trackedFiles = new ArrayList<>();
        PostgreSSLClientKeyManager keyManager = new PostgreSSLClientKeyManager(trackedFiles::add);
        Path source = Files.createTempFile("dbeaver-test-ssl-", ".key");
        Properties properties = new Properties();
        properties.setProperty("sslkey", source.toString());
        try {
            Files.writeString(source, makePkcs1Key());
            properties.setProperty("sslfactory", "example.CustomFactory");
            keyManager.prepareConnectionProperties(new VoidProgressMonitor(), properties, null);
            Assertions.assertEquals(source.toString(), properties.getProperty("sslkey"));

            properties.remove("sslfactory");
            properties.setProperty("sslmode", "disable");
            keyManager.prepareConnectionProperties(new VoidProgressMonitor(), properties, null);
            Assertions.assertEquals(source.toString(), properties.getProperty("sslkey"));

            properties.remove("sslmode");
            keyManager.prepareConnectionProperties(new VoidProgressMonitor(), properties, null);
            Assertions.assertNotEquals(source.toString(), properties.getProperty("sslkey"));
            Assertions.assertEquals(List.of(Path.of(properties.getProperty("sslkey"))), trackedFiles);
        } finally {
            Files.deleteIfExists(source);
            for (Path tracked : trackedFiles) {
                Files.deleteIfExists(tracked);
            }
        }
    }

    @Test
    public void sslOverridesFromPropertiesAndUrlDoNotReadTheHandlerKey() throws Exception {
        Path missingKey = Files.createTempFile("dbeaver-test-ssl-", ".key");
        Files.delete(missingKey);
        DBPDataSourceContainer container = mock(DBPDataSourceContainer.class);
        DBPDriver driver = mock(DBPDriver.class);
        when(container.getDriver()).thenReturn(driver);
        DBPConnectionConfiguration config = new DBPConnectionConfiguration();
        when(container.getActualConnectionConfiguration()).thenReturn(config);
        TestPostgreDataSource dataSource = new TestPostgreDataSource(container);
        DBWHandlerDescriptor descriptor = mock(DBWHandlerDescriptor.class);
        when(descriptor.getId()).thenReturn(PostgreConstants.HANDLER_SSL);
        DBWHandlerConfiguration handler = new DBWHandlerConfiguration(descriptor, container);
        handler.setEnabled(true);
        handler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD, "CERTIFICATES");
        handler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, missingKey.toString());
        config.updateHandler(handler);

        try {
            when(driver.getConnectionProperties()).thenReturn(Map.of("sslfactory", "example.CustomFactory"));
            Properties properties = assembleAndFixupProperties(dataSource, config);
            Assertions.assertEquals(missingKey.toString(), properties.getProperty("sslkey"));

            when(driver.getConnectionProperties()).thenReturn(Map.of("sslmode", "DISABLE"));
            properties = assembleAndFixupProperties(dataSource, config);
            Assertions.assertEquals(missingKey.toString(), properties.getProperty("sslkey"));

            when(driver.getConnectionProperties()).thenReturn(Map.of());
            config.setProperty("sslfactory", "example.CustomFactory");
            properties = assembleAndFixupProperties(dataSource, config);
            Assertions.assertEquals(missingKey.toString(), properties.getProperty("sslkey"));

            config.setProperty("sslfactory", null);
            config.setProperty("sslmode", "disable");
            properties = assembleAndFixupProperties(dataSource, config);
            Assertions.assertEquals(missingKey.toString(), properties.getProperty("sslkey"));

            config.setProperty("sslmode", null);
            config.setUrl("jdbc:postgresql://localhost/postgres?sslmode=DISABLE");
            properties = assembleAndFixupProperties(dataSource, config);
            Assertions.assertEquals(missingKey.toString(), properties.getProperty("sslkey"));

            config.setUrl("jdbc:postgresql://localhost/postgres?sslfactory=example.CustomFactory");
            properties = assembleAndFixupProperties(dataSource, config);
            Assertions.assertEquals(missingKey.toString(), properties.getProperty("sslkey"));

            config.setUrl("jdbc:postgresql://localhost/postgres?sslkey=another.key");
            properties = assembleAndFixupProperties(dataSource, config);
            Assertions.assertEquals(missingKey.toString(), properties.getProperty("sslkey"));

            config.setUrl("jdbc:postgresql://localhost/postgres?sslmode=require&sslmode=%44ISABLE");
            properties = assembleAndFixupProperties(dataSource, config);
            Assertions.assertEquals(missingKey.toString(), properties.getProperty("sslkey"));

            for (String query : List.of(
                "sslkey", "&sslkey=&", "option=a=b=c&&sslmode=DISABLE&", "sslfactory=example%2ECustomFactory",
                "sslmode=require&sslmode=disable", "option=%invalid"
            )) {
                config.setUrl("jdbc:postgresql:postgres?" + query);
                properties = assembleAndFixupProperties(dataSource, config);
                Assertions.assertEquals(missingKey.toString(), properties.getProperty("sslkey"), query);
            }
        } finally {
            dataSource.shutdown(new VoidProgressMonitor());
        }
    }

    @Test
    public void convertsTheKeyFromMergedDriverProperties() throws Exception {
        Path source = Files.createTempFile("dbeaver-test-ssl-", ".key");
        Path missingHandlerKey = Files.createTempFile("dbeaver-test-ssl-", ".key");
        Files.delete(missingHandlerKey);
        DBPDataSourceContainer container = mock(DBPDataSourceContainer.class);
        DBPDriver driver = mock(DBPDriver.class);
        when(container.getDriver()).thenReturn(driver);
        when(driver.getConnectionProperties()).thenReturn(Map.of("sslkey", source.toString(), "sslmode", "disable"));
        DBPConnectionConfiguration config = new DBPConnectionConfiguration();
        config.setUrl("jdbc:postgresql:postgres?sslmode=disable&sslmode=require&option=a=b=c&empty=&ssl");
        when(container.getActualConnectionConfiguration()).thenReturn(config);
        TestPostgreDataSource dataSource = new TestPostgreDataSource(container);
        DBWHandlerDescriptor descriptor = mock(DBWHandlerDescriptor.class);
        when(descriptor.getId()).thenReturn(PostgreConstants.HANDLER_SSL);
        DBWHandlerConfiguration handler = new DBWHandlerConfiguration(descriptor, container);
        handler.setEnabled(true);
        handler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD, "CERTIFICATES");
        handler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, missingHandlerKey.toString());
        config.updateHandler(handler);
        Path converted;
        try {
            Files.writeString(source, makePkcs1Key());
            Properties properties = assembleAndFixupProperties(dataSource, config);
            converted = Path.of(properties.getProperty("sslkey"));
            Assertions.assertNotEquals(source, converted);
            Assertions.assertTrue(Files.exists(converted));
        } finally {
            dataSource.shutdown(new VoidProgressMonitor());
            Files.deleteIfExists(source);
        }
        Assertions.assertFalse(Files.exists(converted));
    }

    @Test
    public void fixupUsesPropertiesAndUrlReplacedAfterCollection() throws Exception {
        DBPDataSourceContainer container = mock(DBPDataSourceContainer.class);
        DBPDriver driver = mock(DBPDriver.class);
        when(container.getDriver()).thenReturn(driver);
        when(driver.getConnectionProperties()).thenReturn(Map.of());
        DBPConnectionConfiguration config = new DBPConnectionConfiguration();
        when(container.getActualConnectionConfiguration()).thenReturn(config);
        TestPostgreDataSource dataSource = new TestPostgreDataSource(container);
        Path source = Files.createTempFile("dbeaver-test-ssl-", ".key");
        Path missing = source.resolveSibling(source.getFileName() + ".missing");
        DBWHandlerDescriptor descriptor = mock(DBWHandlerDescriptor.class);
        when(descriptor.getId()).thenReturn(PostgreConstants.HANDLER_SSL);
        DBWHandlerConfiguration handler = new DBWHandlerConfiguration(descriptor, container);
        handler.setEnabled(true);
        handler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD, "CERTIFICATES");
        handler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, missing.toString());
        config.updateHandler(handler);
        try {
            Files.writeString(source, makePkcs1Key());
            VoidProgressMonitor monitor = new VoidProgressMonitor();
            Properties properties = dataSource.collectProperties(monitor, config);
            Assertions.assertEquals(missing.toString(), properties.getProperty("sslkey"));

            // Substitution and authentication may change the collected properties and the URL before fixup.
            properties.setProperty("sslkey", source.toString());
            properties.setProperty("sslmode", "disable");
            dataSource.fixupConnectionProperties(monitor, config, "jdbc:postgresql:postgres?sslmode=require", properties);
            Assertions.assertNotEquals(source.toString(), properties.getProperty("sslkey"));

            properties.setProperty("sslkey", missing.toString());
            properties.setProperty("sslmode", "require");
            dataSource.fixupConnectionProperties(monitor, config, "jdbc:postgresql:postgres?sslmode=disable", properties);
            Assertions.assertEquals(missing.toString(), properties.getProperty("sslkey"));
        } finally {
            dataSource.shutdown(new VoidProgressMonitor());
            Files.deleteIfExists(source);
        }
    }

    @Test
    public void fixupFailureEndsAuthenticationWithoutRepeatingIt() throws Exception {
        DBPDataSourceContainer container = mock(DBPDataSourceContainer.class);
        DBPDriver driver = mock(DBPDriver.class);
        when(container.getDriver()).thenReturn(driver);
        when(container.getPreferenceStore()).thenReturn(mock(DBPPreferenceStore.class));
        DBPConnectionConfiguration config = new DBPConnectionConfiguration();
        config.setUrl("jdbc:postgresql:postgres");
        when(container.getActualConnectionConfiguration()).thenReturn(config);
        DBPDataSourceProvider<?> provider = mock(DBPDataSourceProvider.class);
        when(driver.getDataSourceProvider()).thenReturn((DBPDataSourceProvider) provider);
        DBPAuthModelDescriptor descriptor = mock(DBPAuthModelDescriptor.class);
        when(provider.detectConnectionAuthModel(driver, config)).thenReturn(descriptor);
        when(descriptor.isApplicableTo(driver)).thenReturn(true);
        DBAAuthModel<DBAAuthCredentials> authModel = mock(DBAAuthModel.class);
        when(descriptor.getInstance()).thenReturn((DBAAuthModel) authModel);
        DBAAuthCredentials credentials = mock(DBAAuthCredentials.class);
        when(authModel.loadCredentials(container, config)).thenReturn(credentials);
        Properties properties = new Properties();
        FixupFailingDataSource dataSource = new FixupFailingDataSource(container, properties);
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        doAnswer(invocation -> {
            properties.setProperty("sslmode", "disable");
            config.setHostName("authenticated-host");
            config.setUrl("jdbc:postgresql://authenticated-host/postgres");
            return null;
        }).when(authModel).initAuthentication(monitor, dataSource, credentials, config, properties);
        try {
            Assertions.assertThrows(DBCException.class, () -> dataSource.openForTest(monitor, config));
            Assertions.assertTrue(dataSource.fixupCalled);
            verify(authModel).loadCredentials(container, config);
            verify(authModel).initAuthentication(monitor, dataSource, credentials, config, properties);
            verify(authModel).endAuthentication(container, config, properties);
        } finally {
            dataSource.shutdown(new VoidProgressMonitor());
        }
    }

    private static final class FixupFailingDataSource extends PostgreDataSource {
        private final Properties properties;
        private boolean fixupCalled;

        private FixupFailingDataSource(@NotNull DBPDataSourceContainer container, @NotNull Properties properties) {
            super(container, "16", "postgres");
            this.properties = properties;
        }

        private void openForTest(@NotNull DBRProgressMonitor monitor, @NotNull DBPConnectionConfiguration config) throws DBCException {
            super.openConnection(monitor, null, config, "test");
        }

        @NotNull
        @Override
        protected Properties getAllConnectionProperties(
            @NotNull DBRProgressMonitor monitor,
            @Nullable JDBCExecutionContext context,
            @NotNull String purpose,
            @NotNull DBPConnectionConfiguration config
        ) {
            return this.properties;
        }

        @Override
        @Nullable
        protected Driver createDriverInstance(@NotNull DBRProgressMonitor monitor, @NotNull DBPDriver driver) {
            return null;
        }

        @Override
        protected void fixupConnectionProperties(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPConnectionConfiguration config,
            @NotNull String url,
            @NotNull Properties properties
        ) throws DBException {
            this.fixupCalled = true;
            Assertions.assertEquals("disable", properties.getProperty("sslmode"));
            Assertions.assertEquals("jdbc:postgresql://authenticated-host/postgres", url);
            throw new DBException("Test fixup failure");
        }
    }

    @Test
    public void detectsPkcs8PemInFileOrStoredKeyWithoutReadingEntireFile() throws Exception {
        DBWHandlerConfiguration handler = new DBWHandlerConfiguration(mock(DBWHandlerDescriptor.class), null);
        Path source = Files.createTempFile("dbeaver-test-ssl-", ".pem");
        try {
            Files.writeString(source, "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----\n");
            handler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, source.toString());
            Assertions.assertTrue(SSLHandlerTrustStoreImpl.canRetryWithPkcs8PemKey(handler));

            Files.writeString(source, " ".repeat(64 * 1024) + "-----BEGIN PRIVATE KEY-----");
            Assertions.assertFalse(SSLHandlerTrustStoreImpl.canRetryWithPkcs8PemKey(handler));

            handler.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, null);
            handler.setSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY_VALUE,
                "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----\n");
            Assertions.assertTrue(SSLHandlerTrustStoreImpl.canRetryWithPkcs8PemKey(handler));
            handler.setSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY_VALUE,
                "-----BEGIN ENCRYPTED PRIVATE KEY-----\nAAAA\n-----END ENCRYPTED PRIVATE KEY-----\n");
            Assertions.assertTrue(SSLHandlerTrustStoreImpl.canRetryWithPkcs8PemKey(handler));
            handler.setSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, source.toString());
            Assertions.assertFalse(SSLHandlerTrustStoreImpl.canRetryWithPkcs8PemKey(handler));
        } finally {
            Files.deleteIfExists(source);
        }
    }

    @NotNull
    private static Properties assembleAndFixupProperties(
        @NotNull TestPostgreDataSource dataSource,
        @NotNull DBPConnectionConfiguration config
    ) throws DBException {
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        Properties properties = dataSource.collectProperties(monitor, config);
        String url = config.getUrl() == null ? "jdbc:postgresql://localhost/postgres" : config.getUrl();
        dataSource.fixupConnectionProperties(monitor, config, url, properties);
        return properties;
    }

    private static final class TestPostgreDataSource extends PostgreDataSource {
        private TestPostgreDataSource(@NotNull DBPDataSourceContainer container) {
            super(container, "16", "postgres");
        }

        @NotNull
        private Properties collectProperties(@NotNull DBRProgressMonitor monitor, @NotNull DBPConnectionConfiguration config)
            throws DBCException {
            return super.getAllConnectionProperties(monitor, null, "test", config);
        }
    }

    @NotNull
    private static String makePkcs1Key() throws Exception {
        var key = (RSAPrivateCrtKey) KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
        var fields = new ByteArrayOutputStream();
        writeInteger(fields, BigInteger.ZERO);
        writeInteger(fields, key.getModulus());
        writeInteger(fields, key.getPublicExponent());
        writeInteger(fields, key.getPrivateExponent());
        writeInteger(fields, key.getPrimeP());
        writeInteger(fields, key.getPrimeQ());
        writeInteger(fields, key.getPrimeExponentP());
        writeInteger(fields, key.getPrimeExponentQ());
        writeInteger(fields, key.getCrtCoefficient());
        var sequence = new ByteArrayOutputStream();
        sequence.write(0x30);
        writeLength(sequence, fields.size());
        fields.writeTo(sequence);
        return "-----BEGIN RSA PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(sequence.toByteArray())
            + "\n-----END RSA PRIVATE KEY-----\n";
    }

    private static void writeInteger(@NotNull ByteArrayOutputStream out, @NotNull BigInteger value) {
        byte[] bytes = value.toByteArray();
        out.write(0x02);
        writeLength(out, bytes.length);
        out.writeBytes(bytes);
    }

    private static void writeLength(@NotNull ByteArrayOutputStream out, int length) {
        if (length < 128) {
            out.write(length);
        } else if (length < 256) {
            out.write(0x81);
            out.write(length);
        } else {
            out.write(0x82);
            out.write(length >>> 8);
            out.write(length);
        }
    }
}
