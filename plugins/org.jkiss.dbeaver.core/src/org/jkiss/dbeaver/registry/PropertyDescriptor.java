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

    public static final String TAG_PROPERTY = "property"; //NON-NLS-1
    public static final String ATTR_ID = "id"; //NON-NLS-1
    public static final String ATTR_LABEL = "label"; //NON-NLS-1
    public static final String ATTR_DESCRIPTION = "description"; //NON-NLS-1
    public static final String ATTR_REQUIRED = "required"; //NON-NLS-1
    public static final String ATTR_TYPE = "type"; //NON-NLS-1
    public static final String ATTR_DEFAULT_VALUE = "defaultValue"; //NON-NLS-1
    public static final String ATTR_VALID_VALUES = "validValues"; //NON-NLS-1
    public static final String VALUE_SPLITTER = ","; //NON-NLS-1

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
        this.id = config.getAttribute(ATTR_ID);
        this.name = config.getAttribute(ATTR_LABEL);
        this.description = config.getAttribute(ATTR_DESCRIPTION);
        this.required = "true".equals(config.getAttribute(ATTR_REQUIRED));
        String typeString = config.getAttribute(ATTR_TYPE);
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
        this.defaultValue = config.getAttribute(ATTR_DEFAULT_VALUE);
        String valueList = config.getAttribute(ATTR_VALID_VALUES);
        if (valueList != null) {
            validValues = valueList.split(VALUE_SPLITTER);
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
