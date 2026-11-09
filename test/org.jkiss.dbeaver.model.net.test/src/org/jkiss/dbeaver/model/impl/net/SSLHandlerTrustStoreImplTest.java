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
package org.jkiss.dbeaver.model.impl.net;

import org.jkiss.code.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class SSLHandlerTrustStoreImplTest {

    @TempDir
    Path tempDir;

    @Test
    public void convertsPkcs1PemToPkcs8() throws Exception {
        var privateKey = (RSAPrivateCrtKey) KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
        var fields = new ByteArrayOutputStream();
        writeDerInteger(fields, BigInteger.ZERO);
        writeDerInteger(fields, privateKey.getModulus());
        writeDerInteger(fields, privateKey.getPublicExponent());
        writeDerInteger(fields, privateKey.getPrivateExponent());
        writeDerInteger(fields, privateKey.getPrimeP());
        writeDerInteger(fields, privateKey.getPrimeQ());
        writeDerInteger(fields, privateKey.getPrimeExponentP());
        writeDerInteger(fields, privateKey.getPrimeExponentQ());
        writeDerInteger(fields, privateKey.getCrtCoefficient());
        var pkcs1 = new ByteArrayOutputStream();
        pkcs1.write(0x30);
        writeDerLength(pkcs1, fields.size());
        fields.writeTo(pkcs1);

        Path source = tempDir.resolve("client.key");
        Path target = tempDir.resolve("client.pk8");
        Files.writeString(source, "-----BEGIN RSA PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(pkcs1.toByteArray())
            + "\n-----END RSA PRIVATE KEY-----\n");

        Assertions.assertTrue(SSLHandlerTrustStoreImpl.hasPkcs1PrivateKeyHeader(source));
        SSLHandlerTrustStoreImpl.savePkcs1PrivateKeyAsPkcs8File(Files.readAllBytes(source), target);
        Assertions.assertArrayEquals(
            privateKey.getEncoded(),
            KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(target))).getEncoded()
        );
        Assertions.assertEquals("-----BEGIN RSA PRIVATE KEY-----", Files.readAllLines(source).getFirst());
        assertNoStagingFiles();
        if (supportsPosixPermissions()) {
            Assertions.assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(target));
        }
    }

    @Test
    public void convertsEncryptedPkcs8PemToEncryptedDer() throws Exception {
        byte[] pkcs8 = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate().getEncoded();
        String algorithm = "PBEWithSHA1AndDESede";
        var passwordKey = SecretKeyFactory.getInstance(algorithm).generateSecret(new PBEKeySpec("password".toCharArray()));
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, passwordKey);
        byte[] encrypted = new EncryptedPrivateKeyInfo(cipher.getParameters(), cipher.doFinal(pkcs8)).getEncoded();
        Path source = tempDir.resolve("encrypted.key");
        Path target = tempDir.resolve("encrypted.pk8");
        Files.writeString(source, "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encrypted)
            + "\n-----END ENCRYPTED PRIVATE KEY-----\n");

        Assertions.assertEquals(SSLHandlerTrustStoreImpl.PemPrivateKeyFormat.ENCRYPTED_PKCS8,
            SSLHandlerTrustStoreImpl.detectPemPrivateKeyFormat(source));
        SSLHandlerTrustStoreImpl.saveEncryptedPkcs8PemAsDerFile(Files.readAllBytes(source), target);
        Assertions.assertArrayEquals(encrypted, Files.readAllBytes(target));
        EncryptedPrivateKeyInfo converted = new EncryptedPrivateKeyInfo(Files.readAllBytes(target));
        cipher.init(Cipher.DECRYPT_MODE, passwordKey, converted.getAlgParameters());
        Assertions.assertArrayEquals(pkcs8, converted.getKeySpec(cipher).getEncoded());
        if (supportsPosixPermissions()) {
            Assertions.assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(target));
        }
    }

    @Test
    public void doesNotConvertAlreadySupportedKeys() throws Exception {
        Path source = tempDir.resolve("client.key");
        Path target = tempDir.resolve("client.pk8");
        byte[] pkcs8 = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate().getEncoded();
        Files.writeString(source, "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getEncoder().encodeToString(pkcs8) + "\n-----END PRIVATE KEY-----\n");
        Assertions.assertFalse(SSLHandlerTrustStoreImpl.hasPkcs1PrivateKeyHeader(source));
        Assertions.assertEquals(SSLHandlerTrustStoreImpl.PemPrivateKeyFormat.OTHER,
            SSLHandlerTrustStoreImpl.detectPemPrivateKeyFormat(source));
        Assertions.assertFalse(Files.exists(target));

        Files.write(source, pkcs8);
        Assertions.assertFalse(SSLHandlerTrustStoreImpl.hasPkcs1PrivateKeyHeader(source));
        Assertions.assertEquals(SSLHandlerTrustStoreImpl.PemPrivateKeyFormat.OTHER,
            SSLHandlerTrustStoreImpl.detectPemPrivateKeyFormat(source));
        Assertions.assertFalse(Files.exists(target));

        Files.write(source, new byte[]{0x30, 0x03, 0x01, 0x01, 0x00});
        Assertions.assertFalse(SSLHandlerTrustStoreImpl.hasPkcs1PrivateKeyHeader(source));
        Assertions.assertFalse(Files.exists(target));
    }

    @Test
    public void onlyScansBoundedKeyPrefix() throws IOException {
        Path source = tempDir.resolve("client.key");
        Files.writeString(source, " ".repeat(64 * 1024) + "-----BEGIN RSA PRIVATE KEY-----");

        Assertions.assertFalse(SSLHandlerTrustStoreImpl.hasPkcs1PrivateKeyHeader(source));
    }

    @Test
    public void malformedPkcs1KeyDoesNotWriteTarget() throws IOException {
        Path source = tempDir.resolve("client.key");
        Path target = tempDir.resolve("client.pk8");
        Files.writeString(source, "-----BEGIN RSA PRIVATE KEY-----\nnot a key\n-----END RSA PRIVATE KEY-----");

        Assertions.assertThrows(IOException.class,
            () -> SSLHandlerTrustStoreImpl.savePkcs1PrivateKeyAsPkcs8File(Files.readAllBytes(source), target));
        Assertions.assertFalse(Files.exists(target));
    }

    @Test
    public void createsTemporaryPrivateKeyWithOwnerOnlyPermissions() throws IOException {
        Path keyFile = tempDir.resolve("postgres.pk8");
        byte[] key = {0, 1, 2, 3, 4};

        SSLHandlerTrustStoreImpl.writePrivateKeyFile(keyFile, key);

        Assertions.assertArrayEquals(key, Files.readAllBytes(keyFile));
        assertNoStagingFiles();
        if (supportsPosixPermissions()) {
            Assertions.assertEquals(
                PosixFilePermissions.fromString("rw-------"),
                Files.getPosixFilePermissions(keyFile)
            );
        }
    }

    @Test
    public void restrictsExistingPrivateKeyBeforeReplacingItsContents() throws IOException {
        Assumptions.assumeTrue(supportsPosixPermissions());
        Path keyFile = tempDir.resolve("postgres.pk8");
        Files.writeString(keyFile, "previous key");
        Files.setPosixFilePermissions(keyFile, PosixFilePermissions.fromString("rw-r--r--"));

        SSLHandlerTrustStoreImpl.writePrivateKeyFile(keyFile, new byte[]{5, 6});

        Assertions.assertArrayEquals(new byte[]{5, 6}, Files.readAllBytes(keyFile));
        assertNoStagingFiles();
        Assertions.assertEquals(
            PosixFilePermissions.fromString("rw-------"),
            Files.getPosixFilePermissions(keyFile)
        );
    }

    @Test
    public void refusesSymbolicLinkAsPrivateKeyTarget() throws IOException {
        Assumptions.assumeTrue(supportsPosixPermissions());
        Path targetFile = tempDir.resolve("target");
        Files.writeString(targetFile, "must not be changed");
        Path link = tempDir.resolve("postgres.pk8");
        Files.createSymbolicLink(link, targetFile);

        Assertions.assertThrows(
            IOException.class,
            () -> SSLHandlerTrustStoreImpl.writePrivateKeyFile(link, new byte[]{1, 2})
        );
        Assertions.assertEquals("must not be changed", Files.readString(targetFile));
        assertNoStagingFiles();
    }

    @Test
    public void refusesDirectoryAsPrivateKeyTarget() throws IOException {
        Assumptions.assumeTrue(supportsPosixPermissions());
        Path keyFile = tempDir.resolve("postgres.pk8");
        Files.createDirectory(keyFile);

        Assertions.assertThrows(
            IOException.class,
            () -> SSLHandlerTrustStoreImpl.writePrivateKeyFile(keyFile, new byte[]{7})
        );
        Assertions.assertTrue(Files.isDirectory(keyFile));
        assertNoStagingFiles();
    }

    private static void writeDerInteger(@NotNull ByteArrayOutputStream output, @NotNull BigInteger value) {
        byte[] bytes = value.toByteArray();
        output.write(0x02);
        writeDerLength(output, bytes.length);
        output.writeBytes(bytes);
    }

    private static void writeDerLength(@NotNull ByteArrayOutputStream output, int length) {
        if (length < 128) {
            output.write(length);
        } else if (length < 256) {
            output.write(0x81);
            output.write(length);
        } else {
            output.write(0x82);
            output.write(length >>> 8);
            output.write(length);
        }
    }

    private void assertNoStagingFiles() throws IOException {
        try (var entries = Files.list(tempDir)) {
            Assertions.assertFalse(entries.anyMatch(file ->
                file.getFileName().toString().startsWith(".dbeaver-private-")
            ));
        }
    }

    private boolean supportsPosixPermissions() throws IOException {
        return Files.getFileStore(tempDir).supportsFileAttributeView(PosixFileAttributeView.class);
    }
}
