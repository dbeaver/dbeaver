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
package org.jkiss.dbeaver.ext.cdata;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchyReader;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverInfo;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverTier;
import org.jkiss.dbeaver.ext.cdata.registry.CDataResolvedDriver;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class CDataConnectionHierarchyReaderTest extends DBeaverUnitTest {
    @TempDir
    Path directory;

    @Test
    public void deregistersIsolatedDriversAfterSuccessfulAndFailedReads() throws Exception {
        String className = "org.jkiss.dbeaver.ext.cdata.CDataHierarchyTestDriver";
        String resourceName = className.replace('.', '/') + ".class";
        Path jar = directory.resolve("driver.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar));
             var input = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            Assertions.assertNotNull(input);
            output.putNextEntry(new JarEntry(resourceName));
            input.transferTo(output);
            output.closeEntry();
        }
        var descriptor = Mockito.mock(CDataDriverDescriptor.class);
        Mockito.when(descriptor.resolveDriver(Mockito.any()))
            .thenReturn(new CDataResolvedDriver(jar, className, directory.resolve("test.lic")));
        Mockito.when(descriptor.getDriverInfo()).thenReturn(new CDataDriverInfo("test", "test", "Test", 2026,
            CDataDriverTier.PROFESSIONAL, "https://example.com/", "test", null));
        AtomicInteger registrations = new AtomicInteger();
        System.getProperties().put("cdata.hierarchy.test.registrations", registrations);
        try {
            for (int attempt = 0; attempt < 3; attempt++) {
                CDataConnectionHierarchyReader.read(new VoidProgressMonitor(), descriptor);
                Assertions.assertEquals(0, registrations.get(), "Driver registration retained its class loader");
            }
            System.setProperty("cdata.hierarchy.test.fail", "true");
            Assertions.assertThrows(DBException.class, () -> CDataConnectionHierarchyReader.read(new VoidProgressMonitor(), descriptor));
            Assertions.assertEquals(0, registrations.get(), "Failed hierarchy loading retained its class loader");
        } finally {
            System.clearProperty("cdata.hierarchy.test.fail");
            System.getProperties().remove("cdata.hierarchy.test.registrations");
        }
    }
}
