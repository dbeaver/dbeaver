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
package org.jkiss.dbeaver.model.ai.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionPurpose;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

class AIFunctionInternalDescriptorTest {
    @Test
    void exposesDamFunctionsOnlyToDataDamMcp() {
        var extensions = Mockito.mock(IExtensionRegistry.class);
        var platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        IConfigurationElement[] definitions = Arrays.stream(AIFunctionPurpose.values()).map(purpose -> {
            var config = configuration();
            Mockito.when(config.getName()).thenReturn("function");
            Mockito.when(config.getAttribute("id")).thenReturn(purpose.name());
            Mockito.when(config.getAttribute("purpose")).thenReturn(purpose.name());
            Mockito.when(config.getAttribute("class")).thenReturn("test.Function");
            return config;
        }).toArray(IConfigurationElement[]::new);
        Mockito.when(extensions.getConfigurationElementsFor(AIFunctionInternalDescriptor.EXTENSION_ID)).thenReturn(definitions);
        Mockito.when(extensions.getConfigurationElementsFor(AIFunctionImplementationDescriptor.EXTENSION_ID))
            .thenReturn(new IConfigurationElement[0]);
        try (var eclipse = Mockito.mockStatic(Platform.class);
             var workbench = Mockito.mockStatic(DBWorkbench.class)) {
            eclipse.when(Platform::getExtensionRegistry).thenReturn(extensions);
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            var registry = new AIFunctionInternalRegistry(Mockito.mock(AIToolboxInternalDescriptor.class));

            Assertions.assertEquals(List.of("TOOL", "ALL"), functionIds(registry, AIFunctionPurpose.TOOL));
            Assertions.assertEquals(List.of("MCP", "ALL"), functionIds(registry, AIFunctionPurpose.MCP));
            Assertions.assertEquals(List.of("MCP", "DAM", "ALL"), functionIds(registry, AIFunctionPurpose.DAM));
            Assertions.assertEquals(List.of("TOOL", "MCP", "ALL"), functionIds(registry, AIFunctionPurpose.ALL));
        }
    }

    @NotNull
    private List<String> functionIds(@NotNull AIFunctionInternalRegistry registry, @NotNull AIFunctionPurpose purpose) {
        return registry.getAllFunctions(purpose).stream().map(AIFunctionDescriptor::getId).toList();
    }

    @Test
    void usesMcpDefaultsWhenXmlOmitsHints() {
        var descriptor = new AIFunctionInternalDescriptor(Mockito.mock(AIToolboxInternalDescriptor.class), configuration());
        Assertions.assertFalse(descriptor.isReadOnlyHint());
        Assertions.assertTrue(descriptor.isDestructiveHint());
        Assertions.assertFalse(descriptor.isIdempotentHint());
    }

    @Test
    void readsExplicitHintsFromXml() {
        var config = configuration();
        Mockito.when(config.getAttribute("readOnlyHint")).thenReturn("true");
        Mockito.when(config.getAttribute("destructiveHint")).thenReturn("false");
        Mockito.when(config.getAttribute("idempotentHint")).thenReturn("true");
        var descriptor = new AIFunctionInternalDescriptor(Mockito.mock(AIToolboxInternalDescriptor.class), config);
        Assertions.assertTrue(descriptor.isReadOnlyHint());
        Assertions.assertFalse(descriptor.isDestructiveHint());
        Assertions.assertTrue(descriptor.isIdempotentHint());
    }

    @NotNull
    private IConfigurationElement configuration() {
        var config = Mockito.mock(IConfigurationElement.class);
        var contributor = Mockito.mock(IContributor.class);
        Mockito.when(config.getContributor()).thenReturn(contributor);
        Mockito.when(contributor.getName()).thenReturn("org.jkiss.dbeaver.model.ai");
        Mockito.when(config.getAttribute("id")).thenReturn("query");
        Mockito.when(config.getAttribute("name")).thenReturn("Query");
        Mockito.when(config.getChildren("parameter")).thenReturn(new IConfigurationElement[0]);
        return config;
    }
}
