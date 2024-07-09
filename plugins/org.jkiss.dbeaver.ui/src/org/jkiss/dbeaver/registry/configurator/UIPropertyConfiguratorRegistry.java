/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.configurator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.HashMap;
import java.util.Map;

public class UIPropertyConfiguratorRegistry {
    private static UIPropertyConfiguratorRegistry instance = null;

    public synchronized static UIPropertyConfiguratorRegistry getInstance() {
        if (instance == null) {
            instance = new UIPropertyConfiguratorRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final Map<String, UIPropertyConfiguratorDescriptor> descriptors = new HashMap<>();

    private UIPropertyConfiguratorRegistry(IExtensionRegistry registry) {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(UIPropertyConfiguratorDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                UIPropertyConfiguratorDescriptor descriptor = new UIPropertyConfiguratorDescriptor(ext);
                descriptors.put(descriptor.getObjectType(), descriptor);
            }
        }
    }

    public UIPropertyConfiguratorDescriptor getDescriptor(Object object) {
        for (Class<?> theClass = object.getClass(); theClass != Object.class; theClass = theClass.getSuperclass()) {
            UIPropertyConfiguratorDescriptor descriptor = descriptors.get(theClass.getName());
            if (descriptor != null) {
                return descriptor;
            }
        }
        return null;
    }

    public UIPropertyConfiguratorDescriptor getDescriptor(String className) {
        return descriptors.get(className);
    }

}
