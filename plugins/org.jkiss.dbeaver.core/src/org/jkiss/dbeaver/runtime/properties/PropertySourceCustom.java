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
package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.dbeaver.model.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.DBPPropertySource;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Simple property source which store properties in map
 */
public class PropertySourceCustom implements DBPPropertySource {

    private List<DBPPropertyDescriptor> props = new ArrayList<DBPPropertyDescriptor>();

    private Map<Object, Object> originalValues = new TreeMap<Object, Object>();
    private Map<Object, Object> propValues = new TreeMap<Object, Object>();
    private Map<Object,Object> defaultValues = new TreeMap<Object, Object>();

    public PropertySourceCustom()
    {
    }

    public PropertySourceCustom(Collection<? extends DBPPropertyDescriptor> properties, Map<Object, Object> values)
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
            for (DBPPropertyDescriptor prop : props) {
                if (prop.getId().equals(value.getKey())) {
                    if (propValue instanceof String) {
                        propValue = GeneralUtils.convertString((String) value.getValue(), prop.getDataType());
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

    public void addProperties(Collection<? extends DBPPropertyDescriptor> properties)
    {
        props.addAll(properties);
        for (DBPPropertyDescriptor prop : properties) {
            final Object defaultValue = prop.getDefaultValue();
            if (defaultValue != null) {
                defaultValues.put(prop.getId(), defaultValue);
            }
        }
    }

    @Override
    public Object getEditableValue()
    {
        return this;
    }

    @Override
    public DBPPropertyDescriptor[] getPropertyDescriptors2() {
        return props.toArray(new DBPPropertyDescriptor[props.size()]);
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
    public void resetPropertyValueToDefault(Object id)
    {
        propValues.remove(id);
        originalValues.remove(id);
    }

}
