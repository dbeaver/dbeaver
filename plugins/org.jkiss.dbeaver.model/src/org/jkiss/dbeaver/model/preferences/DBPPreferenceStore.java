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
package org.jkiss.dbeaver.model.preferences;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.io.IOException;

public interface DBPPreferenceStore {

    boolean contains(@NotNull String name);

    boolean getBoolean(@NotNull String name);
    double getDouble(@NotNull String name);
    float getFloat(@NotNull String name);
    int getInt(@NotNull String name);
    long getLong(@NotNull String name);
    @Nullable
    String getString(@NotNull String name);

    boolean getDefaultBoolean(@NotNull String name);
    double getDefaultDouble(@NotNull String name);
    float getDefaultFloat(@NotNull String name);
    int getDefaultInt(@NotNull String name);
    long getDefaultLong(@NotNull String name);
    @Nullable
    String getDefaultString(@NotNull String name);

    boolean isDefault(@NotNull String name);

    boolean needsSaving();

    void setDefault(@NotNull String name, double value);
    void setDefault(@NotNull String name, float value);
    void setDefault(@NotNull String name, int value);
    void setDefault(@NotNull String name, long value);
    void setDefault(@NotNull String name, @Nullable String defaultObject);
    void setDefault(@NotNull String name, boolean value);
    void setToDefault(@NotNull String name);

    void setValue(@NotNull String name, double value);
    void setValue(@NotNull String name, float value);
    void setValue(@NotNull String name, int value);
    void setValue(@NotNull String name, long value);
    void setValue(@NotNull String name, @Nullable String value);
    void setValue(@NotNull String name, boolean value);

    void addPropertyChangeListener(@NotNull DBPPreferenceListener listener);
    void removePropertyChangeListener(@NotNull DBPPreferenceListener listener);
    void firePropertyChangeEvent(@NotNull String name, @Nullable Object oldValue, @Nullable Object newValue);

    void save() throws IOException;

}
