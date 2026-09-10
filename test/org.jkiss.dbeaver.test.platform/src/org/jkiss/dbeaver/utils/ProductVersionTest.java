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
package org.jkiss.dbeaver.utils;

import org.jkiss.dbeaver.registry.updater.VersionDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProductVersionTest extends DBeaverUnitTest {

    @Test
    public void comparePublicVersions() {
        assertTrue(VersionUtils.compareVersions("26.0.0", "26.0.0.1") < 0);
        assertTrue(VersionUtils.compareVersions("26.0.0.1", "26.0.0.2") < 0);
        assertTrue(VersionUtils.compareVersions("26.0.1", "26.0.0.9") > 0);
    }

    @Test
    public void readPublicVersionFromProductMarker(@TempDir Path tempDir) throws IOException {
        Path marker = tempDir.resolve(".eclipseproduct");
        for (String productId : new String[] {"org.jkiss.dbeaver.product", "com.dbeaver.enterprise", "org.dbvr.product"}) {
            Files.writeString(marker, "id=" + productId + "\nversion=26.0.0.1\n");
            assertEquals("26.0.0.1", readProductVersion(marker, "26.0.0"));
        }
    }

    @Test
    public void fallBackForInvalidProductMarker(@TempDir Path tempDir) throws IOException {
        Path marker = tempDir.resolve(".eclipseproduct");
        Files.writeString(marker, "id=org.jkiss.dbeaver.product\nversion=${dbeaver-version}\n");

        assertEquals("26.0.0", readProductVersion(marker, "26.0.0"));
        assertEquals("26.0.0", readProductVersion(tempDir.resolve("missing"), "26.0.0"));
    }

    @Test
    public void rejectHostEclipseProductMarker(@TempDir Path tempDir) throws IOException {
        Path marker = tempDir.resolve(".eclipseproduct");
        Files.writeString(marker, "id=org.eclipse.platform.ide\nversion=26.0.0.1\n");

        assertEquals("26.0.0", readProductVersion(marker, "26.0.0"));
    }

    @Test
    public void preservePlainVersionAndExposeReleaseVersion(@TempDir Path tempDir) throws IOException {
        Path versionFile = tempDir.resolve("version.xml");
        Files.writeString(versionFile, "<version><number>26.0.0.1</number><date>09.09.2026</date></version>");

        VersionDescriptor descriptor = new VersionDescriptor(DBWorkbench.getPlatform(), versionFile.toUri().toString());
        assertEquals("26.0.0", descriptor.getPlainVersion());
        assertEquals("26.0.0.1", descriptor.getReleaseVersion());
    }

    private static String readProductVersion(Path marker, String fallback) {
        try {
            var method = GeneralUtils.class.getDeclaredMethod("readProductVersion", Path.class, String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, marker, fallback);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new AssertionError(e);
        }
    }
}
