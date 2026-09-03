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
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DriverLibraryPlatformTest {
    @Test
    void excludesOnlyMatchingPlatform() {
        IConfigurationElement config = Mockito.mock(IConfigurationElement.class);
        Mockito.when(config.getAttribute(RegistryConstants.ATTR_TYPE)).thenReturn("jar");
        Mockito.when(config.getAttribute(RegistryConstants.ATTR_PATH)).thenReturn("driver.jar");
        Mockito.when(config.getAttribute(RegistryConstants.ATTR_OS)).thenReturn("win32");
        Mockito.when(config.getAttribute(RegistryConstants.ATTR_ARCH)).thenReturn("aarch64");
        Mockito.when(config.getAttribute(RegistryConstants.ATTR_EXCLUDE)).thenReturn("true");

        DriverLibraryLocal library = new DriverLibraryLocal(null, config);

        Assertions.assertFalse(library.system.matches(new OSDescriptor("win32", "aarch64")));
        Assertions.assertTrue(library.system.matches(new OSDescriptor("win32", "x86_64")));
        Assertions.assertTrue(library.system.matches(new OSDescriptor("linux", "aarch64")));
    }
}
