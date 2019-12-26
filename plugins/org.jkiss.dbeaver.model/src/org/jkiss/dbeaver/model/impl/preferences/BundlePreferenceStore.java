package org.jkiss.dbeaver.model.impl.preferences;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

import java.io.IOException;

public class BundlePreferenceStore extends AbstractPreferenceStore {

    private final IEclipsePreferences defaultProps;
    private final IEclipsePreferences props;
    private boolean dirty = false;

    public BundlePreferenceStore(Bundle bundle) {
        defaultProps = DefaultScope.INSTANCE.getNode(bundle.getSymbolicName());
        props = InstanceScope.INSTANCE.getNode(bundle.getSymbolicName());
    }

    @Override
    public boolean contains(String name) {
        return props.get(name, null) != null || defaultProps.get(name, null) != null;
    }

    @Override
    public boolean getBoolean(String name) {
        return props.get(name, null) != null ?
            props.getBoolean(name, BOOLEAN_DEFAULT_DEFAULT) :
            defaultProps.get(name, null) != null ?
                defaultProps.getBoolean(name, BOOLEAN_DEFAULT_DEFAULT) :
                BOOLEAN_DEFAULT_DEFAULT;
    }

    @Override
    public double getDouble(String name) {
        return props.get(name, null) != null ?
            props.getDouble(name, DOUBLE_DEFAULT_DEFAULT) :
            defaultProps.get(name, null) != null ?
                defaultProps.getDouble(name, DOUBLE_DEFAULT_DEFAULT) :
                DOUBLE_DEFAULT_DEFAULT;
    }

    @Override
    public float getFloat(String name) {
        return props.get(name, null) != null ?
            props.getFloat(name, FLOAT_DEFAULT_DEFAULT) :
            defaultProps.get(name, null) != null ?
                defaultProps.getFloat(name, FLOAT_DEFAULT_DEFAULT) :
                FLOAT_DEFAULT_DEFAULT;
    }

    @Override
    public int getInt(String name) {
        return props.get(name, null) != null ?
            props.getInt(name, INT_DEFAULT_DEFAULT) :
            defaultProps.get(name, null) != null ?
                defaultProps.getInt(name, INT_DEFAULT_DEFAULT) :
                INT_DEFAULT_DEFAULT;
    }

    @Override
    public long getLong(String name) {
        return props.get(name, null) != null ?
            props.getLong(name, LONG_DEFAULT_DEFAULT) :
            defaultProps.get(name, null) != null ?
                defaultProps.getLong(name, LONG_DEFAULT_DEFAULT) :
                LONG_DEFAULT_DEFAULT;
    }

    @Override
    public String getString(String name) {
        return props.get(name, null) != null ?
            props.get(name, STRING_DEFAULT_DEFAULT) :
            defaultProps.get(name, null) != null ?
                defaultProps.get(name, STRING_DEFAULT_DEFAULT) :
                STRING_DEFAULT_DEFAULT;
    }

    @Override
    public boolean getDefaultBoolean(String name) {
        return defaultProps.getBoolean(name, BOOLEAN_DEFAULT_DEFAULT);
    }

    @Override
    public double getDefaultDouble(String name) {
        return defaultProps.getDouble(name, DOUBLE_DEFAULT_DEFAULT);
    }

    @Override
    public float getDefaultFloat(String name) {
        return defaultProps.getFloat(name, FLOAT_DEFAULT_DEFAULT);
    }

    @Override
    public int getDefaultInt(String name) {
        return defaultProps.getInt(name, INT_DEFAULT_DEFAULT);
    }

    @Override
    public long getDefaultLong(String name) {
        return defaultProps.getLong(name, LONG_DEFAULT_DEFAULT);
    }

    @Override
    public String getDefaultString(String name) {
        return defaultProps.get(name, STRING_DEFAULT_DEFAULT);
    }

    @Override
    public boolean isDefault(String name) {
        return props.get(name, null) == null && defaultProps.get(name, null) != null;
    }

    @Override
    public boolean needsSaving() {
        return dirty;
    }

    @Override
    public void setDefault(String name, double value) {
        defaultProps.putDouble(name, value);
    }

    @Override
    public void setDefault(String name, float value) {
        defaultProps.putFloat(name, value);
    }

    @Override
    public void setDefault(String name, int value) {
        defaultProps.putInt(name, value);
    }

    @Override
    public void setDefault(String name, long value) {
        defaultProps.putLong(name, value);
    }

    @Override
    public void setDefault(String name, String defaultObject) {
        defaultProps.put(name, defaultObject);
    }

    @Override
    public void setDefault(String name, boolean value) {
        defaultProps.putBoolean(name, value);
    }

    @Override
    public void setToDefault(String name) {
        String oldValue = getString(name);
        String defaultValue = getDefaultString(name);
        props.remove(name);
        if (!CommonUtils.equalObjects(oldValue, defaultValue)) {
            dirty = true;
            firePropertyChangeEvent(name, oldValue, defaultValue);
        }
    }

    @Override
    public void setValue(String name, double value) {
        double oldValue = getDouble(name);
        if (oldValue == value) {
            return;
        }
        if (getDefaultDouble(name) == value) {
            props.remove(name);
        } else {
            props.putDouble(name, value);
        }
        dirty = true;
        firePropertyChangeEvent(name, oldValue, value);
    }

    @Override
    public void setValue(String name, float value) {
        float oldValue = getFloat(name);
        if (oldValue == value) {
            return;
        }
        if (getDefaultFloat(name) == value) {
            props.remove(name);
        } else {
            props.putFloat(name, value);
        }
        dirty = true;
        firePropertyChangeEvent(name, oldValue, value);
    }

    @Override
    public void setValue(String name, int value) {
        int oldValue = getInt(name);
        if (oldValue == value) {
            return;
        }
        if (getDefaultInt(name) == value) {
            props.remove(name);
        } else {
            props.putInt(name, value);
        }
        dirty = true;
        firePropertyChangeEvent(name, oldValue, value);
    }

    @Override
    public void setValue(String name, long value) {
        long oldValue = getLong(name);
        if (oldValue == value) {
            return;
        }
        if (getDefaultLong(name) == value) {
            props.remove(name);
        } else {
            props.putLong(name, value);
        }
        dirty = true;
        firePropertyChangeEvent(name, oldValue, value);
    }

    @Override
    public void setValue(String name, String value) {
        String oldValue = getString(name);
        if (CommonUtils.equalObjects(oldValue, value)) {
            return;
        }
        if (getDefaultString(name).equals(value)) {
            props.remove(name);
        } else {
            props.put(name, value);
        }
        dirty = true;
        firePropertyChangeEvent(name, oldValue, value);
    }

    @Override
    public void setValue(String name, boolean value) {
        boolean oldValue = getBoolean(name);
        if (oldValue == value) {
            return;
        }
        if (getDefaultBoolean(name) == value) {
            props.remove(name);
        } else {
            props.putBoolean(name, value);
        }
        dirty = true;
        firePropertyChangeEvent(name, oldValue ? Boolean.TRUE : Boolean.FALSE, value ? Boolean.TRUE : Boolean.FALSE);
    }

    @Override
    public void save() throws IOException {
        try {
            props.flush();
            defaultProps.flush();
        } catch (BackingStoreException e) {
            throw new IOException(e);
        }
    }

}
