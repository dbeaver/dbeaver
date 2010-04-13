/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPDriverPropertyGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * DriverPropertyDescriptor
 */
public class DriverPropertyGroupDescriptor implements DBPDriverPropertyGroup
{
    private DriverDescriptor driver;
    private String name;
    private String description;
    private List<DriverPropertyDescriptor> properties = new ArrayList<DriverPropertyDescriptor>();

    public DriverPropertyGroupDescriptor(DriverDescriptor driver, IConfigurationElement config)
    {
        this.driver = driver;
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        IConfigurationElement[] propElements = config.getChildren("property");
        for (IConfigurationElement prop : propElements) {
            properties.add(new DriverPropertyDescriptor(this, prop));
        }
    }

    public DriverDescriptor getDriver()
    {
        return driver;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public List<DriverPropertyDescriptor> getProperties()
    {
        return properties;
    }
}