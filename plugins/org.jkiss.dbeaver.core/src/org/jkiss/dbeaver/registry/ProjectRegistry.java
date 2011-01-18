/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

import java.util.ArrayList;
import java.util.List;

public class ProjectRegistry
{
    static final Log log = LogFactory.getLog(ProjectRegistry.class);

    private final List<ResourceHandlerDescriptor> resourceHandlers = new ArrayList<ResourceHandlerDescriptor>();

    public ProjectRegistry()
    {
    }

    public void loadExtensions(IExtensionRegistry registry)
    {
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ResourceHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                ResourceHandlerDescriptor handlerDescriptor = new ResourceHandlerDescriptor(ext);
                resourceHandlers.add(handlerDescriptor);
            }
        }
    }

    public void dispose()
    {
        for (ResourceHandlerDescriptor handlerDescriptor : this.resourceHandlers) {
            handlerDescriptor.dispose();
        }
        this.resourceHandlers.clear();
    }

    public List<ResourceHandlerDescriptor> getResourceHandlers()
    {
        return resourceHandlers;
    }

    public List<ResourceHandlerDescriptor> getResourceHandlers(String resourceType)
    {
        List<ResourceHandlerDescriptor> handlerDescriptors = new ArrayList<ResourceHandlerDescriptor>();
        for (ResourceHandlerDescriptor handlerDescriptor : this.resourceHandlers) {
            if (handlerDescriptor.getResourceType().equals(resourceType)) {
                handlerDescriptors.add(handlerDescriptor);
            }
        }
        return handlerDescriptors;
    }

    public ResourceHandlerDescriptor getResourceHandler(String resourceType)
    {
        List<ResourceHandlerDescriptor> handlerDescriptors = new ArrayList<ResourceHandlerDescriptor>();
        for (ResourceHandlerDescriptor handlerDescriptor : this.resourceHandlers) {
            if (handlerDescriptor.getResourceType().equals(resourceType)) {
                return handlerDescriptor;
            }
        }
        return null;
    }

}
