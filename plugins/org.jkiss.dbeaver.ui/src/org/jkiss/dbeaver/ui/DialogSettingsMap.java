/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new LinkedHashSet<>();
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
        return null;
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        if (value instanceof Map) {
            IDialogSettings section = settings.getSection(key);
            if (section == null) {
                section = settings.addNewSection(key);
            }
            fillSection(section, (Map)value);
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
        DialogSettingsMap settingsMap = new DialogSettingsMap(settings);
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            settingsMap.put(entry.getKey(), entry.getValue());
        }
        return settingsMap;
    }

}