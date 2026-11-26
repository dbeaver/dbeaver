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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.registry.ResourceTypeDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpecialFolderTest {

    @Test
    void localizedLabelsDoNotChangePathsOrResourceTypeNames() {
        IConfigurationElement config = configuration();
        when(config.getAttribute("name")).thenReturn("SQL-скрипт");
        IConfigurationElement[] roots = {
            root("Scripts", "Скрипты"), root("Legacy", null)
        };
        when(config.getChildren("root")).thenReturn(roots);

        ResourceTypeDescriptor type = new ResourceTypeDescriptor(config);
        assertEquals("SQL-скрипт", type.getName());
        assertEquals("Scripts", type.getDefaultRoot(null));
        assertEquals("Скрипты", type.getRootFolderLabel("Scripts"));
        assertNull(type.getRootFolderLabel("Legacy"));
        assertNull(type.getRootFolderLabel("My Scripts"));
        assertNull(type.getRootFolderLabel("Scripts/Scripts"));
        assertNull(type.getRootFolderLabel("Скрипты"));
    }

    @Test
    void eachDeclaredRootHasItsOwnOptionalLabel() {
        IConfigurationElement config = configuration();
        IConfigurationElement defaultRoot = root("Diagrams", "Диаграммы");
        when(defaultRoot.getAttribute("default")).thenReturn("true");
        IConfigurationElement[] roots = {
            root("Legacy", "Старые диаграммы"), defaultRoot
        };
        when(config.getChildren("root")).thenReturn(roots);

        ResourceTypeDescriptor type = new ResourceTypeDescriptor(config);
        assertEquals("Diagrams", type.getDefaultRoot(null));
        assertEquals("Диаграммы", type.getRootFolderLabel("Diagrams"));
        assertEquals("Старые диаграммы", type.getRootFolderLabel("Legacy"));
    }

    @NotNull
    private static IConfigurationElement configuration() {
        IConfigurationElement config = mock(IConfigurationElement.class);
        when(config.getContributor()).thenReturn(() -> "org.jkiss.dbeaver.model.navigator.test");
        return config;
    }

    @NotNull
    private static IConfigurationElement root(@NotNull String folder, @Nullable String label) {
        IConfigurationElement root = mock(IConfigurationElement.class);
        when(root.getAttribute("folder")).thenReturn(folder);
        when(root.getAttribute("label")).thenReturn(label);
        return root;
    }
}
