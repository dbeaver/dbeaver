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
package org.jkiss.dbeaver.registry.project;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPObjectSettingsProvider;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.security.SMObjectType;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettingsUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Keeps information cache about project settings overrides.
 */
public abstract class BaseProjectSettings implements DBPObjectSettingsProvider {

    private static final Log log = Log.getLog(BaseProjectSettings.class);

    private final DBPProject project;
    private Map<SMObjectType, Map<String, Map<String, String>>> projectSettings;

    protected BaseProjectSettings(@NotNull DBPProject project) {
        this.project = project;
    }

    @Nullable
    @Override
    public Map<String, String> getObjectSettings(@NotNull SMObjectType objectType, @NotNull String objectId) {
        synchronized (this) {
            if (projectSettings == null) {
                try {
                    projectSettings = loadAllProjectSettings();
                } catch (DBException e) {
                    log.error("Error loading project settings. Setting to empty.", e);
                    projectSettings = new LinkedHashMap<>();
                }
            }
        }
        Map<String, Map<String, String>> cache = projectSettings.get(objectType);
        return cache == null ? null : cache.get(objectId);
    }

    @Override
    public void setObjectSettings(
        @NotNull SMObjectType objectType,
        @NotNull String objectId,
        @NotNull Map<String, String> settings
    ) throws DBException {
        saveProjectSettings(objectType, objectId, settings);

        // update local cache
        updateObjectSettingsCache(objectType, objectId, settings);
    }

    @Override
    public void clearObjectSettings(
        @NotNull SMObjectType objectType,
        @NotNull String objectId,
        @NotNull Set<String> settings
    ) throws DBException {
        Map<String, String> settingMap = new LinkedHashMap<>(settings.size());
        for (String id : settings) {
            settingMap.put(id, null);
        }
        saveProjectSettings(objectType, objectId, settingMap);

        deleteObjectSettingsCache(objectType, objectId, settings);
    }

    public void updateObjectSettingsCache(
        @NotNull SMObjectType objectType,
        @NotNull String objectId,
        Map<String, String> settingsToSet
    ) {
        if (projectSettings != null) {
            synchronized (this) {
                Map<String, String> settings = projectSettings
                    .computeIfAbsent(objectType, ot -> new LinkedHashMap<>())
                    .computeIfAbsent(objectId, k -> new LinkedHashMap<>());
                settings.putAll(settingsToSet);
            }
        }

        DataSourceNavigatorSettingsUtils.objectSettingUpdated(project, objectId, settingsToSet.keySet());
    }

    public void deleteObjectSettingsCache(
        @NotNull SMObjectType objectType,
        @NotNull String objectId,
        @Nullable Collection<String> settingIds
    ) {
        if (projectSettings == null) {
            return;
        }
        synchronized (this) {
            Map<String, Map<String, String>> cache = projectSettings.get(objectType);
            if (cache != null) {
                Map<String, String> settingRemoved;
                if (settingIds == null) {
                    settingRemoved = cache.remove(objectId);
                } else {
                    settingRemoved = new LinkedHashMap<>();
                    for (String settingId : settingIds) {
                        Map<String, String> settings = cache.get(objectId);
                        if (settings != null) {
                            settingRemoved.put(
                                settingId,
                                settings.remove(settingId));
                        }
                    }
                }
                DataSourceNavigatorSettingsUtils.objectSettingUpdated(project, objectId, settingRemoved.keySet());
            }
        }
    }

    @NotNull
    protected abstract Map<SMObjectType, Map<String, Map<String, String>>> loadAllProjectSettings() throws DBException;

    protected abstract void saveProjectSettings(
        @NotNull SMObjectType objectType,
        @NotNull String objectId,
        @NotNull Map<String, String> settings
    ) throws DBException;


}
