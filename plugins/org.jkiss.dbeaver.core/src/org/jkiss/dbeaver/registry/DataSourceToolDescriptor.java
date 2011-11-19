/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPTool;
import org.osgi.framework.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * DataSourceToolDescriptor
 */
public class DataSourceToolDescriptor extends AbstractContextDescriptor
{
    private final String id;
    private final String label;
    private final String description;
    private final String toolClassName;
    private final Object icon;

    public DataSourceToolDescriptor(
        DataSourceProviderDescriptor provider, IConfigurationElement config)
    {
        super(provider.getContributor(), config);
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.toolClassName = config.getAttribute(RegistryConstants.ATTR_CLASS);
        this.icon = config.getAttribute(RegistryConstants.ATTR_ICON);
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

    public Object getIcon()
    {
        return icon;
    }

    public DBPTool createTool()
    {
        try {
            Class<DBPTool> toolClass = getObjectClass(toolClassName, DBPTool.class);
            return toolClass.newInstance();
        }
        catch (Throwable ex) {
            throw new IllegalStateException("Can't create tool '" + toolClassName + "'", ex);
        }
    }

}
