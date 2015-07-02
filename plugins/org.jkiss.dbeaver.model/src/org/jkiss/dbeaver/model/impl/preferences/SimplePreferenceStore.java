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
package org.jkiss.dbeaver.model.impl.preferences;

import org.jkiss.dbeaver.model.DBPPreferenceListener;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

    import java.util.HashMap;
import java.util.Map;

/**
 * Preference store which can be stored/loaded in any way.
 * Also it can use parent store to obtain values from it if this store do not contain the key.
 * However, save will always use THIS store, not parent.
 * Originally copied from standard PreferenceStore class
 */
public abstract class SimplePreferenceStore extends AbstractPreferenceStore implements DBPPreferenceListener {
    private DBPPreferenceStore parentStore;
    private Map<String, String> properties;
    private Map<String, String> defaultProperties;
    private boolean dirty = false;

    public SimplePreferenceStore()
    {
        defaultProperties = new HashMap<String, String>();
        properties = new HashMap<String, String>();
    }

    protected SimplePreferenceStore(DBPPreferenceStore parentStore)
    {
        this();
        this.parentStore = parentStore;
        if (parentStore != null) {
            parentStore.addPropertyChangeListener(this);
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
    public void firePropertyChangeEvent(String name, Object oldValue, Object newValue)
    {
        final Object[] finalListeners = getListeners();
        // Do we need to fire an event.
        if (finalListeners.length > 0
            && (oldValue == null || !oldValue.equals(newValue))) {
            final PreferenceChangeEvent pe = new PreferenceChangeEvent(this, name,
                oldValue, newValue);
            for (int i = 0; i < finalListeners.length; ++i) {
                final DBPPreferenceListener l = (DBPPreferenceListener) finalListeners[i];
                l.preferenceChange(pe);
            }
        }
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

    @Override
    public void preferenceChange(PreferenceChangeEvent event)
    {
        for (Object listener : getListeners()) {
            ((DBPPreferenceListener)listener).preferenceChange(event);
        }
    }
}
