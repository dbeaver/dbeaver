/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.osgi.framework.Bundle;

/**
 * DataSourceViewDescriptor
 */
public class DataSourceViewDescriptor
{
    private DataSourceProviderDescriptor provider;
    private String id;
    private String targetID;
    private String label;
    private String viewClassName;
    private Object icon;

    public DataSourceViewDescriptor(DataSourceProviderDescriptor provider, IConfigurationElement config)
    {
        this.provider = provider;
        this.id = config.getAttribute("id");
        this.targetID = config.getAttribute("targetID");
        this.label = config.getAttribute("label");
        this.viewClassName = config.getAttribute("class");
        this.icon = config.getAttribute("icon");
    }

    public DataSourceProviderDescriptor getProvider()
    {
        return provider;
    }

    public String getId()
    {
        return id;
    }

    public String getTargetID()
    {
        return targetID;
    }

    public String getLabel()
    {
        return label;
    }

    public String getViewClassName()
    {
        return viewClassName;
    }

    public Object getIcon()
    {
        return icon;
    }

    public <T> T createView(Class<T> implementsClass)
    {
        try {
            Bundle extBundle = provider.getContributorBundle();
            if (extBundle == null) {
                throw new IllegalStateException("Bundle " + provider.getContributorName() + " not found");
            }
            Class<?> viewClass = extBundle.loadClass(viewClassName);
            return implementsClass.cast(
                viewClass.newInstance());
        }
        catch (Throwable ex) {
            throw new IllegalStateException("Can't create view '" + viewClassName + "'", ex);
        }
    }
}
