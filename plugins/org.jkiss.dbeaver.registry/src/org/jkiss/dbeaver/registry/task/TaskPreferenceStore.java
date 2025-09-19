/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.preferences.AbstractPreferenceStore;
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
    private final DBPPreferenceStore parentStore;
    private final DBTTask task;
    private final Map<String, Object> properties;
    private boolean dirty = false;

    public TaskPreferenceStore(@NotNull DBTTask task) {
        this.parentStore = DBWorkbench.getPlatform().getPreferenceStore();
        this.task = task;
        this.properties = new LinkedHashMap<>(task.getProperties());
    }

    public TaskPreferenceStore(@NotNull Map<String, Object> properties) {
        this.parentStore = DBWorkbench.getPlatform().getPreferenceStore();
        this.task = null;
        this.properties = properties;
    }

    @Nullable
    public DBPPreferenceStore getParentStore() {
        return parentStore;
    }

    @NotNull
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public void save() throws IOException {
        if (task != null) {
            task.setProperties(properties);
        }
    }

    @Override
    public boolean contains(@NotNull String name) {
        return properties.containsKey(name);
    }

    @Override
    public boolean getBoolean(@NotNull String name) {
        return CommonUtils.toBoolean(getValue(name));
    }

    @Override
    public boolean getDefaultBoolean(@NotNull String name) {
        return CommonUtils.toBoolean(getValue(name));
    }

    @Override
    public double getDouble(@NotNull String name) {
        return CommonUtils.toDouble(getValue(name));
    }

    @Override
    public double getDefaultDouble(@NotNull String name) {
        return CommonUtils.toDouble(getValue(name));
    }

    @Override
    public float getFloat(@NotNull String name) {
        return (float) CommonUtils.toDouble(getValue(name));
    }

    @Override
    public float getDefaultFloat(@NotNull String name) {
        return getFloat(name);
    }

    @Override
    public int getInt(@NotNull String name) {
        return CommonUtils.toInt(getValue(name));
    }

    @Override
    public int getDefaultInt(@NotNull String name) {
        return getInt(name);
    }

    @Override
    public long getLong(@NotNull String name) {
        return CommonUtils.toLong(getValue(name));
    }

    @Override
    public long getDefaultLong(@NotNull String name) {
        return getLong(name);
    }

    @Override
    public String getString(@NotNull String name) {
        return CommonUtils.toString(getValue(name));
    }

    @Nullable
    public Object getValue(String name) {
        Object value = properties.get(name);
        if (value == null) {
            value = parentStore.getString(name);
        }
        return value;
    }

    @Override
    public String getDefaultString(@NotNull String name) {
        return getString(name);
    }

    @Override
    public boolean isDefault(@NotNull String name) {
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
    public void setDefault(@NotNull String name, double value) {
        // no defaults
    }

    @Override
    public void setDefault(@NotNull String name, float value) {
        // no defaults
    }

    @Override
    public void setDefault(@NotNull String name, int value) {
        // no defaults
    }

    @Override
    public void setDefault(@NotNull String name, long value) {
        // no defaults
    }

    @Override
    public void setDefault(@NotNull String name, @Nullable String value) {
        // no defaults
    }

    @Override
    public void setDefault(@NotNull String name, boolean value) {
        // no defaults
    }

    @Override
    public void setToDefault(@NotNull String name) {
        Object oldValue = properties.get(name);
        properties.remove(name);
        dirty = true;
        firePropertyChangeEvent(name, oldValue, null);
    }

    @Override
    public void setValue(@NotNull String name, double value) {
        double oldValue = getDouble(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(@NotNull String name, float value) {
        float oldValue = getFloat(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(@NotNull String name, int value) {
        int oldValue = getInt(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(@NotNull String name, long value) {
        long oldValue = getLong(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(@NotNull String name, @Nullable String value) {
        String oldValue = getString(name);
        if (oldValue == null || !oldValue.equals(value) || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(@NotNull String name, boolean value) {
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

    @Nullable
    @Override
    public <T> T getObject(String name) {
        return (T) properties.get(name);
    }

    @Override
    public Map<String, Object> getPropertyMap() {
        return properties;
    }
}
