/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.model.DBPProperty;
import org.jkiss.dbeaver.model.DBPPropertyGroup;

/**
 * PropertyDescriptor
 */
public class PropertyDescriptor implements DBPProperty
{

    static final Log log = LogFactory.getLog(PropertyDescriptor.class);

    public static final String PROPERTY_TAG = "property";

    private DBPPropertyGroup group;
    private String id;
    private String name;
    private String description;
    private DBPProperty.PropertyType type;
    private boolean required;
    private String defaultValue;
    private String[] validValues;

    public PropertyDescriptor(PropertyGroupDescriptor group, IConfigurationElement config)
    {
        this.group = group;
        this.id = config.getAttribute("id");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.required = "true".equals(config.getAttribute("required"));
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

    public PropertyDescriptor(DBPPropertyGroup group, String id, String name, String description, PropertyType type, boolean required, String defaultValue, String[] validValues) {
        this.group = group;
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
        this.validValues = validValues;
    }

    public DBPPropertyGroup getGroup()
    {
        return group;
    }

    public String getId()
    {
        return id;
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

    public boolean isRequired()
    {
        return required;
    }

    public String[] getValidValues()
    {
        return validValues;
    }

}
