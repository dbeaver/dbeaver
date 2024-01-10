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
package org.jkiss.dbeaver.model.preferences;

import java.io.IOException;

public interface DBPPreferenceStore {

    boolean contains(String name);

    boolean getBoolean(String name);
    double getDouble(String name);
    float getFloat(String name);
    int getInt(String name);
    long getLong(String name);
    String getString(String name);

    boolean getDefaultBoolean(String name);
    double getDefaultDouble(String name);
    float getDefaultFloat(String name);
    int getDefaultInt(String name);
    long getDefaultLong(String name);
    String getDefaultString(String name);

    boolean isDefault(String name);

    boolean needsSaving();

    void setDefault(String name, double value);
    void setDefault(String name, float value);
    void setDefault(String name, int value);
    void setDefault(String name, long value);
    void setDefault(String name, String defaultObject);
    void setDefault(String name, boolean value);
    void setToDefault(String name);

    void setValue(String name, double value);
    void setValue(String name, float value);
    void setValue(String name, int value);
    void setValue(String name, long value);
    void setValue(String name, String value);
    void setValue(String name, boolean value);

    void addPropertyChangeListener(DBPPreferenceListener listener);
    void removePropertyChangeListener(DBPPreferenceListener listener);
    void firePropertyChangeEvent(String name, Object oldValue, Object newValue);

    void save() throws IOException;

}
