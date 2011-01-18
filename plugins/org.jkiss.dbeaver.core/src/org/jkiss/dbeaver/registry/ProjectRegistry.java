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

    private final List<ProjectHandlerDescriptor> projectHandlers = new ArrayList<ProjectHandlerDescriptor>();
    private final List<ResourceHandlerDescriptor> resourceHandlers = new ArrayList<ResourceHandlerDescriptor>();

    public ProjectRegistry()
    {
    }

    public void loadExtensions(IExtensionRegistry registry)
    {
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ProjectHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                ProjectHandlerDescriptor provider = new ProjectHandlerDescriptor(ext);
                projectHandlers.add(provider);
            }
        }
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ResourceHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                ResourceHandlerDescriptor provider = new ResourceHandlerDescriptor(ext);
                resourceHandlers.add(provider);
            }
        }
    }

    public void dispose()
    {
        for (ProjectHandlerDescriptor dataSourceDescriptor : this.projectHandlers) {
            dataSourceDescriptor.dispose();
        }
        this.projectHandlers.clear();

        for (ResourceHandlerDescriptor dataSourceDescriptor : this.resourceHandlers) {
            dataSourceDescriptor.dispose();
        }
        this.resourceHandlers.clear();
    }

    public List<ProjectHandlerDescriptor> getProjectHandlers()
    {
        return projectHandlers;
    }

    public List<ResourceHandlerDescriptor> getResourceHandlers()
    {
        return resourceHandlers;
    }
}
