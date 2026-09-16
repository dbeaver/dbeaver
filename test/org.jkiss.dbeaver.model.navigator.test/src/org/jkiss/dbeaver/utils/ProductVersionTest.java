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

import org.jkiss.code.NotNull;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProductVersionTest extends DBeaverUnitTest {

    @Test
    public void readsSupportedProductVersions(@NotNull @TempDir Path tempDir) throws IOException {
        Path marker = tempDir.resolve(".eclipseproduct");
        String[] productIds = {
            "org.jkiss.dbeaver.product",
            "com.dbeaver.app.enterprise.product",
            "org.dbvr.app.ce.product",
            "io.cloudbeaver.product.ce"
        };
        for (String productId : productIds) {
            Files.writeString(marker, "id=" + productId + "\nversion=26.2.2.10\n");
            Assertions.assertEquals("26.2.2.10", GeneralUtils.readProductVersion(marker, "26.2.2"));
        }

        Files.writeString(marker, "id=org.jkiss.dbeaver.product\nversion=26.2.2\n");
        Assertions.assertEquals("26.2.2", GeneralUtils.readProductVersion(marker, "26.2.1"));
    }

    @Test
    public void fallsBackForInvalidProductMarkers(@NotNull @TempDir Path tempDir) throws IOException {
        Path marker = tempDir.resolve(".eclipseproduct");
        String fallback = "26.2.2";
        String[] invalidMarkers = {
            "id=org.eclipse.platform.ide\nversion=26.2.2.10\n",
            "id=org.jkiss.dbeaver.product\nversion=${dbeaver-version}\n",
            "id=org.jkiss.dbeaver.product\nversion=26.2\n",
            "id=org.jkiss.dbeaver.product\nversion=26.2.2.10.1\n",
            "id=org.jkiss.dbeaver.product\nversion=26.2.2.qualifier\n"
        };
        for (String markerContents : invalidMarkers) {
            Files.writeString(marker, markerContents);
            Assertions.assertEquals(fallback, GeneralUtils.readProductVersion(marker, fallback));
        }
        Assertions.assertEquals(fallback, GeneralUtils.readProductVersion(tempDir.resolve("missing"), fallback));
    }

    @Test
    public void comparesPublicVersionsNumerically() {
        Assertions.assertTrue(VersionUtils.compareVersions("26.2.2.10", "26.2.2.9") > 0);
        Assertions.assertTrue(VersionUtils.compareVersions("26.2.2.9", "26.2.2.10") < 0);
        Assertions.assertTrue(VersionUtils.compareVersions("26.2.3", "26.2.2.10") > 0);
    }

}
