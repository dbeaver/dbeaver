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
package org.jkiss.dbeaver.launcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class DBeaverLauncherTest {
    @TempDir
    Path directory;

    @Test
    void rejectsDirectoryAsSplash() throws IOException {
        Path archive = directory.resolve("splash.jar");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("splash.png/"));
            zip.closeEntry();
        }

        String previous = System.getProperty("osgi.configuration.area");
        System.setProperty("osgi.configuration.area", directory.toUri().toString());
        try {
            Assertions.assertNull(new DBeaverLauncher().extractFromJAR(archive.toString(), "splash.png"));
        } finally {
            restoreConfigurationArea(previous);
        }
    }

    @Test
    void extractsSplashAndReusesCache() throws IOException {
        Path archive = directory.resolve("splash.jar");
        byte[] content = {1, 2, 3};
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("splash.png"));
            zip.write(content);
            zip.closeEntry();
        }

        String previous = System.getProperty("osgi.configuration.area");
        System.setProperty("osgi.configuration.area", directory.toUri().toString());
        try {
            DBeaverLauncher launcher = new DBeaverLauncher();
            String extracted = launcher.extractFromJAR(archive.toString(), "splash.png");
            Assertions.assertNotNull(extracted);
            Assertions.assertArrayEquals(content, Files.readAllBytes(Path.of(extracted)));
            Files.delete(archive);
            Assertions.assertEquals(extracted, launcher.extractFromJAR(archive.toString(), "splash.png"));
        } finally {
            restoreConfigurationArea(previous);
        }
    }

    @Test
    void rejectsSplashOutsideCacheDirectory() throws IOException {
        Path archive = directory.resolve("splash.jar");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("../../outside.png"));
            zip.write(1);
            zip.closeEntry();
        }

        String previous = System.getProperty("osgi.configuration.area");
        System.setProperty("osgi.configuration.area", directory.toUri().toString());
        try {
            Assertions.assertNull(new DBeaverLauncher().extractFromJAR(archive.toString(), "../../outside.png"));
            Assertions.assertFalse(Files.exists(directory.resolve("outside.png")));
        } finally {
            restoreConfigurationArea(previous);
        }
    }

    private static void restoreConfigurationArea(String previous) {
        if (previous == null) {
            System.clearProperty("osgi.configuration.area");
        } else {
            System.setProperty("osgi.configuration.area", previous);
        }
    }
}
