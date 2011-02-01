/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.utils;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Preference store which can be stored/loaded in any way.
 * Also it can use parent store to obtain values from it if this store do not contain the key.
 * However, save will always use THIS store, not parent.
 * Originally copied from standard PreferenceStore class
 */
public abstract class AbstractPreferenceStore extends EventManager implements IPersistentPreferenceStore
{
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

    public void addPropertyChangeListener(IPropertyChangeListener listener)
    {
        addListenerObject(listener);
    }

    public boolean contains(String name)
    {
        return (properties.containsKey(name) || defaultProperties
            .containsKey(name));
    }

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
                    public void run()
                    {
                        l.propertyChange(pe);
                    }
                });
            }
        }
    }

    public boolean getBoolean(String name)
    {
        return getBoolean(properties, true, false, name);
    }

    public boolean getDefaultBoolean(String name)
    {
        return getBoolean(defaultProperties, true, true, name);
    }

    private boolean getBoolean(Map<String, String> p, boolean useParent, boolean useParentDefault, String name)
    {
        String value = p != null ? p.get(name) : null;
        if (value == null) {
            return parentStore != null && useParent?
                (useParentDefault ? parentStore.getDefaultBoolean(name) : parentStore.getBoolean(name)) :
                BOOLEAN_DEFAULT_DEFAULT;
        }
        return value.equals(IPreferenceStore.TRUE);
    }

    public double getDouble(String name)
    {
        return getDouble(properties, true, false, name);
    }

    public double getDefaultDouble(String name)
    {
        return getDouble(defaultProperties, true, true, name);
    }

    private double getDouble(Map<String, String> p, boolean useParent, boolean useParentDefault, String name)
    {
        String value = p != null ? p.get(name) : null;
        if (value == null) {
            return parentStore != null && useParent ?
                (useParentDefault ? parentStore.getDefaultDouble(name) : parentStore.getDouble(name)) :
                DOUBLE_DEFAULT_DEFAULT;
        }
        double ival = DOUBLE_DEFAULT_DEFAULT;
        try {
            ival = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // do nothing
        }
        return ival;
    }

    public float getFloat(String name)
    {
        return getFloat(properties, true, false, name);
    }

    public float getDefaultFloat(String name)
    {
        return getFloat(defaultProperties, true, true, name);
    }

    private float getFloat(Map<String, String> p, boolean useParent, boolean useParentDefault, String name)
    {
        String value = p != null ? p.get(name) : null;
        if (value == null) {
            return parentStore != null && useParent ?
                (useParentDefault ? parentStore.getDefaultFloat(name) : parentStore.getFloat(name)) :
                FLOAT_DEFAULT_DEFAULT;
        }
        float ival = FLOAT_DEFAULT_DEFAULT;
        try {
            ival = Float.parseFloat(value);
        } catch (NumberFormatException e) {
            // do nothing
        }
        return ival;
    }

    public int getInt(String name)
    {
        return getInt(properties, true, false, name);
    }

    public int getDefaultInt(String name)
    {
        return getInt(defaultProperties, true, true, name);
    }

    private int getInt(Map<String, String> p, boolean useParent, boolean useParentDefault, String name)
    {
        String value = p != null ? p.get(name) : null;
        if (value == null) {
            return parentStore != null && useParent ?
                (useParentDefault ? parentStore.getDefaultInt(name) : parentStore.getInt(name)) :
                INT_DEFAULT_DEFAULT;
        }
        int ival = 0;
        try {
            ival = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // do nothing
        }
        return ival;
    }

    public long getLong(String name)
    {
        return getLong(properties, true, false, name);
    }

    public long getDefaultLong(String name)
    {
        return getLong(defaultProperties, true, true, name);
    }

    private long getLong(Map<String, String> p, boolean useParent, boolean useParentDefault, String name)
    {
        String value = p != null ? p.get(name) : null;
        if (value == null) {
            return parentStore != null && useParent ?
                (useParentDefault ? parentStore.getDefaultLong(name) : parentStore.getLong(name)) :
                LONG_DEFAULT_DEFAULT;
        }
        long ival = LONG_DEFAULT_DEFAULT;
        try {
            ival = Long.parseLong(value);
        } catch (NumberFormatException e) {
            // do nothing
        }
        return ival;
    }

    public String getString(String name)
    {
        return getString(properties, true, false, name);
    }

    public String getDefaultString(String name)
    {
        return getString(defaultProperties, true, true, name);
    }

    private String getString(Map<String, String> p, boolean useParent, boolean useParentDefault, String name)
    {
        String value = p != null ? p.get(name) : null;
        if (value == null) {
            return parentStore != null && useParent ?
                (useParentDefault ? parentStore.getDefaultString(name) : parentStore.getString(name)) :
                STRING_DEFAULT_DEFAULT;
        }
        return value;
    }

    public boolean isDefault(String name)
    {
        return (!properties.containsKey(name) && defaultProperties
            .containsKey(name));
    }

    public boolean isSet(String name)
    {
        return properties.containsKey(name);
    }

    public boolean needsSaving()
    {
        return dirty;
    }

    public String[] preferenceNames()
    {
        return properties.keySet().toArray(new String[properties.size()]);
    }

    public void putValue(String name, String value)
    {
        String oldValue = getString(properties, false, false, name);
        if (oldValue == null || !oldValue.equals(value)) {
            setValue(properties, name, value);
            dirty = true;
        }
    }

    public void removePropertyChangeListener(IPropertyChangeListener listener)
    {
        removeListenerObject(listener);
    }

    public void setDefault(String name, double value)
    {
        setValue(defaultProperties, name, value);
    }

    public void setDefault(String name, float value)
    {
        setValue(defaultProperties, name, value);
    }

    public void setDefault(String name, int value)
    {
        setValue(defaultProperties, name, value);
    }

    public void setDefault(String name, long value)
    {
        setValue(defaultProperties, name, value);
    }

    public void setDefault(String name, String value)
    {
        setValue(defaultProperties, name, value);
    }

    public void setDefault(String name, boolean value)
    {
        setValue(defaultProperties, name, value);
    }

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

    public void setValue(String name, double value)
    {
        double oldValue = getDouble(properties, false, false, name);
        if (oldValue != value || !isSet(name)) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    public void setValue(String name, float value)
    {
        float oldValue = getFloat(properties, false, false, name);
        if (oldValue != value || !isSet(name)) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    public void setValue(String name, int value)
    {
        int oldValue = getInt(properties, false, false, name);
        if (oldValue != value || !isSet(name)) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    public void setValue(String name, long value)
    {
        long oldValue = getLong(properties, false, false, name);
        if (oldValue != value || !isSet(name)) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    public void setValue(String name, String value)
    {
        String oldValue = getString(properties, false, false, name);
        if (oldValue == null || !oldValue.equals(value) || !isSet(name)) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue, value);
        }
    }

    public void setValue(String name, boolean value)
    {
        boolean oldValue = getBoolean(properties, false, false, name);
        if (oldValue != value || !isSet(name)) {
            setValue(properties, name, value);
            dirty = true;
            firePropertyChangeEvent(name, oldValue ? Boolean.TRUE
                : Boolean.FALSE, value ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    private void setValue(Map<String, String> p, String name, double value)
    {
        p.put(name, Double.toString(value));
    }

    private void setValue(Map<String, String> p, String name, float value)
    {
        p.put(name, Float.toString(value));
    }

    private void setValue(Map<String, String> p, String name, int value)
    {
        p.put(name, Integer.toString(value));
    }

    private void setValue(Map<String, String> p, String name, long value)
    {
        p.put(name, Long.toString(value));
    }

    private void setValue(Map<String, String> p, String name, String value)
    {
        p.put(name, value);
    }

    private void setValue(Map<String, String> p, String name, boolean value)
    {
        p.put(name, value ? IPreferenceStore.TRUE : IPreferenceStore.FALSE);
    }

}
