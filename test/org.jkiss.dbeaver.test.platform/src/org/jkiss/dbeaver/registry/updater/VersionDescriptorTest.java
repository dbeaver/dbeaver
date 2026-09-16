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
package org.jkiss.dbeaver.registry.updater;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VersionDescriptorTest extends DBeaverUnitTest {

    @Test
    public void exposesFullRemoteVersion(@NotNull @TempDir Path tempDir) throws IOException {
        Path versionFile = tempDir.resolve("version.xml");
        Files.writeString(versionFile, "<version><number>26.2.2.10</number><date>15.09.2026</date></version>");

        VersionDescriptor descriptor = new VersionDescriptor(DBWorkbench.getPlatform(), versionFile.toUri().toString());
        Assertions.assertEquals("26.2.2.10", descriptor.getPlainVersion());
    }

    @Test
    public void comparesPublicVersionComponentsNumerically() {
        Assertions.assertEquals(0, VersionDescriptor.comparePublicVersions("26.2.2", "26.2.2.0"));
        Assertions.assertTrue(VersionDescriptor.comparePublicVersions("26.2.2.10", "26.2.2.2") > 0);
    }
}
