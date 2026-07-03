/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.io.IOException;

public class RemoteUserBundlePreferenceStore extends AbstractPreferenceStore {

    private final DBPPreferenceStore remoteStore;

    public RemoteUserBundlePreferenceStore(@NotNull DBPPreferenceStore remoteStore) {
        this.remoteStore = remoteStore;
    }

    @Override
    public boolean contains(@NotNull String name) {
        return remoteStore.contains(name);
    }

    @Override
    public boolean getBoolean(@NotNull String name) {
        return remoteStore.getBoolean(name);
    }

    @Override
    public double getDouble(@NotNull String name) {
        return remoteStore.getDouble(name);
    }

    @Override
    public float getFloat(@NotNull String name) {
        return remoteStore.getFloat(name);
    }

    @Override
    public int getInt(@NotNull String name) {
        return remoteStore.getInt(name);
    }

    @Override
    public long getLong(@NotNull String name) {
        return remoteStore.getLong(name);
    }

    @Nullable
    @Override
    public String getString(@NotNull String name) {
        return remoteStore.getString(name);
    }

    @Override
    public boolean getDefaultBoolean(@NotNull String name) {
        return remoteStore.getDefaultBoolean(name);
    }

    @Override
    public double getDefaultDouble(@NotNull String name) {
        return remoteStore.getDefaultDouble(name);
    }

    @Override
    public float getDefaultFloat(@NotNull String name) {
        return remoteStore.getDefaultFloat(name);
    }

    @Override
    public int getDefaultInt(@NotNull String name) {
        return remoteStore.getDefaultInt(name);
    }

    @Override
    public long getDefaultLong(@NotNull String name) {
        return remoteStore.getDefaultLong(name);
    }

    @Nullable
    @Override
    public String getDefaultString(@NotNull String name) {
        return remoteStore.getDefaultString(name);
    }

    @Override
    public boolean isDefault(@NotNull String name) {
        return remoteStore.isDefault(name);
    }

    @Override
    public boolean needsSaving() {
        return remoteStore.needsSaving();
    }

    @Override
    public void setDefault(@NotNull String name, double value) {
        remoteStore.setDefault(name, value);
    }

    @Override
    public void setDefault(@NotNull String name, float value) {
        remoteStore.setDefault(name, value);
    }

    @Override
    public void setDefault(@NotNull String name, int value) {
        remoteStore.setDefault(name, value);
    }

    @Override
    public void setDefault(@NotNull String name, long value) {
        remoteStore.setDefault(name, value);
    }

    @Override
    public void setDefault(@NotNull String name, @Nullable String defaultObject) {
        remoteStore.setDefault(name, defaultObject);
    }

    @Override
    public void setDefault(@NotNull String name, boolean value) {
        remoteStore.setDefault(name, value);
    }

    @Override
    public void setToDefault(@NotNull String name) {
        remoteStore.setToDefault(name);
    }

    @Override
    public void setValue(@NotNull String name, double value) {
        double oldValue = getDouble(name);
        remoteStore.setValue(name, value);
        firePropertyChangeEvent(name, oldValue, value);
    }

    @Override
    public void setValue(@NotNull String name, float value) {
        float oldValue = getFloat(name);
        remoteStore.setValue(name, value);
        firePropertyChangeEvent(name, oldValue, value);
    }

    @Override
    public void setValue(@NotNull String name, int value) {
        int oldValue = getInt(name);
        remoteStore.setValue(name, value);
        firePropertyChangeEvent(name, oldValue, value);
    }

    @Override
    public void setValue(@NotNull String name, long value) {
        long oldValue = getLong(name);
        remoteStore.setValue(name, value);
        firePropertyChangeEvent(name, oldValue, value);
    }

    @Override
    public void setValue(@NotNull String name, @Nullable String value) {
        String oldValue = getString(name);
        remoteStore.setValue(name, value);
        firePropertyChangeEvent(name, oldValue, value);
    }

    @Override
    public void setValue(@NotNull String name, boolean value) {
        boolean oldValue = getBoolean(name);
        remoteStore.setValue(name, value);
        firePropertyChangeEvent(name, oldValue, value);
    }


    @Override
    public void save() throws IOException {
        remoteStore.save();
    }
}
