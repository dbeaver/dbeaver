/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractUserPreferenceStore extends AbstractPreferenceStore {
    private static final Log log = Log.getLog(AbstractUserPreferenceStore.class);

    protected final DBPPreferenceStore parentStore;
    protected final Map<String, Object> userPreferences = new HashMap<>();

    public AbstractUserPreferenceStore(@NotNull DBPPreferenceStore parentStore) {
        this.parentStore = parentStore;
    }

    public void updateAllUserPreferences(@NotNull Map<String, Object> newUserPreferences) {
        this.userPreferences.clear();
        this.userPreferences.putAll(newUserPreferences);
    }

    @Override
    public String getString(@NotNull String name) {
        Object value = userPreferences.get(name);
        return value == null ? getDefaultString(name) : value.toString();
    }

    @Override
    public boolean contains(@NotNull String name) {
        return userPreferences.containsKey(name) || parentStore.contains(name);
    }

    @Override
    public boolean getBoolean(@NotNull String name) {
        return toBoolean(getString(name));
    }

    @Override
    public double getDouble(@NotNull String name) {
        return toDouble(getString(name));
    }

    @Override
    public float getFloat(@NotNull String name) {
        return toFloat(getString(name));
    }

    @Override
    public int getInt(@NotNull String name) {
        return toInt(getString(name));
    }

    @Override
    public long getLong(@NotNull String name) {
        return toLong(getString(name));
    }


    @Override
    public boolean getDefaultBoolean(@NotNull String name) {
        return toBoolean(getDefaultString(name));
    }

    @Override
    public double getDefaultDouble(@NotNull String name) {
        return toDouble(getDefaultString(name));
    }

    @Override
    public float getDefaultFloat(@NotNull String name) {
        return toFloat(getDefaultString(name));
    }

    @Override
    public int getDefaultInt(@NotNull String name) {
        return toInt(getDefaultString(name));
    }

    @Override
    public long getDefaultLong(@NotNull String name) {
        return toLong(getDefaultString(name));
    }


    @Override
    public void setDefault(@NotNull String name, double value) {
        setDefault(name, String.valueOf(value));
    }

    @Override
    public void setDefault(@NotNull String name, float value) {
        setDefault(name, String.valueOf(value));
    }

    @Override
    public void setDefault(@NotNull String name, int value) {
        setDefault(name, String.valueOf(value));
    }

    @Override
    public void setDefault(@NotNull String name, long value) {
        setDefault(name, String.valueOf(value));
    }

    @Override
    public void setDefault(@NotNull String name, @Nullable String defaultObject) {
        this.parentStore.setDefault(name, defaultObject);
    }

    @Override
    public void setDefault(@NotNull String name, boolean value) {
        setDefault(name, String.valueOf(value));
    }

    @Override
    public void setValue(@NotNull String name, double value) {
        setUserPreference(name, value);
    }

    @Override
    public void setValue(@NotNull String name, float value) {
        setUserPreference(name, value);
    }

    @Override
    public void setValue(@NotNull String name, int value) {
        setUserPreference(name, value);
    }

    @Override
    public void setValue(@NotNull String name, long value) {
        setUserPreference(name, value);
    }

    @Override
    public void setValue(@NotNull String name, @Nullable String value) {
        setUserPreference(name, value);
    }

    @Override
    public void setValue(@NotNull String name, boolean value) {
        setUserPreference(name, value);
    }

    protected abstract void setUserPreference(String name, Object value);

}
