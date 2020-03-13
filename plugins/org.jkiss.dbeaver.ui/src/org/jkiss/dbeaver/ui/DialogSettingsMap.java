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
import org.jkiss.code.NotNull;
import org.jkiss.utils.CommonUtils;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * DialogSettingsMap
 */
public class DialogSettingsMap extends AbstractMap<String, Object> {

    private final IDialogSettings settings;

    public DialogSettingsMap(IDialogSettings settings) {
        this.settings = settings;
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> sectionSet = new LinkedHashSet<>();
        IDialogSettings[] sections = settings.getSections();
        if (sections != null) {
            for (IDialogSettings section : sections) {
                sectionSet.add(new SimpleEntry<>(section.getName(), new DialogSettingsMap(section)));
            }
        }
        return sectionSet;
    }

    @Override
    public Object get(Object key) {
        return getOrDefault(key, null);
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        String keyValue = settings.get((String) key);
        if (keyValue != null) {
            return keyValue;
        }
        IDialogSettings section = settings.getSection((String) key);
        if (section != null) {
            return new DialogSettingsMap(section);
        }
        return defaultValue;
    }

    @Override
    public Object put(String key, Object value) {
        return putIfAbsent(key, value);
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        if (value instanceof Map) {
            IDialogSettings section = settings.addNewSection(key);
            return fillSection(section, (Map)value);
        } else {
            settings.put(key, CommonUtils.toString(value));
        }
        return value;
    }

    @Override
    public boolean remove(Object key, Object value) {
        return false;
    }

    private DialogSettingsMap fillSection(IDialogSettings section, Map<String, Object> value) {
        DialogSettingsMap settingsMap = new DialogSettingsMap(section);
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            settingsMap.put(entry.getKey(), entry.getValue());
        }
        return settingsMap;
    }

    @Override
    public int hashCode() {
        return settings.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DialogSettingsMap && settings.equals(((DialogSettingsMap) o).settings);
    }
}