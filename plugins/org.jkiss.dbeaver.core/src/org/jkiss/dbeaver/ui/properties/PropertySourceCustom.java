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

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

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

    public void setValues(Map<Object, Object> values)
    {
        this.originalValues = new HashMap<Object, Object>();
        // Set only allowed properties + transform property types
        for (Map.Entry<Object, Object> value : values.entrySet()) {
            Object propValue = value.getValue();
            for (IPropertyDescriptor prop : props) {
                if (prop.getId().equals(value.getKey())) {
                    if (propValue instanceof String && prop instanceof IPropertyDescriptorEx) {
                        propValue = RuntimeUtils.convertString((String) value.getValue(), ((IPropertyDescriptorEx) prop).getDataType());
                    }
                    originalValues.put(value.getKey(), propValue);
                    break;
                }
            }
        }

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

    @Override
    public Object getEditableValue()
    {
        return this;
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors()
    {
        return props.toArray(new IPropertyDescriptor[props.size()]);
    }

    @Override
    public Object getPropertyValue(Object id)
    {
        Object value = propValues.get(id);
        if (value == null) {
            value = originalValues.get(id);
        }
        return value != null ? value : defaultValues.get(id);
    }

    @Override
    public boolean isPropertyResettable(Object id)
    {
        return true;
    }

    @Override
    public boolean isPropertySet(Object id)
    {
        final Object value = getPropertyValue(id);
        if (value == null) {
            return false;
        }
        final Object defaultValue = defaultValues.get(id);
        return !CommonUtils.equalObjects(value, defaultValue);
    }

    @Override
    public void resetPropertyValue(Object id)
    {
        propValues.remove(id);
    }

    @Override
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

    @Override
    public boolean isDirty(Object id)
    {
        return !propValues.isEmpty();
    }

    @Override
    public boolean hasDefaultValue(Object id)
    {
        return defaultValues.containsKey(id);
    }

    @Override
    public void resetPropertyValueToDefault(Object id)
    {
        propValues.remove(id);
        originalValues.remove(id);
    }

}
