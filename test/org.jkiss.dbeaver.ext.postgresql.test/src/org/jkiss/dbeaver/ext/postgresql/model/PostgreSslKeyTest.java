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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
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
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import static org.mockito.Mockito.mock;
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
                convertedDuringInitialization = preparePgJdbcKeyFileIfNeeded(monitor, constructorKey);
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
        PostgreDataSource dataSource = new PostgreDataSource(container, "16", "postgres");
        VoidProgressMonitor monitor = new VoidProgressMonitor();
        Path source = Files.createTempFile("dbeaver-test-ssl-", ".key");
        Path first;
        Path second;
        Path encryptedFile;
        try {
            Files.writeString(source, makePkcs1Key());
            first = dataSource.preparePgJdbcKeyFileIfNeeded(monitor, source);
            Assertions.assertNotNull(first);
            Assertions.assertTrue(Files.exists(first));
            Assertions.assertEquals(first, dataSource.preparePgJdbcKeyFileIfNeeded(monitor, source));

            Files.writeString(source, makePkcs1Key());
            second = dataSource.preparePgJdbcKeyFileIfNeeded(monitor, source);
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
            encryptedFile = dataSource.preparePgJdbcKeyFileIfNeeded(monitor, source);
            Assertions.assertNotNull(encryptedFile);
            Assertions.assertArrayEquals(encryptedDer, Files.readAllBytes(encryptedFile));
            Assertions.assertEquals(encryptedFile, dataSource.preparePgJdbcKeyFileIfNeeded(monitor, source));

            Files.writeString(source, "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----\n");
            Assertions.assertNull(dataSource.preparePgJdbcKeyFileIfNeeded(monitor, source));
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
        Path converted = Files.createTempFile("dbeaver-test-ssl-", ".pk8");
        try {
            byte[] pkcs8 = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate().getEncoded();
            Files.writeString(source, "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(pkcs8)
                + "\n-----END PRIVATE KEY-----\n");
            DBWHandlerConfiguration original = new DBWHandlerConfiguration(mock(DBWHandlerDescriptor.class), null);
            original.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, source.toString());
            DBWHandlerConfiguration retry = new DBWHandlerConfiguration(original);

            Assertions.assertTrue(SSLHandlerTrustStoreImpl.loadDerFromPem(retry, converted));
            Assertions.assertEquals(source.toString(), original.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY));
            Assertions.assertEquals(converted.toString(), retry.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY));
            Assertions.assertArrayEquals(pkcs8, Files.readAllBytes(converted));
        } finally {
            Files.deleteIfExists(source);
            Files.deleteIfExists(converted);
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
