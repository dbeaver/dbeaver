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
package org.jkiss.dbeaver.model.impl.preferences;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Preference store which can be stored/loaded in any way.
 * Also it can use parent store to obtain values from it if this store do not contain the key.
 * However, save will always use THIS store, not parent.
 * Originally copied from standard PreferenceStore class
 */
public abstract class SimplePreferenceStore extends AbstractPreferenceStore {
    private DBPPreferenceStore parentStore;
    private Map<String, String> properties;
    private Map<String, String> defaultProperties;
    private boolean dirty = false;

    public SimplePreferenceStore()
    {
        defaultProperties = new HashMap<>();
        properties = new HashMap<>();
    }

    protected SimplePreferenceStore(DBPPreferenceStore parentStore)
    {
        this();
        this.parentStore = parentStore;
        if (parentStore != null) {
            // FIXME: ? adding self as parent change listener produces too many events. And this seems to be senseless.
            // FIXME: but i'm not 100% sure.
            // FIXME: In any case we have to remove listener at dispose to avoid leaks and dead links.
            //parentStore.addPropertyChangeListener(this);
        }
    }

    public DBPPreferenceStore getParentStore()
    {
        return parentStore;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<String, String> properties)
    {
        this.properties = new HashMap<>(properties);
    }

    public Map<String, String> getDefaultProperties()
    {
        return defaultProperties;
    }

    public void setDefaultProperties(Map<String, String> defaultProperties)
    {
        this.defaultProperties = new HashMap<>(defaultProperties);
    }

    public void clear()
    {
        properties.clear();
    }

    @Override
    public void addPropertyChangeListener(@NotNull DBPPreferenceListener listener)
    {
        addListenerObject(listener);
    }

    @Override
    public void removePropertyChangeListener(@NotNull DBPPreferenceListener listener)
    {
        removeListenerObject(listener);
    }

    @Override
    public boolean contains(@NotNull String name)
    {
        return properties.containsKey(name);
    }

    @Override
    public boolean getBoolean(@NotNull String name)
    {
        return toBoolean(getString(name));
    }

    @Override
    public boolean getDefaultBoolean(@NotNull String name)
    {
        return toBoolean(getDefaultString(name));
    }


    @Override
    public double getDouble(@NotNull String name)
    {
        return toDouble(getString(name));
    }

    @Override
    public double getDefaultDouble(@NotNull String name)
    {
        return toDouble(getDefaultString(name));
    }

    @Override
    public float getFloat(@NotNull String name)
    {
        return toFloat(getString(name));
    }

    @Override
    public float getDefaultFloat(@NotNull String name)
    {
        return toFloat(getDefaultString(name));
    }


    @Override
    public int getInt(@NotNull String name)
    {
        return toInt(getString(name));
    }

    @Override
    public int getDefaultInt(@NotNull String name)
    {
        return toInt(getDefaultString(name));
    }


    @Override
    public long getLong(@NotNull String name)
    {
        return toLong(getString(name));
    }

    @Override
    public long getDefaultLong(@NotNull String name)
    {
        return toLong(getDefaultString(name));
    }

    @Override
    public String getString(@NotNull String name)
    {
        String value = properties.get(name);
        if (value == null && parentStore != null) {
            if (parentStore.isDefault(name)) {
                value = defaultProperties.get(name);
            }
            if (value == null) {
                value = parentStore.getString(name);
            }
        }
        return value;
    }

    @Override
    public String getDefaultString(@NotNull String name)
    {
        String value = defaultProperties.get(name);
        if (value == null && parentStore != null) {
            if (parentStore.isDefault(name)) {
                return parentStore.getDefaultString(name);
            } else {
                return "";
            }
        }
        return value;
    }

    @Override
    public boolean isDefault(@NotNull String name)
    {
        return (!properties.containsKey(name) && (defaultProperties.containsKey(name) || (parentStore != null && parentStore.isDefault(name))));
    }

    public boolean isSet(String name)
    {
        return properties.containsKey(name);
    }

    @Override
    public boolean needsSaving()
    {
        return dirty;
    }

    public String[] preferenceNames()
    {
        return properties.keySet().toArray(new String[0]);
    }

    @Override
    public void setDefault(@NotNull String name, double value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setDefault(@NotNull String name, float value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setDefault(@NotNull String name, int value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setDefault(@NotNull String name, long value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setDefault(@NotNull String name, @Nullable String value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setDefault(@NotNull String name, boolean value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setToDefault(@NotNull String name)
    {
        Object oldValue = properties.get(name);
        properties.remove(name);
        dirty = true;
        Object newValue = null;
        if (defaultProperties != null) {
            newValue = defaultProperties.get(name);
        }
        firePropertyChangeEvent(name, oldValue, newValue);
    }

    @Override
    public void setValue(@NotNull String name, double value)
    {
        double oldValue = getDouble(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, String.valueOf(value));
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(@NotNull String name, float value)
    {
        float oldValue = getFloat(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, String.valueOf(value));
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(@NotNull String name, int value)
    {
        int oldValue = getInt(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, String.valueOf(value));
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(@NotNull String name, long value)
    {
        long oldValue = getLong(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, String.valueOf(value));
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(@NotNull String name, @Nullable String value)
    {
        String oldValue = getString(name);
        if (oldValue == null || !oldValue.equals(value) || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(@NotNull String name, boolean value)
    {
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
        if (!(obj instanceof SimplePreferenceStore)) {
            return false;
        }
        SimplePreferenceStore copy = (SimplePreferenceStore)obj;
        return
            CommonUtils.equalObjects(parentStore, copy.parentStore) &&
            CommonUtils.equalObjects(properties, copy.properties) &&
            CommonUtils.equalObjects(defaultProperties, copy.defaultProperties);
    }
}
