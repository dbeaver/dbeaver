/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.ListenerList;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

public abstract class AbstractPreferenceStore implements DBPPreferenceStore {

    public static final boolean BOOLEAN_DEFAULT_DEFAULT = false;
    public static final double DOUBLE_DEFAULT_DEFAULT = 0.0;
    public static final float FLOAT_DEFAULT_DEFAULT = 0.0f;
    public static final int INT_DEFAULT_DEFAULT = 0;
    public static final long LONG_DEFAULT_DEFAULT = 0L;
    public static final String STRING_DEFAULT_DEFAULT = ""; //$NON-NLS-1$
    public static final String TRUE = "true"; //$NON-NLS-1$
    public static final String FALSE = "false"; //$NON-NLS-1$

    private volatile transient ListenerList<DBPPreferenceListener> listenerList = null;

    @Override
    public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
        this.firePropertyChangeEvent(this, name, oldValue, newValue);
    }

    public void firePropertyChangeEvent(Object source, String name, Object oldValue, Object newValue) {
        final DBPPreferenceListener[] finalListeners = getListeners();
        // Do we need to fire an event
        if (finalListeners.length > 0 && !CommonUtils.equalObjects(oldValue, newValue)) {
            final DBPPreferenceListener.PreferenceChangeEvent pe = new DBPPreferenceListener.PreferenceChangeEvent(source, name, oldValue, newValue);
            for (DBPPreferenceListener finalListener : finalListeners) {
                finalListener.preferenceChange(pe);
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


    protected boolean toBoolean(String value) {
        return value != null && value.equals(AbstractPreferenceStore.TRUE);
    }

    protected double toDouble(String value) {
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

    protected float toFloat(String value) {
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

    protected int toInt(String value) {
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

    protected long toLong(String value) {
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

    protected final DBPPreferenceListener[] getListeners() {
        final ListenerList<DBPPreferenceListener> list = listenerList;
        if (list == null) {
            return new DBPPreferenceListener[0];
        }

        Object[] ol = list.getListeners();
        DBPPreferenceListener[] listeners = new DBPPreferenceListener[ol.length];
        for (int i = 0; i < list.size(); i++) {
            listeners[i] = (DBPPreferenceListener) ol[i];
        }
        return listeners;
    }

    protected synchronized final void addListenerObject(final DBPPreferenceListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException();
        }

        if (listenerList == null) {
            listenerList = new ListenerList<>(ListenerList.IDENTITY);
        }

        listenerList.add(listener);
    }

    protected synchronized final void removeListenerObject(final DBPPreferenceListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException();
        }

        if (listenerList != null) {
            listenerList.remove(listener);

            if (listenerList.isEmpty()) {
                listenerList = null;
            }
        }
    }

}
