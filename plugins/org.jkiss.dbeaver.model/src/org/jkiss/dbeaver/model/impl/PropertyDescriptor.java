/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertyDescriptor
 */
public class PropertyDescriptor implements DBPPropertyDescriptor, IPropertyValueListProvider<Object>
{
    public enum PropertyType
    {
        t_string(String.class),
        t_boolean(Boolean.class),
        t_short(Short.class),
        t_integer(Integer.class),
        t_long(Long.class),
        t_float(Float.class),
        t_double(Double.class),
        t_numeric(Double.class),
        t_resource(IResource.class);

        private final Class<?> valueType;

        PropertyType(Class<?> valueType)
        {
            this.valueType = valueType;
        }

        public Class<?> getValueType()
        {
            return valueType;
        }
    }

    static final Log log = Log.getLog(PropertyDescriptor.class);

    public static final String TAG_PROPERTY_GROUP = "propertyGroup"; //NON-NLS-1
    public static final String NAME_UNDEFINED = "<undefined>"; //NON-NLS-1
    public static final String TAG_PROPERTY = "property"; //NON-NLS-1
    public static final String ATTR_ID = "id"; //NON-NLS-1
    public static final String ATTR_LABEL = "label"; //NON-NLS-1
    public static final String ATTR_DESCRIPTION = "description"; //NON-NLS-1
    public static final String ATTR_REQUIRED = "required"; //NON-NLS-1
    public static final String ATTR_TYPE = "type"; //NON-NLS-1
    public static final String ATTR_DEFAULT_VALUE = "defaultValue"; //NON-NLS-1
    public static final String ATTR_VALID_VALUES = "validValues"; //NON-NLS-1
    public static final String VALUE_SPLITTER = ","; //NON-NLS-1

    private Object id;
    private String name;
    private String description;
    private String category;
    private Class<?> type;
    private boolean required;
    private Object defaultValue;
    private Object[] validValues;
    private boolean editable;

    public static List<PropertyDescriptor> extractProperties(IConfigurationElement config)
    {
        String category = config.getAttribute(ATTR_LABEL);
        if (CommonUtils.isEmpty(category)) {
            category = NAME_UNDEFINED;
        }
        List<PropertyDescriptor> properties = new ArrayList<>();
        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptor.TAG_PROPERTY);
        for (IConfigurationElement prop : propElements) {
            properties.add(new PropertyDescriptor(category, prop));
        }
        return properties;
    }

    public PropertyDescriptor(String category, IConfigurationElement config)
    {
        this.category = category;
        this.id = config.getAttribute(ATTR_ID);
        this.name = config.getAttribute(ATTR_LABEL);
        this.description = config.getAttribute(ATTR_DESCRIPTION);
        this.required = CommonUtils.getBoolean(config.getAttribute(ATTR_REQUIRED));
        String typeString = config.getAttribute(ATTR_TYPE);
        if (typeString == null) {
            type = String.class;
        } else {
            try {
                type = PropertyType.valueOf("t_" + typeString).getValueType();
            }
            catch (IllegalArgumentException ex) {
                log.warn(ex);
                type = String.class;
            }
        }
        this.defaultValue = GeneralUtils.convertString(config.getAttribute(ATTR_DEFAULT_VALUE), type);
        String valueList = config.getAttribute(ATTR_VALID_VALUES);
        if (valueList != null) {
            final String[] values = valueList.split(VALUE_SPLITTER);
            validValues = new Object[values.length];
            for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
                validValues[i] = GeneralUtils.convertString(values[i], type);
            }
        }
        this.editable = true;
    }

    public PropertyDescriptor(String category, Object id, String name, String description, Class<?> type, boolean required, String defaultValue, String[] validValues, boolean editable) {
        this.category = category;
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
        this.validValues = validValues;
        this.editable = editable;
    }

    @Nullable
    @Override
    public String getCategory()
    {
        return category;
    }

    @NotNull
    @Override
    public Object getId()
    {
        return id;
    }

    @NotNull
    @Override
    public String getDisplayName()
    {
        return name;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public Object getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public boolean isEditable(Object object)
    {
        return editable;
    }

    @Override
    public Class<?> getDataType()
    {
        return type;
    }

    @Override
    public boolean isRequired()
    {
        return required;
    }

    @Override
    public boolean allowCustomValue()
    {
        return ArrayUtils.isEmpty(validValues);
    }

    @Override
    public Object[] getPossibleValues(Object object)
    {
        return validValues;
    }

}
