/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPDriverProperty;

/**
 * DriverPropertyDescriptor
 */
public class DriverPropertyDescriptor implements DBPDriverProperty
{

    static final Log log = LogFactory.getLog(DriverPropertyDescriptor.class);

    private DriverPropertyGroupDescriptor group;
    private String name;
    private String description;
    private String defaultValue;
    private PropertyType type;

    public DriverPropertyDescriptor(DriverPropertyGroupDescriptor group, IConfigurationElement config)
    {
        this.group = group;
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.defaultValue = config.getAttribute("defaultValue");
        String typeString = config.getAttribute("type");
        if (typeString == null) {
            type = PropertyType.STRING;
        } else {
            try {
                type = PropertyType.valueOf(typeString.toUpperCase());
            }
            catch (IllegalArgumentException ex) {
                log.warn(ex);
                type = PropertyType.STRING;
            }
        }
    }

    public DriverPropertyGroupDescriptor getGroup()
    {
        return group;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public PropertyType getType()
    {
        return type;
    }

    public String[] getValidValues()
    {
        return null;
    }
}
