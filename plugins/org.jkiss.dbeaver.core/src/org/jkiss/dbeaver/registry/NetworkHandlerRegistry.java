/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

import java.util.ArrayList;
import java.util.List;

public class NetworkHandlerRegistry
{
    private final List<NetworkHandlerDescriptor> descriptors = new ArrayList<NetworkHandlerDescriptor>();

    public NetworkHandlerRegistry(IExtensionRegistry registry)
    {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(NetworkHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                NetworkHandlerDescriptor formatterDescriptor = new NetworkHandlerDescriptor(ext);
                descriptors.add(formatterDescriptor);
            }
        }
    }

    public void dispose()
    {
        this.descriptors.clear();
    }

    public List<NetworkHandlerDescriptor> getDescriptors()
    {
        return descriptors;
    }
}
