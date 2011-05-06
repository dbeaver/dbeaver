/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import java.util.*;

/**
 * Simple property source which store properties in map
 */
public class PropertySourceCustom implements IPropertySourceEx {

    private List<IPropertyDescriptor> props = new ArrayList<IPropertyDescriptor>();

    private Map<Object, Object> originalValues = new TreeMap<Object, Object>();
    private Map<Object, Object> propValues = new TreeMap<Object, Object>();
    private Map<Object,Object> defaultValues = new TreeMap<Object, Object>();

    public PropertySourceCustom()
    {
    }

    public PropertySourceCustom(Collection<? extends IPropertyDescriptor> properties, Map<Object, Object> values)
    {
        addProperties(properties);
        setValues(values);
    }

    public void setValues(Map<Object, Object> originalValues)
    {
        this.originalValues = originalValues;
    }

    public void setDefaultValues(Map<Object, Object> defaultValues)
    {
        this.defaultValues = defaultValues;
    }

    public Map<Object, Object> getProperties() {
        Map<Object, Object> allValues = new HashMap<Object, Object>(originalValues);
        allValues.putAll(propValues);
        return allValues;
    }

    public Map<Object, Object> getPropertiesWithDefaults() {
        Map<Object, Object> allValues = new HashMap<Object, Object>(defaultValues);
        allValues.putAll(originalValues);
        allValues.putAll(propValues);
        return allValues;
    }

    public IPropertyDescriptor addProperty(Object id, String displayName, String description, String category, Class<Object> dataType, boolean required)
    {
        return addProperty(id, displayName, description, category, dataType, required, null, null, null);
    }

    public IPropertyDescriptor addProperty(Object id, String displayName, String description, String category, Class<Object> dataType, boolean required, Object[] possibleValues, String[] filterFlags, Object helpContextIds)
    {
        PropertyDescriptor prop = new PropertyDescriptor(id, displayName, description, category, dataType, required, possibleValues, filterFlags, helpContextIds);
        props.add(prop);
        return prop;
    }

    public void addProperties(Collection<? extends IPropertyDescriptor> properties)
    {
        props.addAll(properties);
        for (IPropertyDescriptor prop : properties) {
            if (prop instanceof IPropertyDescriptorEx) {
                final Object defaultValue = ((IPropertyDescriptorEx) prop).getDefaultValue();
                if (defaultValue != null) {
                    defaultValues.put(prop.getId(), defaultValue);
                }
            }
        }
    }

    public Object getEditableValue()
    {
        return this;
    }

    public IPropertyDescriptor[] getPropertyDescriptors()
    {
        return props.toArray(new IPropertyDescriptor[props.size()]);
    }

    public Object getPropertyValue(Object id)
    {
        Object value = propValues.get(id);
        if (value == null) {
            value = originalValues.get(id);
        }
        return value != null ? value : defaultValues.get(id);
    }

    public boolean isPropertyResettable(Object id)
    {
        return true;
    }

    public boolean isPropertySet(Object id)
    {
        final Object value = getPropertyValue(id);
        if (value == null) {
            return false;
        }
        final Object defaultValue = defaultValues.get(id);
        return !CommonUtils.equalObjects(value, defaultValue);
    }

    public void resetPropertyValue(Object id)
    {
        propValues.remove(id);
    }

    public void setPropertyValue(Object id, Object value)
    {
        if (!originalValues.containsKey(id)) {
            if (propValues.containsKey(id)) {
                originalValues.put(id, propValues.get(id));
            } else if (defaultValues.containsKey(id)) {
                originalValues.put(id, defaultValues.get(id));
            } else {
                originalValues.put(id, null);
            }
        }
        if (value == null || value.equals(originalValues.get(id))) {
            propValues.remove(id);
        } else {
            propValues.put(id, value);
        }
    }

    public boolean isDirty(Object id)
    {
        return !propValues.isEmpty();
    }

    public boolean hasDefaultValue(Object id)
    {
        return defaultValues.containsKey(id);
    }

    public void resetPropertyValueToDefault(Object id)
    {
        propValues.remove(id);
        originalValues.remove(id);
    }

    private class PropertyDescriptor implements IPropertyDescriptorEx, IPropertyValueListProvider {

        private Object id;
        private String displayName;
        private String description;
        private String category;
        private Class<Object> dataType;
        private boolean required;
        private Object[] possibleValues;
        private String[] filterFlags;
        private Object helpContextIds;

        private PropertyDescriptor(
            Object id,
            String displayName,
            String description,
            String category,
            Class<Object> dataType,
            boolean required,
            Object[] possibleValues,
            String[] filterFlags,
            Object helpContextIds)
        {
            this.category = category;
            this.description = description;
            this.displayName = displayName;
            this.dataType = dataType;
            this.required = required;
            this.possibleValues = possibleValues;
            this.filterFlags = filterFlags;
            this.helpContextIds = helpContextIds;
            this.id = id;
        }

        public CellEditor createPropertyEditor(Composite parent)
        {
            return ObjectPropertyDescriptor.createCellEditor(parent, getEditableValue(), this);
        }

        public String getCategory()
        {
            return category;
        }

        public String getDescription()
        {
            return description;
        }

        public String getDisplayName()
        {
            return displayName;
        }

        public String[] getFilterFlags()
        {
            return filterFlags;
        }

        public Object getHelpContextIds()
        {
            return helpContextIds;
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
            return anotherProperty instanceof PropertyDescriptor && anotherProperty.getId().equals(id);
        }

        public Class<?> getDataType()
        {
            return dataType;
        }

        public boolean isRequired()
        {
            return required;
        }

        public Object getDefaultValue()
        {
            return null;
        }

        public boolean allowCustomValue()
        {
            return false;
        }

        public Object[] getPossibleValues(Object object)
        {
            return possibleValues;
        }

    }
    
}
