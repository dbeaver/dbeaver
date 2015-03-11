/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.properties;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.model.DBPPropertyDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertyDescriptorEx
 */
public class PropertyDescriptorEx implements DBPPropertyDescriptor, IPropertyDescriptorEx, IPropertyValueListProvider<Object>
{

    static final Log log = Log.getLog(PropertyDescriptorEx.class);

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

    public static List<PropertyDescriptorEx> extractProperties(IConfigurationElement config)
    {
        String category = config.getAttribute(ATTR_LABEL);
        if (CommonUtils.isEmpty(category)) {
            category = NAME_UNDEFINED;
        }
        List<PropertyDescriptorEx> properties = new ArrayList<PropertyDescriptorEx>();
        IConfigurationElement[] propElements = config.getChildren(PropertyDescriptorEx.TAG_PROPERTY);
        for (IConfigurationElement prop : propElements) {
            properties.add(new PropertyDescriptorEx(category, prop));
        }
        return properties;
    }

    public PropertyDescriptorEx(String category, IConfigurationElement config)
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
                type = PropertyType.valueOf(typeString.toUpperCase()).getValueType();
            }
            catch (IllegalArgumentException ex) {
                log.warn(ex);
                type = String.class;
            }
        }
        this.defaultValue = RuntimeUtils.convertString(config.getAttribute(ATTR_DEFAULT_VALUE), type);
        String valueList = config.getAttribute(ATTR_VALID_VALUES);
        if (valueList != null) {
            final String[] values = valueList.split(VALUE_SPLITTER);
            validValues = new Object[values.length];
            for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
                validValues[i] = RuntimeUtils.convertString(values[i], type);
            }
        }
        this.editable = true;
    }

    public PropertyDescriptorEx(String category, Object id, String name, String description, Class<?> type, boolean required, String defaultValue, String[] validValues, boolean editable) {
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

    @Override
    public CellEditor createPropertyEditor(Composite parent)
    {
        if (!editable) {
            return null;
        }
        return ObjectPropertyDescriptor.createCellEditor(parent, null, this);
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

    @Override
    public ILabelProvider getLabelProvider()
    {
        return null;
    }

    @Override
    public boolean isCompatibleWith(IPropertyDescriptor anotherProperty)
    {
        return CommonUtils.equalObjects(category, anotherProperty.getCategory()) &&
            CommonUtils.equalObjects(id, anotherProperty.getId());
    }

    @NotNull
    @Override
    public String getDisplayName()
    {
        return name;
    }

    @Override
    public String[] getFilterFlags()
    {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object getHelpContextIds()
    {
        return null;
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
