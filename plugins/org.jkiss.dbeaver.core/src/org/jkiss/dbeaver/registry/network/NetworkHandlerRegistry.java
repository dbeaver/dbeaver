/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.registry.network;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NetworkHandlerRegistry
{
    private static NetworkHandlerRegistry instance = null;

    public synchronized static NetworkHandlerRegistry getInstance()
    {
        if (instance == null) {
            instance = new NetworkHandlerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<NetworkHandlerDescriptor> descriptors = new ArrayList<>();

    private NetworkHandlerRegistry(IExtensionRegistry registry)
    {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(NetworkHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                NetworkHandlerDescriptor formatterDescriptor = new NetworkHandlerDescriptor(ext);
                descriptors.add(formatterDescriptor);
            }
            descriptors.sort(Comparator.comparingInt(NetworkHandlerDescriptor::getOrder));
        }
    }

    public List<NetworkHandlerDescriptor> getDescriptors()
    {
        return descriptors;
    }
    
    public NetworkHandlerDescriptor getDescriptor(String id)
    {
        for (NetworkHandlerDescriptor descriptor : descriptors) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

    public List<NetworkHandlerDescriptor> getDescriptors(DBPDataSourceContainer dataSource) {
        List<NetworkHandlerDescriptor> result = new ArrayList<>();
        for (NetworkHandlerDescriptor d : descriptors) {
            if (!d.hasObjectTypes() || d.matches(dataSource.getDriver().getDataSourceProvider())) {
                result.add(d);
            }
        }
        return result;
    }
}
