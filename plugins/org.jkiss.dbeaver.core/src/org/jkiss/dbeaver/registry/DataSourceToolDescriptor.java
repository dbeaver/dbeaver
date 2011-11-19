/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPTool;
import org.osgi.framework.Bundle;

/**
 * DataSourceToolDescriptor
 */
public class DataSourceToolDescriptor extends AbstractDescriptor
{
    private final DataSourceProviderDescriptor provider;
    private final String id;
    private final String label;
    private final String description;
    private final String toolClassName;
    private final Object icon;

    public DataSourceToolDescriptor(
        DataSourceProviderDescriptor provider, IConfigurationElement config)
    {
        super(provider.getContributor());
        this.provider = provider;
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.toolClassName = config.getAttribute(RegistryConstants.ATTR_CLASS);
        this.icon = config.getAttribute(RegistryConstants.ATTR_ICON);
    }

    public DataSourceProviderDescriptor getProvider()
    {
        return provider;
    }

    public String getId()
    {
        return id;
    }

    public String getLabel()
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    public String getToolClassName()
    {
        return toolClassName;
    }

    public Object getIcon()
    {
        return icon;
    }

    public <T extends DBPTool> T createTool(Class<T> implementsClass)
    {
        try {
            Bundle extBundle = provider.getContributorBundle();
            if (extBundle == null) {
                throw new IllegalStateException("Bundle " + provider.getContributorName() + " not found");
            }
            Class<?> viewClass = extBundle.loadClass(toolClassName);
            return implementsClass.cast(
                viewClass.newInstance());
        }
        catch (Throwable ex) {
            throw new IllegalStateException("Can't create tool '" + toolClassName + "'", ex);
        }
    }
}
