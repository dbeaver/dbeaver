/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

public class SSHImplementationRegistry
{
    private static SSHImplementationRegistry instance = null;

    public synchronized static SSHImplementationRegistry getInstance()
    {
        if (instance == null) {
            instance = new SSHImplementationRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<SSHImplementationDescriptor> descriptors = new ArrayList<>();

    private SSHImplementationRegistry(IExtensionRegistry registry)
    {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(SSHImplementationDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                SSHImplementationDescriptor descriptor = new SSHImplementationDescriptor(ext);
                descriptors.add(descriptor);
            }
        }
    }

    public List<SSHImplementationDescriptor> getDescriptors() {
        return descriptors;
    }

    public SSHImplementationDescriptor getDescriptor(String id) {
        for (SSHImplementationDescriptor desc : descriptors) {
            if (desc.getId().equals(id)) {
                return desc;
            }
        }
        return null;
    }

}
