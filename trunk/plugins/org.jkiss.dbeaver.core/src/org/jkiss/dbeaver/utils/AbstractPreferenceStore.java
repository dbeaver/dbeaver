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
package org.jkiss.dbeaver.utils;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Preference store which can be stored/loaded in any way.
 * Also it can use parent store to obtain values from it if this store do not contain the key.
 * However, save will always use THIS store, not parent.
 * Originally copied from standard PreferenceStore class
 */
public abstract class AbstractPreferenceStore extends EventManager implements IPersistentPreferenceStore, IPropertyChangeListener {
    private IPreferenceStore parentStore;
    private Map<String, String> properties;
    private Map<String, String> defaultProperties;
    private boolean dirty = false;

    public AbstractPreferenceStore()
    {
        defaultProperties = new HashMap<String, String>();
        properties = new HashMap<String, String>();
    }

    protected AbstractPreferenceStore(IPreferenceStore parentStore)
    {
        this();
        this.parentStore = parentStore;
        if (parentStore != null) {
            parentStore.addPropertyChangeListener(this);
        }
    }

    public IPreferenceStore getParentStore()
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
    public void addPropertyChangeListener(IPropertyChangeListener listener)
    {
        addListenerObject(listener);
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
            final PropertyChangeEvent pe = new PropertyChangeEvent(this, name,
                oldValue, newValue);
            for (int i = 0; i < finalListeners.length; ++i) {
                final IPropertyChangeListener l = (IPropertyChangeListener) finalListeners[i];
                SafeRunnable.run(new SafeRunnable(JFaceResources.getString("PreferenceStore.changeError")) //$NON-NLS-1$
                {
                    @Override
                    public void run()
                    {
                        l.propertyChange(pe);
                    }
                });
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
        return value != null && value.equals(IPreferenceStore.TRUE);
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
    public void putValue(String name, String value)
    {
        String oldValue = getString(name);
        if (oldValue == null || !oldValue.equals(value)) {
            properties.put(name, String.valueOf(value));
            dirty = true;
        }
    }

    @Override
    public void removePropertyChangeListener(IPropertyChangeListener listener)
    {
        removeListenerObject(listener);
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
    public void save() throws IOException
    {

    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        for (Object listener : getListeners()) {
            ((IPropertyChangeListener)listener).propertyChange(event);
        }
    }
}
