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

    @NotNull
    private Path createArchive(@NotNull String name) throws IOException {
        Path archive = directory.resolve(name);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            output.putNextEntry(new ZipEntry("outside-file.jar"));
            output.write(1);
            output.closeEntry();
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
