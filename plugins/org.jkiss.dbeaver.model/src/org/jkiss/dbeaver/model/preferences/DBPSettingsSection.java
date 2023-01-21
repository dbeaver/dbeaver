/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

public interface DBPSettingsSection {

    String getName();

    DBPSettingsSection getSection(String sectionName);
    DBPSettingsSection[] getSections();

    DBPSettingsSection addNewSection(String name);
    void addSection(DBPSettingsSection section);

    String get(String key);
    String[] getArray(String key);
    boolean getBoolean(String key);
    double getDouble(String key) throws NumberFormatException;
    float getFloat(String key) throws NumberFormatException;
    int getInt(String key) throws NumberFormatException;
    long getLong(String key) throws NumberFormatException;

    void put(String key, String[] value);
    void put(String key, double value);
    void put(String key, float value);
    void put(String key, int value);
    void put(String key, long value);
    void put(String key, String value);
    void put(String key, boolean value);

}
