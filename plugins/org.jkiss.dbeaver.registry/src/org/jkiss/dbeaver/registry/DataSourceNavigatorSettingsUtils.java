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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectSettingsProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.security.SMObjectType;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class DataSourceNavigatorSettingsUtils {
    public static final String PARAM_ID_NAVIGATOR_SETTINGS = "navigator-settings.";

    private static final Log log = Log.getLog(DataSourceNavigatorSettingsUtils.class);

    public static void loadSettingsFromMap(@NotNull DataSourceNavigatorSettings navSettings, @NotNull Map<String, Object> objectMap) {
        navSettings.setShowSystemObjects(JSONUtils.getBoolean(objectMap, DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_SYSTEM_OBJECTS));
        navSettings.setShowUtilityObjects(JSONUtils.getBoolean(objectMap, DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_UTIL_OBJECTS));
        navSettings.setShowOnlyEntities(JSONUtils.getBoolean(objectMap, DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_ONLY_ENTITIES));
        navSettings.setHideFolders(JSONUtils.getBoolean(objectMap, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_FOLDERS));
        navSettings.setHideSchemas(JSONUtils.getBoolean(objectMap, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_SCHEMAS));
        navSettings.setHideVirtualModel(JSONUtils.getBoolean(objectMap, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_VIRTUAL));
        navSettings.setMergeEntities(JSONUtils.getBoolean(objectMap, DataSourceSerializerModern.ATTR_NAVIGATOR_MERGE_ENTITIES));
    }

    @Nullable
    public static DataSourceNavigatorSettings getUserNavigatorSettings(@NotNull DBPDataSourceContainer dataSource) {
        DBPObjectSettingsProvider settingsProvider = DBUtils.getAdapter(DBPObjectSettingsProvider.class, dataSource.getProject());
        if (settingsProvider == null) {
            return null;
        }
        Map<String, String> settings = settingsProvider.getObjectSettings(SMObjectType.datasource, dataSource.getId());
        if (settings == null || settings.isEmpty()) {
            return null;
        }
        Map<String, Object> navigatorSettingsMap = settings.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(PARAM_ID_NAVIGATOR_SETTINGS))
            .map((entry) -> Map.entry(entry.getKey().substring((PARAM_ID_NAVIGATOR_SETTINGS).length()), entry.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
        if (navigatorSettingsMap.isEmpty()) {
            return null;
        }

        DataSourceNavigatorSettings navigatorSettings = new DataSourceNavigatorSettings();
        loadSettingsFromMap(navigatorSettings, navigatorSettingsMap);
        return navigatorSettings;
    }

    public static void updateCustomNavigatorSettings(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DataSourceNavigatorSettings settings
    ) throws DBException {
        DBPObjectSettingsProvider settingsProvider = DBUtils.getAdapter(DBPObjectSettingsProvider.class, dataSource.getProject());
        if (settingsProvider == null || !(dataSource instanceof DataSourceDescriptor dsd)) {
            return;
        }
        Map<String, String> settingsMap = DataSourceNavigatorSettings.saveSettingsToMap(settings)
            .entrySet().stream()
            .map((entry) -> Map.entry(PARAM_ID_NAVIGATOR_SETTINGS + entry.getKey(), CommonUtils.toString(entry.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        settingsProvider.setObjectSettings(
            SMObjectType.datasource,
            dataSource.getId(),
            settingsMap
        );
        dsd.getNavigatorSettings().setUserSettings(settings);
    }


    public static void objectSettingUpdated(@NotNull DBPProject project, @NotNull String objectId, @NotNull Collection<String> settingIds) {
        DBPDataSourceContainer dataSourceContainer = project.getDataSourceRegistry().getDataSource(objectId);
        if (dataSourceContainer == null) {
            log.warn("Data source container '" + objectId + "' not found in registry");
            return;
        }
        if (settingIds.stream().noneMatch(s -> s.startsWith(PARAM_ID_NAVIGATOR_SETTINGS))) {
            // No relevant settings changed
            return;
        }
        DataSourceNavigatorSettings navigatorSettings = getUserNavigatorSettings(dataSourceContainer);
        ((DataSourceNavigatorSettings) dataSourceContainer.getNavigatorSettings()).setUserSettings(navigatorSettings);

        // Refresh data source
        DBNNode node = DBNUtils.getNodeByObject(dataSourceContainer);
        if (node != null) {
            try {
                node.refreshNode(new VoidProgressMonitor(), DataSourceNavigatorSettingsUtils.class);
            } catch (DBException e) {
                log.warn("Error refreshing data source settings", e);
            }
        }

    }
}
