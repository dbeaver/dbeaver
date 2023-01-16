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

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DialogSettingsMap
 */
public class DialogSettingsMap extends AbstractMap<String, Object> {
    private static final Log log = Log.getLog(DialogSettingsMap.class);

    private final IDialogSettings settings;

    public DialogSettingsMap(IDialogSettings settings) {
        this.settings = settings;
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        final Set<Entry<String, Object>> entries = new LinkedHashSet<>();
        for (IDialogSettings section : ArrayUtils.safeArray(settings.getSections())) {
            entries.add(new SimpleEntry<>(section.getName(), new DialogSettingsMap(section)));
        }
        for (Entry<String, String> entry : getItemsInternal().entrySet()) {
            entries.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
        }
        for (Entry<String, String[]> entry : getArrayItemsInternal().entrySet()) {
            entries.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
        }
        return entries;
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
    @SuppressWarnings("unchecked")
    public Object putIfAbsent(String key, Object value) {
        if (value instanceof Map) {
            final DialogSettingsMap map = new DialogSettingsMap(settings.addNewSection(key));
            map.putAll((Map<String, String>) value);
            return map;
        } else if (ArrayUtils.isArray(value)) {
            final Object[] src = (Object[]) value;
            final String[] dst = new String[src.length];
            for (int i = 0; i < src.length; i++) {
                dst[i] = CommonUtils.toString(src[i]);
            }
            settings.put(key, dst);
        } else {
            settings.put(key, CommonUtils.toString(value));
        }
        return value;
    }

    @Override
    public boolean remove(Object key, Object value) {
        return false;
    }

    @Override
    public int hashCode() {
        return settings.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DialogSettingsMap && settings.equals(((DialogSettingsMap) o).settings);
    }

    @NotNull
    private Map<String, String> getItemsInternal() {
        try {
            return BeanUtils.getFieldValue(settings, "items");
        } catch (Throwable e) {
            log.error("Can't read items from settings", e);
            return Collections.emptyMap();
        }
    }

    @NotNull
    private Map<String, String[]> getArrayItemsInternal() {
        try {
            return BeanUtils.getFieldValue(settings, "arrayItems");
        } catch (Throwable e) {
            log.error("Can't read array items from settings", e);
            return Collections.emptyMap();
        }
    }
}