/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertyDescriptorEx
 */
public class PropertyDescriptorEx implements IPropertyDescriptorEx, IPropertyValueListProvider<Object>
{

    static final Log log = LogFactory.getLog(PropertyDescriptorEx.class);

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
        this.required = "true".equals(config.getAttribute(ATTR_REQUIRED));
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

    public CellEditor createPropertyEditor(Composite parent)
    {
        return ObjectPropertyDescriptor.createCellEditor(parent, null, this);
    }

    public String getCategory()
    {
        return category;
    }

    public Object getId()
    {
        return id;
    }

    public ILabelProvider getLabelProvider()
    {
        return null;
    }

    public boolean isCompatibleWith(IPropertyDescriptor anotherProperty)
    {
        return category.equals(anotherProperty.getCategory()) && id.equals(anotherProperty.getId());
    }

    public String getDisplayName()
    {
        return name;
    }

    public String[] getFilterFlags()
    {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getHelpContextIds()
    {
        return null;
    }

    public String getDescription()
    {
        return description;
    }

    public Object getDefaultValue()
    {
        return defaultValue;
    }

    public boolean isEditable(Object object)
    {
        return editable;
    }

    public Class<?> getDataType()
    {
        return type;
    }

    public boolean isRequired()
    {
        return required;
    }

    public boolean allowCustomValue()
    {
        return CommonUtils.isEmpty(validValues);
    }

    public Object[] getPossibleValues(Object object)
    {
        return validValues;
    }

}
