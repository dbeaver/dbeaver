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
package org.jkiss.dbeaver.model.impl.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallCacheCleanerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void deletesOnlyOldVersionDirectories() throws IOException {
        Path cacheRoot = temporaryDirectory.resolve("install-data");
        Path product = cacheRoot.resolve("org.jkiss.dbeaver.product");
        Path currentVersion = product.resolve("26.2.5-202611151759");
        Path oldVersion = product.resolve("26.2.3-202610181708");
        Files.createDirectories(currentVersion);
        Files.createDirectories(oldVersion);
        Files.createDirectories(product.resolve("plugins").resolve("user-plugin"));
        Files.createDirectories(product.resolve("features").resolve("user-feature"));
        Files.createDirectories(product.resolve("p2").resolve("org.eclipse.equinox.p2.engine"));
        Files.createDirectories(product.resolve("26.2.2-invalid-build"));
        Files.writeString(oldVersion.resolve("cached-version-file"), "old cache");
        Files.writeString(product.resolve("plugins/user-plugin/plugin.jar"), "user plugin");
        Files.writeString(product.resolve("features/user-feature/feature.xml"), "user feature");
        Files.writeString(product.resolve("p2/org.eclipse.equinox.p2.engine/profile.dat"), "user profile");

        Instant cutoff = Instant.now();
        setLastModifiedRecursively(oldVersion, cutoff.minusSeconds(60));
        setLastModifiedRecursively(product.resolve("plugins"), cutoff.minusSeconds(60));
        setLastModifiedRecursively(product.resolve("features"), cutoff.minusSeconds(60));
        setLastModifiedRecursively(product.resolve("p2"), cutoff.minusSeconds(60));
        setLastModifiedRecursively(product.resolve("26.2.2-invalid-build"), cutoff.minusSeconds(60));

        List<IOException> errors = new ArrayList<>();
        InstallCacheCleaner.clean(cacheRoot, currentVersion, cutoff, errors::add);

        assertTrue(errors.isEmpty(), () -> "Unexpected cleanup errors: " + errors);
        assertFalse(Files.exists(oldVersion), "An old version cache should be removed");
        assertTrue(Files.isDirectory(currentVersion), "The current version must be preserved");
        assertTrue(
            Files.isRegularFile(product.resolve("plugins/user-plugin/plugin.jar")),
            "User plugins must be preserved"
        );
        assertTrue(
            Files.isRegularFile(product.resolve("features/user-feature/feature.xml")),
            "User features must be preserved"
        );
        assertTrue(
            Files.isRegularFile(product.resolve("p2/org.eclipse.equinox.p2.engine/profile.dat")),
            "The user p2 profile must be preserved"
        );
        assertTrue(Files.isDirectory(product.resolve("26.2.2-invalid-build")), "Non-version directories must be preserved");
    }

    private static void setLastModifiedRecursively(Path path, Instant lastModified) throws IOException {
        try (var paths = Files.walk(path)) {
            for (Path nested : paths.toList()) {
                Files.setLastModifiedTime(nested, FileTime.from(lastModified));
            }
        }
    }
}
