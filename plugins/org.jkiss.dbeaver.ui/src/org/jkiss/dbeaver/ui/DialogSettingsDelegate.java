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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.jkiss.dbeaver.model.preferences.DBPSettingsSection;

/**
 * DBPSettingsSection impl which delegates everything to IDialogSettings
 */
public class DialogSettingsDelegate implements DBPSettingsSection {

    private final IDialogSettings dialogSettings;

    public DialogSettingsDelegate(IDialogSettings dialogSettings) {
        this.dialogSettings = dialogSettings;
    }

    @Override
    public String getName() {
        return dialogSettings.getName();
    }

    @Override
    public DBPSettingsSection getSection(String sectionName) {
        IDialogSettings section = dialogSettings.getSection(sectionName);
        return section == null ? null : new DialogSettingsDelegate(section);
    }

    @Override
    public DBPSettingsSection[] getSections() {
        IDialogSettings[] sections = dialogSettings.getSections();
        if (sections == null) {
            return null;
        }
        DBPSettingsSection[] ss = new DBPSettingsSection[sections.length];
        for (int i = 0; i < sections.length; i++) {
            ss[i] = new DialogSettingsDelegate(sections[i]);
        }
        return ss;
    }

    @Override
    public DBPSettingsSection addNewSection(String name) {
        return new DialogSettingsDelegate(
            this.dialogSettings.addNewSection(name));
    }

    @Override
    public void addSection(DBPSettingsSection section) {
        if (section instanceof DialogSettingsDelegate) {
            dialogSettings.addSection(((DialogSettingsDelegate) section).dialogSettings);
        } else {
            throw new IllegalArgumentException("Section must extend DialogSettingsDelegate");
        }
    }

    @Override
    public String get(String key) {
        return dialogSettings.get(key);
    }

    @Override
    public String[] getArray(String key) {
        return dialogSettings.getArray(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return dialogSettings.getBoolean(key);
    }

    @Override
    public double getDouble(String key) throws NumberFormatException {
        return dialogSettings.getDouble(key);
    }

    @Override
    public float getFloat(String key) throws NumberFormatException {
        return dialogSettings.getFloat(key);
    }

    @Override
    public int getInt(String key) throws NumberFormatException {
        return dialogSettings.getInt(key);
    }

    @Override
    public long getLong(String key) throws NumberFormatException {
        return dialogSettings.getLong(key);
    }

    @Override
    public void put(String key, String[] value) {
        dialogSettings.put(key, value);
    }

    @Override
    public void put(String key, double value) {
        dialogSettings.put(key, value);
    }

    @Override
    public void put(String key, float value) {
        dialogSettings.put(key, value);
    }

    @Override
    public void put(String key, int value) {
        dialogSettings.put(key, value);
    }

    @Override
    public void put(String key, long value) {
        dialogSettings.put(key, value);
    }

    @Override
    public void put(String key, String value) {
        dialogSettings.put(key, value);
    }

    @Override
    public void put(String key, boolean value) {
        dialogSettings.put(key, value);
    }
}
