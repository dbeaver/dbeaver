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
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;

import java.io.IOException;

public class RemoteUserBundlePreferenceStore extends BundlePreferenceStore {

    private final DBPPreferenceStore remoteStore;

    public RemoteUserBundlePreferenceStore(@NotNull Bundle bundle, @NotNull DBPPreferenceStore remoteStore) {
        super(bundle);
        this.remoteStore = remoteStore;
    }

    @Override
    public boolean contains(@NotNull String name) {
        return remoteStore.contains(name);
    }

    @Override
    public boolean getBoolean(@NotNull String name) {
        boolean remoteValue = remoteStore.getBoolean(name);
        boolean bundleValue = super.getBoolean(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            putBoolean(name, remoteValue);
        }
        return remoteValue;
    }

    @Override
    public double getDouble(@NotNull String name) {
        double remoteValue = remoteStore.getDouble(name);
        double bundleValue = super.getDouble(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            putDouble(name, remoteValue);
        }
        return remoteValue;
    }

    @Override
    public float getFloat(@NotNull String name) {
        float remoteValue = remoteStore.getFloat(name);
        float bundleValue = super.getFloat(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            putFloat(name, remoteValue);
        }
        return remoteValue;
    }

    @Override
    public int getInt(@NotNull String name) {
        int remoteValue = remoteStore.getInt(name);
        int bundleValue = super.getInt(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            putInt(name, remoteValue);
        }
        return remoteValue;
    }

    @Override
    public long getLong(@NotNull String name) {
        long remoteValue = remoteStore.getLong(name);
        long bundleValue = super.getLong(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            putLong(name, remoteValue);
        }
        return remoteValue;
    }

    @Nullable
    @Override
    public String getString(@NotNull String name) {
        String remoteValue = remoteStore.getString(name);
        String bundleValue = super.getString(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            putString(name, remoteValue);
        }
        return remoteValue;
    }

    @Override
    public boolean getDefaultBoolean(@NotNull String name) {
        boolean remoteValue = remoteStore.getDefaultBoolean(name);
        boolean bundleValue = super.getDefaultBoolean(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            super.setDefault(name, remoteValue);
        }
        return remoteValue;
    }

    @Override
    public double getDefaultDouble(@NotNull String name) {
        double remoteValue = remoteStore.getDefaultDouble(name);
        double bundleValue = super.getDefaultDouble(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            super.setDefault(name, remoteValue);
        }
        return remoteValue;
    }

    @Override
    public float getDefaultFloat(@NotNull String name) {
        float remoteValue = remoteStore.getDefaultFloat(name);
        float bundleValue = super.getDefaultFloat(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            super.setDefault(name, remoteValue);
        }
        return remoteValue;
    }

    @Override
    public int getDefaultInt(@NotNull String name) {
        int remoteValue = remoteStore.getDefaultInt(name);
        int bundleValue = super.getDefaultInt(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            super.setDefault(name, remoteValue);
        }
        return remoteValue;
    }

    @Override
    public long getDefaultLong(@NotNull String name) {
        long remoteValue = remoteStore.getDefaultLong(name);
        long bundleValue = super.getDefaultLong(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            super.setDefault(name, remoteValue);
        }
        return remoteValue;
    }

    @Nullable
    @Override
    public String getDefaultString(@NotNull String name) {
        String remoteValue = remoteStore.getDefaultString(name);
        String bundleValue = super.getDefaultString(name);
        if (!CommonUtils.equalObjects(remoteValue, bundleValue)) {
            super.setDefault(name, remoteValue);
        }
        return remoteValue;
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
        super.setDefault(name, value);
    }

    @Override
    public void setDefault(@NotNull String name, float value) {
        remoteStore.setDefault(name, value);
        super.setDefault(name, value);
    }

    @Override
    public void setDefault(@NotNull String name, int value) {
        remoteStore.setDefault(name, value);
        super.setDefault(name, value);
    }

    @Override
    public void setDefault(@NotNull String name, long value) {
        remoteStore.setDefault(name, value);
        super.setDefault(name, value);
    }

    @Override
    public void setDefault(@NotNull String name, @Nullable String defaultObject) {
        remoteStore.setDefault(name, defaultObject);
        super.setDefault(name, defaultObject);
    }

    @Override
    public void setDefault(@NotNull String name, boolean value) {
        remoteStore.setDefault(name, value);
        super.setDefault(name, value);
    }

    @Override
    public void setToDefault(@NotNull String name) {
        remoteStore.setToDefault(name);
        super.setToDefault(name);
    }

    @Override
    public void setValue(@NotNull String name, double value) {
        remoteStore.setValue(name, value);
        super.setValue(name, value);
    }

    @Override
    public void setValue(@NotNull String name, float value) {
        remoteStore.setValue(name, value);
        super.setValue(name, value);
    }

    @Override
    public void setValue(@NotNull String name, int value) {
        remoteStore.setValue(name, value);
        super.setValue(name, value);
    }

    @Override
    public void setValue(@NotNull String name, long value) {
        remoteStore.setValue(name, value);
        super.setValue(name, value);
    }

    @Override
    public void setValue(@NotNull String name, @Nullable String value) {
        remoteStore.setValue(name, value);
        super.setValue(name, value);
    }

    @Override
    public void setValue(@NotNull String name, boolean value) {
        remoteStore.setValue(name, value);
        super.setValue(name, value);
    }


    @Override
    public void save() throws IOException {
        remoteStore.save();
        // never flush props, should always be in memory
    }
}
