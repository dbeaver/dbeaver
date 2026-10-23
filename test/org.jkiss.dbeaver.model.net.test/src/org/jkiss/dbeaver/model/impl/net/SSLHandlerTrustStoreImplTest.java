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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;

public class SSLHandlerTrustStoreImplTest {

    @TempDir
    Path tempDir;

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
