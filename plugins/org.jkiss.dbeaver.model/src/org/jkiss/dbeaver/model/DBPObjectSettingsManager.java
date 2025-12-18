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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DBPObjectSettingsManager {

    private final Map<String, List<DBPObjectSettingsListener>> listeners = new ConcurrentHashMap<>();

    public void addListener(@Nullable String settingId, @NotNull DBPObjectSettingsListener listener) {
        listeners.computeIfAbsent(settingId, k -> new ArrayList<>()).add(listener);
    }

    public void removeListener(@Nullable String settingId, @NotNull DBPObjectSettingsListener listener) {
        List<DBPObjectSettingsListener> set = listeners.get(settingId);
        if (set != null) {
            set.remove(listener);
            if (set.isEmpty()) {
                listeners.remove(settingId, set);
            }
        }
    }

    public void notifySettingsChanged(@NotNull String objectId, @NotNull List<String> settingIds) {
        for (String settingId : settingIds) {
            List<DBPObjectSettingsListener> settingsListeners = this.listeners.get(settingId);
            if (settingsListeners != null) {
                for (DBPObjectSettingsListener listener : settingsListeners) {
                    listener.objectSettingUpdated(objectId, settingId);
                }
            }
        }
    }
}