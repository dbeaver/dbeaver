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
package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class DriverLibraryPlatformTest {
    @TempDir
    Path directory;

    @Test
    void excludesOnlyMatchingPlatform() {
        IConfigurationElement config = Mockito.mock(IConfigurationElement.class);
        Mockito.when(config.getAttribute(RegistryConstants.ATTR_TYPE)).thenReturn("jar");
        Mockito.when(config.getAttribute(RegistryConstants.ATTR_PATH)).thenReturn("driver.jar");
        Mockito.when(config.getAttribute(RegistryConstants.ATTR_OS)).thenReturn("win32");
        Mockito.when(config.getAttribute(RegistryConstants.ATTR_ARCH)).thenReturn("aarch64");
        Mockito.when(config.getAttribute(RegistryConstants.ATTR_EXCLUDE)).thenReturn("true");

        TestDriverLibrary library = new TestDriverLibrary(config);

        Assertions.assertFalse(library.matches(new OSDescriptor("win32", "aarch64")));
        Assertions.assertTrue(library.matches(new OSDescriptor("win32", "x86_64")));
        Assertions.assertTrue(library.matches(new OSDescriptor("linux", "aarch64")));
    }

    @Test
    void keepsExtractedLibrariesInDedicatedCacheDirectory() throws Exception {
        Path archive = createArchive("driver.zip");
        Path cache = directory.resolve("drivers/zip-cache");

        List<Path> libraries = extractZipArchives(List.of(archive), cache);

        Path extracted = cache.resolve("driver/outside-file.jar");
        Assertions.assertEquals(List.of(archive, extracted), libraries);
        Assertions.assertTrue(Files.exists(extracted));
        Assertions.assertFalse(Files.exists(directory.resolve("drivers/outside-file.jar")));
    }

    @Test
    void rejectsArchiveNamesResolvingToCacheRootOrParent() throws Exception {
        Path cache = directory.resolve("drivers/zip-cache");
        for (String name : List.of("...zip", ".zip")) {
            Path archive = createArchive(name);

            Assertions.assertEquals(List.of(archive), extractZipArchives(List.of(archive), cache));
        }
        Assertions.assertFalse(Files.exists(cache.resolve("outside-file.jar")));
        Assertions.assertFalse(Files.exists(directory.resolve("drivers/outside-file.jar")));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static List<Path> extractZipArchives(@NotNull List<Path> archives, @NotNull Path cache) throws Exception {
        // The test bundle has a separate class loader, so package-private access requires reflection.
        Method method = DriverUtils.class.getDeclaredMethod("extractZipArchives", List.class, Path.class);
        method.setAccessible(true);
        return (List<Path>) method.invoke(null, archives, cache);
    }

    @Test
    void rejectsEntryPathsOutsideArchiveCache() throws Exception {
        Path cache = directory.resolve("drivers/zip-cache");
        Path outside = directory.resolve("outside.jar");
        for (String entry : List.of("../outside.jar", "..\\outside.jar", outside.toString(), "C:/outside.jar")) {
            Path archive = createArchive("driver.zip", entry);
            Assertions.assertEquals(List.of(archive), extractZipArchives(List.of(archive), cache));
        }
        Assertions.assertFalse(Files.exists(outside));
        Assertions.assertFalse(Files.exists(cache.resolve("outside.jar")));
    }

    @Test
    void extractsNestedLibrariesAndReusesRegularCachedFiles() throws Exception {
        Path archive = createArchive("driver.zip", "lib/driver.jar", "lib/nested.zip", "ignored.txt");
        Path cache = directory.resolve("drivers/zip-cache");
        Path jar = cache.resolve("driver/lib/driver.jar");
        Path zip = cache.resolve("driver/lib/nested.zip");
        Assertions.assertEquals(List.of(archive, jar, zip), extractZipArchives(List.of(archive), cache));
        Assertions.assertArrayEquals(new byte[]{1}, Files.readAllBytes(jar));
        Assertions.assertArrayEquals(new byte[]{1}, Files.readAllBytes(zip));
        Assertions.assertFalse(Files.exists(cache.resolve("driver/ignored.txt")));
        Files.write(jar, new byte[]{2});
        Assertions.assertEquals(List.of(archive, jar, zip), extractZipArchives(List.of(archive), cache));
        Assertions.assertArrayEquals(new byte[]{2}, Files.readAllBytes(jar));
    }

    @Test
    void stopsAtClassFiles() throws Exception {
        Path archive = createArchive("driver.zip", "Driver.class", "lib/driver.jar");
        Path cache = directory.resolve("drivers/zip-cache");
        Assertions.assertEquals(List.of(archive), extractZipArchives(List.of(archive), cache));
        Assertions.assertFalse(Files.exists(cache));
    }

    @Test
    void rejectsSymlinkedCacheDirectoriesAndFiles() throws Exception {
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Path existing = Files.write(outside.resolve("outside-file.jar"), new byte[]{2});
        Path archive = createArchive("driver.zip");
        Path cache = Files.createDirectories(directory.resolve("drivers/zip-cache"));
        Path archiveCache = cache.resolve("driver");
        Files.createSymbolicLink(archiveCache, outside);
        Assertions.assertEquals(List.of(archive), extractZipArchives(List.of(archive), cache));
        Files.delete(archiveCache);
        Files.createDirectory(archiveCache);
        Files.createSymbolicLink(archiveCache.resolve("outside-file.jar"), existing);
        Assertions.assertEquals(List.of(archive), extractZipArchives(List.of(archive), cache));
        Assertions.assertArrayEquals(new byte[]{2}, Files.readAllBytes(existing));
    }

    @NotNull
    private Path createArchive(@NotNull String name) throws IOException {
        return createArchive(name, "outside-file.jar");
    }

    @NotNull
    private Path createArchive(@NotNull String name, @NotNull String... entries) throws IOException {
        Path archive = directory.resolve(name);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (String entry : entries) {
                output.putNextEntry(new ZipEntry(entry));
                output.write(1);
                output.closeEntry();
            }
        }
        return archive;
    }

    private static class TestDriverLibrary extends DriverLibraryLocal {
        private TestDriverLibrary(@NotNull IConfigurationElement config) {
            super(null, config);
        }

        private boolean matches(OSDescriptor os) {
            return system.matches(os);
        }
    }
}
