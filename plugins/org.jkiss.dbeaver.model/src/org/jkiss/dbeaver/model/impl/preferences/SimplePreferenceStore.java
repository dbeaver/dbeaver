/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
        this.properties = properties;
    }

    public Map<String, String> getDefaultProperties()
    {
        return defaultProperties;
    }

    public void setDefaultProperties(Map<String, String> defaultProperties)
    {
        this.defaultProperties = defaultProperties;
    }

    public void clear()
    {
        properties.clear();
    }

    @Override
    public void addPropertyChangeListener(DBPPreferenceListener listener)
    {
        addListenerObject(listener);
    }

    @Override
    public void removePropertyChangeListener(DBPPreferenceListener listener)
    {
        removeListenerObject(listener);
    }

    @Override
    public boolean contains(String name)
    {
        return (properties.containsKey(name) || defaultProperties
            .containsKey(name));
    }

    @Override
    public boolean getBoolean(String name)
    {
        return toBoolean(getString(name));
    }

    @Override
    public boolean getDefaultBoolean(String name)
    {
        return toBoolean(getDefaultString(name));
    }

    private boolean toBoolean(String value)
    {
        return value != null && value.equals(AbstractPreferenceStore.TRUE);
    }

    @Override
    public double getDouble(String name)
    {
        return toDouble(getString(name));
    }

    @Override
    public double getDefaultDouble(String name)
    {
        return toDouble(getDefaultString(name));
    }

    private double toDouble(String value)
    {
        double ival = DOUBLE_DEFAULT_DEFAULT;
        if (!CommonUtils.isEmpty(value)) {
            try {
                ival = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        return ival;
    }

    @Override
    public float getFloat(String name)
    {
        return toFloat(getString(name));
    }

    @Override
    public float getDefaultFloat(String name)
    {
        return toFloat(getDefaultString(name));
    }

    private float toFloat(String value)
    {
        float ival = FLOAT_DEFAULT_DEFAULT;
        if (!CommonUtils.isEmpty(value)) {
            try {
                ival = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        return ival;
    }

    @Override
    public int getInt(String name)
    {
        return toInt(getString(name));
    }

    @Override
    public int getDefaultInt(String name)
    {
        return toInt(getDefaultString(name));
    }

    private int toInt(String value)
    {
        int ival = INT_DEFAULT_DEFAULT;
        if (!CommonUtils.isEmpty(value)) {
            try {
                ival = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        return ival;
    }

    @Override
    public long getLong(String name)
    {
        return toLong(getString(name));
    }

    @Override
    public long getDefaultLong(String name)
    {
        return toLong(getDefaultString(name));
    }

    private long toLong(String value)
    {
        long ival = LONG_DEFAULT_DEFAULT;
        if (!CommonUtils.isEmpty(value)) {
            try {
                ival = Long.parseLong(value);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        return ival;
    }

    @Override
    public String getString(String name)
    {
        String value = properties.get(name);
        if (value == null) {
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
    public String getDefaultString(String name)
    {
        String value = defaultProperties.get(name);
        if (value == null) {
            if (parentStore.isDefault(name)) {
                return parentStore.getDefaultString(name);
            } else {
                return "";
            }
        }
        return value;
    }

    @Override
    public boolean isDefault(String name)
    {
        return (!properties.containsKey(name) && (defaultProperties.containsKey(name) || parentStore.isDefault(name)));
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
        return properties.keySet().toArray(new String[properties.size()]);
    }

    @Override
    public void setDefault(String name, double value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setDefault(String name, float value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setDefault(String name, int value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setDefault(String name, long value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setDefault(String name, String value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setDefault(String name, boolean value)
    {
        defaultProperties.put(name, String.valueOf(value));
    }

    @Override
    public void setToDefault(String name)
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
    public void setValue(String name, double value)
    {
        double oldValue = getDouble(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, String.valueOf(value));
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, float value)
    {
        float oldValue = getFloat(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, String.valueOf(value));
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, int value)
    {
        int oldValue = getInt(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, String.valueOf(value));
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, long value)
    {
        long oldValue = getLong(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, String.valueOf(value));
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, String value)
    {
        String oldValue = getString(name);
        if (oldValue == null || !oldValue.equals(value) || !isSet(name)) {
            properties.put(name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    @Override
    public void setValue(String name, boolean value)
    {
        boolean oldValue = getBoolean(name);
        if (oldValue != value || !isSet(name)) {
            properties.put(name, String.valueOf(value));
            dirty = true;
            firePropertyChangeEvent(name, oldValue ? Boolean.TRUE
                : Boolean.FALSE, value ? Boolean.TRUE : Boolean.FALSE);
        }
    }

}
