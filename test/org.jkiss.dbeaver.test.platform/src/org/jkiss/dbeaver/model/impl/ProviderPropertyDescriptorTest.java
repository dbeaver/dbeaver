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
package org.jkiss.dbeaver.model.impl;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ProviderPropertyDescriptorTest {
    @Test
    public void mainPlacementAndRequiredAreIndependentMetadata() {
        ProviderPropertyDescriptor property = new ProviderPropertyDescriptor(
            "Connection", configuration("tenant", true, "main,custom", null)
        );

        assertEquals("tenant", property.getId());
        assertEquals("Tenant", property.getDisplayName());
        assertEquals(String.class, property.getDataType());
        assertTrue(property.isRequired());
        assertTrue(property.hasFeature("main"));
        assertArrayEquals(new String[]{"main", "custom", "required"}, property.getFeatures());
        assertEquals(Set.of(DBPDriverConfigurationType.MANUAL, DBPDriverConfigurationType.URL), property.getConfigurationTypes());
    }

    @Test
    public void mainPlacementDoesNotImplyRequired() {
        ProviderPropertyDescriptor property = new ProviderPropertyDescriptor(
            "Connection", configuration("tenant", false, "main", null)
        );

        assertFalse(property.isRequired());
        assertArrayEquals(new String[]{"main"}, property.getFeatures());
    }

    @Test
    public void propertiesWithoutFeaturesRetainDefaults() {
        ProviderPropertyDescriptor property = new ProviderPropertyDescriptor(
            "Connection", configuration("timeout", false, null, null)
        );

        assertFalse(property.isRequired());
        assertNull(property.getFeatures());
        assertFalse(property.hasFeature("main"));
        assertEquals(Set.of(DBPDriverConfigurationType.MANUAL, DBPDriverConfigurationType.URL), property.getConfigurationTypes());
    }

    @Test
    public void propertyGroupsPreservePlacementAndConfigurationTypes() {
        IConfigurationElement group = Mockito.mock(IConfigurationElement.class);
        Mockito.when(group.getName()).thenReturn("propertyGroup");
        Mockito.when(group.getAttribute("label")).thenReturn("Connection");
        IConfigurationElement[] entries = {
            configuration("tenant", true, "main", "URL"),
            configuration("timeout", false, null, null)
        };
        Mockito.when(group.getChildren("property")).thenReturn(entries);

        var properties = ProviderPropertyDescriptor.extractProviderProperties(group);

        assertEquals(2, properties.size());
        assertEquals("Connection", properties.get(0).getCategory());
        assertEquals("Connection", properties.get(1).getCategory());
        assertTrue(properties.get(0).hasFeature("main"));
        assertEquals(Set.of(DBPDriverConfigurationType.URL), properties.get(0).getConfigurationTypes());
        assertFalse(properties.get(1).hasFeature("main"));
    }

    @NotNull
    private static IConfigurationElement configuration(
        @NotNull String id,
        boolean required,
        @Nullable String features,
        @Nullable String configurationTypes
    ) {
        IConfigurationElement config = Mockito.mock(IConfigurationElement.class);
        Mockito.when(config.getAttribute("id")).thenReturn(id);
        Mockito.when(config.getAttribute("label")).thenReturn("Tenant");
        Mockito.when(config.getAttribute("type")).thenReturn("string");
        Mockito.when(config.getAttribute("required")).thenReturn(Boolean.toString(required));
        Mockito.when(config.getAttribute("features")).thenReturn(features);
        Mockito.when(config.getAttribute("supportedConfigurationTypes")).thenReturn(configurationTypes);
        return config;
    }
}
