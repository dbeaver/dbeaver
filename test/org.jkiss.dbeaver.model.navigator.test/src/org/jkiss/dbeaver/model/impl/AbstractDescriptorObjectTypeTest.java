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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;

class AbstractDescriptorObjectTypeTest {
    @Test
    void forcedTypeCheckLoadsBundleOnlyForMatchingHierarchy() throws Exception {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(bundle.getState()).thenReturn(Bundle.RESOLVED);
        Mockito.doReturn(Runnable.class).when(bundle).loadClass("java.lang.Runnable");
        IConfigurationElement config = Mockito.mock(IConfigurationElement.class);
        Mockito.when(config.getAttribute("name")).thenReturn("java.lang.Runnable");
        Mockito.when(config.getAttribute("forceCheck")).thenReturn("true");

        AbstractDescriptor descriptor = new AbstractDescriptor("test") {
            @Override
            @NotNull
            public Bundle getContributorBundle() {
                return bundle;
            }
        };
        AbstractDescriptor.ObjectType type = descriptor.new ObjectType(config);

        Assertions.assertFalse(type.matchesType(String.class));
        Mockito.verify(bundle, Mockito.never()).loadClass(Mockito.anyString());

        Assertions.assertTrue(type.matchesType(Thread.class));
        Mockito.verify(bundle).loadClass("java.lang.Runnable");

        // Matching names still require the contributor's class to be assignable (e.g., across bundle classloaders).
        Mockito.doReturn(Comparable.class).when(bundle).loadClass("java.lang.Runnable");
        Assertions.assertFalse(descriptor.new ObjectType(config).matchesType(Thread.class));
    }
}
