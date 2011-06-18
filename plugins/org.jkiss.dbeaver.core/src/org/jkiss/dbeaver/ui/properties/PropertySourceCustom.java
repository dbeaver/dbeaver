/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.properties;

import org.jkiss.utils.CommonUtils;
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

}
