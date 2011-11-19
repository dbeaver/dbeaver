/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * DataSourceViewDescriptor
 */
public class DataSourceViewDescriptor extends AbstractDescriptor
{
    private String id;
    private String targetID;
    private String label;
    private String viewClassName;
    private Object icon;

    public DataSourceViewDescriptor(DataSourceProviderDescriptor provider, IConfigurationElement config)
    {
        super(provider.getContributor());
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.targetID = config.getAttribute(RegistryConstants.ATTR_TARGET_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.viewClassName = config.getAttribute(RegistryConstants.ATTR_CLASS);
        this.icon = config.getAttribute(RegistryConstants.ATTR_ICON);
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

    public Object getIcon()
    {
        return icon;
    }

    public <T> T createView(Class<T> implementsClass)
    {
        try {
            Class<T> viewClass = getObjectClass(viewClassName, implementsClass);
            return viewClass.newInstance();
        }
        catch (Throwable ex) {
            throw new IllegalStateException("Can't create view '" + viewClassName + "'", ex);
        }
    }
}
