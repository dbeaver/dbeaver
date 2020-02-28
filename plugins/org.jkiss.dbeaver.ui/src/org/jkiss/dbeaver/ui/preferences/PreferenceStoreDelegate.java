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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.io.IOException;

/**
 * PrefPageEmpty
 */
public class PreferenceStoreDelegate implements IPreferenceStore, IPersistentPreferenceStore
{
    private final DBPPreferenceStore delegate;

/*
    private static class PropertyChangeListenerDelegate implements IPropertyChangeListener {
        private final DBPPreferenceListener delegate;

        public PropertyChangeListenerDelegate(DBPPreferenceListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            delegate.preferenceChange(
                new DBPPreferenceListener.PreferenceChangeEvent(
                    event.getSource(),
                    event.getProperty(),
                    event.getOldValue(),
                    event.getNewValue()
                ));
        }
    }
*/

    public PreferenceStoreDelegate(DBPPreferenceStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public void save() throws IOException {
        delegate.save();
    }

    @Override
    public void addPropertyChangeListener(IPropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(IPropertyChangeListener listener) {

    }

    @Override
    public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
        delegate.firePropertyChangeEvent(name, oldValue, newValue);
    }

    @Override
    public boolean contains(String name) {
        return delegate.contains(name);
    }

    @Override
    public boolean getBoolean(String name) {
        return delegate.getBoolean(name);
    }

    @Override
    public boolean getDefaultBoolean(String name) {
        return delegate.getDefaultBoolean(name);
    }

    @Override
    public double getDefaultDouble(String name) {
        return delegate.getDefaultDouble(name);
    }

    @Override
    public float getDefaultFloat(String name) {
        return delegate.getDefaultFloat(name);
    }

    @Override
    public int getDefaultInt(String name) {
        return delegate.getDefaultInt(name);
    }

    @Override
    public long getDefaultLong(String name) {
        return delegate.getDefaultLong(name);
    }

    @Override
    public String getDefaultString(String name) {
        return delegate.getDefaultString(name);
    }

    @Override
    public double getDouble(String name) {
        return delegate.getDouble(name);
    }

    @Override
    public float getFloat(String name) {
        return delegate.getFloat(name);
    }

    @Override
    public int getInt(String name) {
        return delegate.getInt(name);
    }

    @Override
    public long getLong(String name) {
        return delegate.getLong(name);
    }

    @Override
    public String getString(String name) {
        return delegate.getString(name);
    }

    @Override
    public boolean isDefault(String name) {
        return delegate.isDefault(name);
    }

    @Override
    public boolean needsSaving() {
        return delegate.needsSaving();
    }

    @Override
    public void putValue(String name, String value) {
        delegate.setValue(name, value);
    }

    @Override
    public void setDefault(String name, double value) {
        delegate.setDefault(name, value);
    }

    @Override
    public void setDefault(String name, float value) {
        delegate.setDefault(name, value);
    }

    @Override
    public void setDefault(String name, int value) {
        delegate.setDefault(name, value);
    }

    @Override
    public void setDefault(String name, long value) {
        delegate.setDefault(name, value);
    }

    @Override
    public void setDefault(String name, String defaultObject) {
        delegate.setDefault(name, defaultObject);
    }

    @Override
    public void setDefault(String name, boolean value) {
        delegate.setDefault(name, value);
    }

    @Override
    public void setToDefault(String name) {
        delegate.setToDefault(name);
    }

    @Override
    public void setValue(String name, double value) {
        delegate.setValue(name, value);
    }

    @Override
    public void setValue(String name, float value) {
        delegate.setValue(name, value);
    }

    @Override
    public void setValue(String name, int value) {
        delegate.setValue(name, value);
    }

    @Override
    public void setValue(String name, long value) {
        delegate.setValue(name, value);
    }

    @Override
    public void setValue(String name, String value) {
        delegate.setValue(name, value);
    }

    @Override
    public void setValue(String name, boolean value) {
        delegate.setValue(name, value);
    }
}
