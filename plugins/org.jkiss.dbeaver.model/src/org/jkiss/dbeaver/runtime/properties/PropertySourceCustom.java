/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Simple property source which store properties in map
 */
public class PropertySourceCustom implements DBPPropertySource {

    private List<DBPPropertyDescriptor> props = new ArrayList<>();

    private Map<Object, Object> originalValues = new TreeMap<>();
    private Map<Object, Object> propValues = new TreeMap<>();
    private Map<Object,Object> defaultValues = new TreeMap<>();

    public PropertySourceCustom()
    {
    }

    public PropertySourceCustom(Collection<? extends DBPPropertyDescriptor> properties, Map<?, ?> values)
    {
        addProperties(properties);
        setValues(values);
    }

    public void setValues(Map<?, ?> values)
    {
        this.originalValues = new HashMap<>();
        // Set only allowed properties + transform property types
        if (values != null) {
            for (Map.Entry<?, ?> value : values.entrySet()) {
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
    }

    public void setDefaultValues(Map<Object, Object> defaultValues)
    {
        this.defaultValues = defaultValues;
    }

    public void addDefaultValues(Map<Object, Object> defaultValues)
    {
        this.defaultValues.putAll(defaultValues);
    }

    public Map<Object, Object> getProperties() {
        Map<Object, Object> allValues = new HashMap<>(originalValues);
        allValues.putAll(propValues);
        return allValues;
    }

    public Map<Object, Object> getPropertiesWithDefaults() {
        Map<Object, Object> allValues = new HashMap<>(defaultValues);
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
    public Object getPropertyValue(@Nullable DBRProgressMonitor monitor, Object id)
    {
        if (id == null) {
            return null;
        }
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
        final Object value = getPropertyValue(null, id);
        if (value == null) {
            return false;
        }
        final Object defaultValue = defaultValues.get(id);
        return !CommonUtils.equalObjects(value, defaultValue);
    }

    @Override
    public void resetPropertyValue(@Nullable DBRProgressMonitor monitor, Object id)
    {
        propValues.remove(id);
    }

    @Override
    public void setPropertyValue(@Nullable DBRProgressMonitor monitor, Object id, Object value)
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
