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
package org.jkiss.dbeaver.registry.task;

import org.jkiss.dbeaver.model.impl.preferences.AbstractPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceMap;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wrapper over simple properties
 */
public class TaskPreferenceStore extends AbstractPreferenceStore implements DBPPreferenceMap {
    private DBPPreferenceStore parentStore;
    private final DBTTask task;
    private Map<String, Object> properties;
    private boolean dirty = false;

    public TaskPreferenceStore(DBTTask task) {
        this.parentStore = DBWorkbench.getPlatform().getPreferenceStore();
        this.task = task;
        this.properties = new LinkedHashMap<>(task.getProperties());
    }

    public TaskPreferenceStore(Map<String, Object> properties) {
        this.parentStore = DBWorkbench.getPlatform().getPreferenceStore();
        this.task = null;
        this.properties = properties;
    }

    public DBPPreferenceStore getParentStore() {
        return parentStore;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public void addPropertyChangeListener(DBPPreferenceListener listener) {
        addListenerObject(listener);
    }

    @Override
    public void removePropertyChangeListener(DBPPreferenceListener listener) {
        removeListenerObject(listener);
    }

    @Override
    public void save() throws IOException {
        if (task != null) {
            task.setProperties(properties);
        }
    }

    @Override
    public boolean contains(String name) {
        return properties.containsKey(name);
    }

    @Override
    public boolean getBoolean(String name) {
        return CommonUtils.toBoolean(getValue(name));
    }

    @Override
    public boolean getDefaultBoolean(String name) {
        return CommonUtils.toBoolean(getValue(name));
    }

    @Override
    public double getDouble(String name) {
        return CommonUtils.toDouble(getValue(name));
    }

    @Override
    public double getDefaultDouble(String name) {
        return CommonUtils.toDouble(getValue(name));
    }

    @Override
    public float getFloat(String name) {
        return (float) CommonUtils.toDouble(getValue(name));
    }

    @Override
    public float getDefaultFloat(String name) {
        return getFloat(name);
    }

    @Override
    public int getInt(String name) {
        return CommonUtils.toInt(getValue(name));
    }

    @Override
    public int getDefaultInt(String name) {
        return getInt(name);
    }

    @Override
    public long getLong(String name) {
        return CommonUtils.toLong(getValue(name));
    }

    @Override
    public long getDefaultLong(String name) {
        return getLong(name);
    }

    @Override
    public String getString(String name) {
        return CommonUtils.toString(getValue(name));
    }

    public Object getValue(String name) {
        Object value = properties.get(name);
        if (value == null) {
            value = parentStore.getString(name);
        }
        return value;
    }

    @Override
    public String getDefaultString(String name) {
        return getString(name);
    }

    @Override
    public boolean isDefault(String name) {
        return false;
    }

    public boolean isSet(String name) {
        return properties.containsKey(name);
    }

    @Override
    public boolean needsSaving() {
        return dirty;
    }

    @Override
    public void setDefault(String name, double value) {
        // no defaults
    }

    @Override
    public void setDefault(String name, float value) {
        // no defaults
    }

    @Override
    public void setDefault(String name, int value) {
        // no defaults
    }

    @Override
    public void setDefault(String name, long value) {
        // no defaults
    }

    @Override
    public void setDefault(String name, String value) {
        // no defaults
    }

    @Override
    public void setDefault(String name, boolean value) {
        // no defaults
    }

    @Override
    public void setToDefault(String name) {
        Object oldValue = properties.get(name);
        properties.remove(name);
        dirty = true;
        firePropertyChangeEvent(name, oldValue, null);
    }

    @Override
    public void setValue(String name, double value) {
        double oldValue = getDouble(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, float value) {
        float oldValue = getFloat(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, int value) {
        int oldValue = getInt(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, long value) {
        long oldValue = getLong(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, String value) {
        String oldValue = getString(name);
        if (oldValue == null || !oldValue.equals(value) || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, boolean value) {
        boolean oldValue = getBoolean(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, String.valueOf(value));
            dirty = true;
            firePropertyChangeEvent(name, oldValue ? Boolean.TRUE
                : Boolean.FALSE, value ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TaskPreferenceStore)) {
            return false;
        }
        TaskPreferenceStore copy = (TaskPreferenceStore) obj;
        return
            CommonUtils.equalObjects(parentStore, copy.parentStore) &&
                CommonUtils.equalObjects(properties, copy.properties);
    }

    @Override
    public <T> T getObject(String name) {
        return (T) properties.get(name);
    }

    @Override
    public Map<String, Object> getPropertyMap() {
        return properties;
    }
}
