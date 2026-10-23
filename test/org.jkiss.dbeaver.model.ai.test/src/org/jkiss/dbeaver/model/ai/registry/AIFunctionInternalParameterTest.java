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
import org.jkiss.dbeaver.model.ai.AIFunctionParameterValueProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

class AIFunctionInternalParameterTest {
    @Test
    void parameterValueProviderIsCreatedOnlyWhenValuesAreRequested() throws Exception {
        IConfigurationElement config = Mockito.mock(IConfigurationElement.class);
        IContributor contributor = Mockito.mock(IContributor.class);
        Mockito.when(config.getContributor()).thenReturn(contributor);
        Mockito.when(contributor.getName()).thenReturn("test");
        Mockito.when(config.getAttribute("name")).thenReturn("inputFormat");
        Mockito.when(config.getAttribute("validValues")).thenReturn("csv,json");

        AIFunctionImplementationDescriptor implementation = Mockito.mock(AIFunctionImplementationDescriptor.class);
        Mockito.when(implementation.getParametersWithValueProvider()).thenReturn(Set.of("inputFormat"));
        AIFunctionParameterValueProvider provider = Mockito.mock(AIFunctionParameterValueProvider.class);
        Mockito.when(provider.getValidValues()).thenReturn(new String[] { "xml", "yaml" });
        Mockito.when(implementation.createParameterValueProvider("inputFormat")).thenReturn(provider);

        AIFunctionInternalParameter parameter = new AIFunctionInternalParameter(config, implementation);
        Mockito.verify(implementation, Mockito.never()).createParameterValueProvider("inputFormat");

        Assertions.assertArrayEquals(new String[] { "xml", "yaml" }, parameter.getValidValues());
        Assertions.assertArrayEquals(new String[] { "xml", "yaml" }, parameter.getValidValues());
        Mockito.verify(implementation, Mockito.times(1)).createParameterValueProvider("inputFormat");
    }
}
