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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectSettingsProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.security.SMObjectType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DataSourceNavigatorSettingsUtils {
    private static final Log log = Log.getLog(DataSourceNavigatorSettingsUtils.class);

    public static void loadSettingsFromMap(@NotNull DataSourceNavigatorSettings navSettings, @NotNull Map<String, ?> objectMap) {
        navSettings.setShowSystemObjects(JSONUtils.getBoolean(objectMap, DataSourceNavigatorSettings.ATTR_NAVIGATOR_SHOW_SYSTEM_OBJECTS));
        navSettings.setShowUtilityObjects(JSONUtils.getBoolean(objectMap, DataSourceNavigatorSettings.ATTR_NAVIGATOR_SHOW_UTIL_OBJECTS));
        navSettings.setShowOnlyEntities(JSONUtils.getBoolean(objectMap, DataSourceNavigatorSettings.ATTR_NAVIGATOR_SHOW_ONLY_ENTITIES));
        navSettings.setHideFolders(JSONUtils.getBoolean(objectMap, DataSourceNavigatorSettings.ATTR_NAVIGATOR_HIDE_FOLDERS));
        navSettings.setHideSchemas(JSONUtils.getBoolean(objectMap, DataSourceNavigatorSettings.ATTR_NAVIGATOR_HIDE_SCHEMAS));
        navSettings.setHideVirtualModel(JSONUtils.getBoolean(objectMap, DataSourceNavigatorSettings.ATTR_NAVIGATOR_HIDE_VIRTUAL));
        navSettings.setMergeEntities(JSONUtils.getBoolean(objectMap, DataSourceNavigatorSettings.ATTR_NAVIGATOR_MERGE_ENTITIES));
    }

    public static void updateCustomNavigatorSettings(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DataSourceNavigatorSettings settings
    ) throws DBException {
        DBPObjectSettingsProvider settingsProvider = DBUtils.getAdapter(DBPObjectSettingsProvider.class, dataSource.getProject());
        if (settingsProvider == null) {
            return;
        }
        Map<String, String> settingsMap = DataSourceNavigatorSettings.saveSettingsToMap(settings);

        settingsProvider.setObjectSettings(
            SMObjectType.datasource,
            dataSource.getId(),
            settingsMap
        );
    }

    public static void clearCustomNavigatorSettings(
        @NotNull DBPDataSourceContainer dataSource
    ) throws DBException {
        DBPObjectSettingsProvider settingsProvider = DBUtils.getAdapter(DBPObjectSettingsProvider.class, dataSource.getProject());
        if (settingsProvider == null) {
            return;
        }
        settingsProvider.clearObjectSettings(
            SMObjectType.datasource,
            dataSource.getId(),
            DataSourceNavigatorSettings.NAVIGATOR_SETTINGS
        );
    }

    public static void objectSettingUpdated(
        @NotNull DBPProject project,
        @NotNull String objectId,
        @NotNull Collection<String> settingIds
    ) {
        DBPDataSourceContainer dataSourceContainer = project.getDataSourceRegistry().getDataSource(objectId);
        if (dataSourceContainer == null) {
            log.warn("Data source container '" + objectId + "' not found in registry");
            return;
        }
        if (settingIds.stream().noneMatch(DataSourceNavigatorSettings.NAVIGATOR_SETTINGS::contains)) {
            // No relevant settings changed
            return;
        }

        dataSourceContainer.getRegistry().refreshConfig(List.of(dataSourceContainer.getId()));
    }
}
