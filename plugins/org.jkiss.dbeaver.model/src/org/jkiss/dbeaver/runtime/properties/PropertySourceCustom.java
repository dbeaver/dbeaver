/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Simple property source which store properties in map
 */
public class PropertySourceCustom implements DBPPropertySource {

    private List<DBPPropertyDescriptor> props = new ArrayList<>();

    private Map<String, Object> originalValues = new TreeMap<>();
    private Map<String, Object> propValues = new TreeMap<>();
    private Map<String,Object> defaultValues = new TreeMap<>();
    private IVariableResolver defValueResolver = null;

    public PropertySourceCustom()
    {
    }

    public PropertySourceCustom(Collection<? extends DBPPropertyDescriptor> properties, Map<String, ?> values)
    {
        addProperties(properties);
        setValues(values);
    }

    public PropertySourceCustom(DBPPropertyDescriptor[] properties, Map<String, ?> values)
    {
        addProperties(properties);
        setValues(values);
    }

    public void setDefValueResolver(IVariableResolver defValueResolver) {
        this.defValueResolver = defValueResolver;
    }

    public void setValues(Map<String, ?> values)
    {
        this.originalValues = new HashMap<>();
        // Set only allowed properties + transform property types
        if (values != null) {
            for (Map.Entry<String, ?> value : values.entrySet()) {
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

    public void setDefaultValues(Map<String, Object> defaultValues)
    {
        this.defaultValues = defaultValues;
    }

    public void addDefaultValues(Map<String, ?> defaultValues)
    {
        this.defaultValues.putAll(defaultValues);
    }

    public Map<String, Object> getPropertyValues() {
        Map<String, Object> allValues = new HashMap<>(originalValues);
        allValues.putAll(propValues);
        return allValues;
    }

    public Map<String, Object> getPropertiesWithDefaults() {
        Map<String, Object> allValues = new HashMap<>(defaultValues);
        allValues.putAll(originalValues);
        allValues.putAll(propValues);
        if (defValueResolver != null) {
            for (Map.Entry<String, Object> prop : allValues.entrySet()) {
                prop.setValue(getDefaultValue(prop.getValue()));
            }
        }
        return allValues;
    }

    public void addProperty(DBPPropertyDescriptor property)
    {
        props.add(property);
        final Object defaultValue = property.getDefaultValue();
        if (defaultValue != null) {
            defaultValues.put(property.getId(), defaultValue);
        }
    }

    public void addProperties(Collection<? extends DBPPropertyDescriptor> properties) {
        props.addAll(properties);
        for (DBPPropertyDescriptor prop : properties) {
            final Object defaultValue = prop.getDefaultValue();
            if (defaultValue != null) {
                defaultValues.put(prop.getId(), defaultValue);
            }
        }
    }

    public void addProperties(DBPPropertyDescriptor[] properties) {
        Collections.addAll(props, properties);
        for (DBPPropertyDescriptor prop : properties) {
            final Object defaultValue = prop.getDefaultValue();
            if (defaultValue != null) {
                defaultValues.put(prop.getId(), defaultValue);
            }
        }
    }

    private Object getDefaultValue(Object defaultValue) {
        if (defValueResolver != null && defaultValue instanceof String) {
            return GeneralUtils.replaceVariables((String) defaultValue, defValueResolver);
        }
        return defaultValue;
    }

    @Override
    public Object getEditableValue()
    {
        return this;
    }

    @Override
    public DBPPropertyDescriptor[] getProperties() {
        return props.toArray(new DBPPropertyDescriptor[0]);
    }

    @Override
    public Object getPropertyValue(@Nullable DBRProgressMonitor monitor, String id)
    {
        if (id == null) {
            return null;
        }
        Object value = propValues.get(id);
        if (value == null) {
            value = originalValues.get(id);
        }
        if (value == null) {
            value = defaultValues.get(id);
        }
        return value != null ? getDefaultValue(value) : null;
    }

    @Override
    public boolean isPropertyResettable(String id)
    {
        return true;
    }

    @Override
    public boolean isPropertySet(String id)
    {
        final Object value = getPropertyValue(null, id);
        if (value == null) {
            return false;
        }
        final Object defaultValue = getDefaultValue(defaultValues.get(id));
        return !CommonUtils.equalObjects(getDefaultValue(value), defaultValue);
    }

    @Override
    public void resetPropertyValue(@Nullable DBRProgressMonitor monitor, String id)
    {
        propValues.remove(id);
    }

    @Override
    public void setPropertyValue(@Nullable DBRProgressMonitor monitor, String id, Object value)
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
    public void resetPropertyValueToDefault(String id)
    {
        propValues.remove(id);
        originalValues.remove(id);
    }

    public void removeAll() {
        props.clear();

        originalValues.clear();
        propValues.clear();
        defaultValues.clear();
    }
}
