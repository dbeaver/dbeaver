package org.jkiss.dbeaver.model.impl.preferences;

import org.eclipse.core.commands.common.EventManager;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

public abstract class AbstractPreferenceStore extends EventManager implements DBPPreferenceStore {

    public static final boolean BOOLEAN_DEFAULT_DEFAULT = false;
    public static final double DOUBLE_DEFAULT_DEFAULT = 0.0;
    public static final float FLOAT_DEFAULT_DEFAULT = 0.0f;
    public static final int INT_DEFAULT_DEFAULT = 0;
    public static final long LONG_DEFAULT_DEFAULT = 0L;
    public static final String STRING_DEFAULT_DEFAULT = ""; //$NON-NLS-1$
    public static final String TRUE = "true"; //$NON-NLS-1$
    public static final String FALSE = "false"; //$NON-NLS-1$

    @Override
    public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
        this.firePropertyChangeEvent(this, name, oldValue, newValue);
    }

    public void firePropertyChangeEvent(Object source, String name, Object oldValue, Object newValue) {
        final Object[] finalListeners = getListeners();
        // Do we need to fire an event.
        if (finalListeners.length > 0 && !CommonUtils.equalObjects(oldValue, newValue)) {
            final DBPPreferenceListener.PreferenceChangeEvent pe = new DBPPreferenceListener.PreferenceChangeEvent(source, name, oldValue, newValue);
            for (int i = 0; i < finalListeners.length; ++i) {
                final DBPPreferenceListener l = (DBPPreferenceListener) finalListeners[i];
                l.preferenceChange(pe);
            }
        }
    }

    @Override
    public void addPropertyChangeListener(DBPPreferenceListener listener) {
        addListenerObject(listener);
    }

    @Override
    public void removePropertyChangeListener(DBPPreferenceListener listener) {
        removeListenerObject(listener);
    }

}
