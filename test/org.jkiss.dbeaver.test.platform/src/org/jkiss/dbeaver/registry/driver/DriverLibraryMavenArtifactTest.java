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
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.registry.maven.MavenArtifact;
import org.jkiss.dbeaver.registry.maven.MavenArtifactVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

class DriverLibraryMavenArtifactTest {
    @Test
    void unresolvedVersionHasNoLocalFile() {
        TestDriverLibrary library = new TestDriverLibrary();
        MavenArtifactVersion version = Mockito.spy(MavenArtifactVersion.createInvalidVersion(
            Mockito.mock(MavenArtifact.class), "{26\\..*}"));
        library.setVersion(version);
        Mockito.doThrow(new InvalidPathException("cdata/box-jdbc-{26\\..*}.jar", "Illegal char <*>"))
            .when(version).getCacheFile();

        Assertions.assertTrue(library.isInvalidLibrary());
        Assertions.assertNull(library.getLocalFile());
    }

    @Test
    void resolvedVersionReturnsCacheFile() {
        TestDriverLibrary library = new TestDriverLibrary();
        MavenArtifactVersion version = Mockito.mock(MavenArtifactVersion.class);
        library.setVersion(version);
        Path cacheFile = Path.of("cdata", "box-jdbc-26.0.0.jar");
        Mockito.when(version.getCacheFile()).thenReturn(cacheFile);

        Assertions.assertFalse(library.isInvalidLibrary());
        Assertions.assertEquals(cacheFile, library.getLocalFile());
    }

    @Test
    void missingVersionHasNoLocalFile() {
        Assertions.assertNull(new TestDriverLibrary().getLocalFile());
    }

    private static class TestDriverLibrary extends DriverLibraryMavenArtifact {
        private TestDriverLibrary() {
            super(null, createConfiguration());
        }

        private void setVersion(MavenArtifactVersion version) {
            localVersion = version;
        }

        private static IConfigurationElement createConfiguration() {
            IConfigurationElement config = Mockito.mock(IConfigurationElement.class);
            Mockito.when(config.getAttribute(RegistryConstants.ATTR_TYPE)).thenReturn("jar");
            Mockito.when(config.getAttribute(RegistryConstants.ATTR_PATH)).thenReturn("maven:/cdata:box-jdbc:{26\\..*}");
            return config;
        }
    }
}
