/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.prop.DBPProperty;

/**
 * PropertyDescriptor
 */
public class PropertyDescriptor implements DBPProperty
{

    static final Log log = LogFactory.getLog(PropertyDescriptor.class);

    public static final String PROPERTY_TAG = "property";

    private PropertyGroupDescriptor group;
    private String name;
    private String description;
    private String defaultValue;
    private DBPProperty.PropertyType type;
    private String[] validValues;

    public PropertyDescriptor(PropertyGroupDescriptor group, IConfigurationElement config)
    {
        this.group = group;
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
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
        this.defaultValue = config.getAttribute("defaultValue");
        String valueList = config.getAttribute("validValues");
        if (valueList != null) {
            validValues = valueList.split(",");
        }
    }

    public PropertyGroupDescriptor getGroup()
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
        return validValues;
    }

}
