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
package org.jkiss.dbeaver.model.net.ssh.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.List;

public class SSHSessionControllerRegistry
{
    private static SSHSessionControllerRegistry instance = null;

    public synchronized static SSHSessionControllerRegistry getInstance()
    {
        if (instance == null) {
            instance = new SSHSessionControllerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<SSHSessionControllerDescriptor> descriptors = new ArrayList<>();

    private SSHSessionControllerRegistry(IExtensionRegistry registry)
    {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(SSHSessionControllerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                SSHSessionControllerDescriptor descriptor = new SSHSessionControllerDescriptor(ext);
                descriptors.add(descriptor);
            }
        }
    }

    public List<SSHSessionControllerDescriptor> getDescriptors() {
        return descriptors;
    }

    public SSHSessionControllerDescriptor getDescriptor(String id) {
        for (SSHSessionControllerDescriptor desc : descriptors) {
            if (desc.getId().equals(id)) {
                return desc;
            }
        }
        return null;
    }

}
