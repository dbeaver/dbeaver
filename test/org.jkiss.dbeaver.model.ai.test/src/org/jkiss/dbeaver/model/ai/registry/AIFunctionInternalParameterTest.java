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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIFunctionParameterValueProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

class AIFunctionInternalParameterTest {
    @Test
    void parameterValueProviderIsCreatedOnlyWhenValuesAreRequested() {
        RecordingValueProvider.instances.set(0);
        IConfigurationElement config = Mockito.mock(IConfigurationElement.class);
        IContributor contributor = Mockito.mock(IContributor.class);
        Mockito.when(config.getContributor()).thenReturn(contributor);
        Mockito.when(contributor.getName()).thenReturn("org.jkiss.dbeaver.model.ai");
        Mockito.when(config.getAttribute("name")).thenReturn("inputFormat");
        Mockito.when(config.getAttribute("validValues")).thenReturn("csv,json");

        IConfigurationElement providerConfig = Mockito.mock(IConfigurationElement.class);
        Mockito.when(providerConfig.getAttribute("parameter")).thenReturn("inputFormat");
        Mockito.when(providerConfig.getAttribute("class")).thenReturn(RecordingValueProvider.class.getName());
        IConfigurationElement implementationConfig = Mockito.mock(IConfigurationElement.class);
        Mockito.when(implementationConfig.getContributor()).thenReturn(contributor);
        Mockito.when(implementationConfig.getChildren("parameterTransformer")).thenReturn(new IConfigurationElement[0]);
        Mockito.when(implementationConfig.getChildren("parameterValueProvider"))
            .thenReturn(new IConfigurationElement[] { providerConfig });

        AIFunctionImplementationDescriptor implementation = new AIFunctionImplementationDescriptor(implementationConfig);
        AIFunctionInternalParameter parameter = new AIFunctionInternalParameter(config, implementation);
        Assertions.assertEquals(0, RecordingValueProvider.instances.get());

        Assertions.assertArrayEquals(new String[] { "xml", "yaml" }, parameter.getValidValues());
        Assertions.assertArrayEquals(new String[] { "xml", "yaml" }, parameter.getValidValues());
        Assertions.assertEquals(1, RecordingValueProvider.instances.get());
    }

    public static final class RecordingValueProvider implements AIFunctionParameterValueProvider {
        private static final AtomicInteger instances = new AtomicInteger();

        public RecordingValueProvider() {
            instances.incrementAndGet();
        }

        @Override
        @NotNull
        public String[] getValidValues() {
            return new String[] { "xml", "yaml" };
        }
    }
}
