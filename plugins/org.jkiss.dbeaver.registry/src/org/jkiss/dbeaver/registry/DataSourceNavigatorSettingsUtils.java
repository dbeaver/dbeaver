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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectSettingsProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DataSourceNavigatorSettingsUtils {
    public static final String PARAM_ID_NAVIGATOR_SETTINGS = "navigator-settings.";

    public static final Gson GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .serializeNulls()
        .create();


    public static void loadSettingsFromMap(@NotNull DataSourceNavigatorSettings navSettings, @NotNull Map<String, Object> objectMap) {
        navSettings.setShowSystemObjects(JSONUtils.getBoolean(
            objectMap,
            DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_SYSTEM_OBJECTS
        ));
        navSettings.setShowUtilityObjects(JSONUtils.getBoolean(
            objectMap,
            DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_UTIL_OBJECTS
        ));
        navSettings.setShowOnlyEntities(JSONUtils.getBoolean(
            objectMap,
            DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_ONLY_ENTITIES
        ));
        navSettings.setHideFolders(JSONUtils.getBoolean(objectMap, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_FOLDERS));
        navSettings.setHideSchemas(JSONUtils.getBoolean(objectMap, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_SCHEMAS));
        navSettings.setHideVirtualModel(JSONUtils.getBoolean(objectMap, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_VIRTUAL));
        navSettings.setMergeEntities(JSONUtils.getBoolean(objectMap, DataSourceSerializerModern.ATTR_NAVIGATOR_MERGE_ENTITIES));
    }

    private static boolean getBoolean(@NotNull String key, @NotNull Map<String, Object> map1, @NotNull Map<String, Object> map2) {
        if (map1.containsKey(key)) {
            return JSONUtils.getBoolean(map1, key);
        }
        return JSONUtils.getBoolean(map2, key);
    }

    private static Map<String, Object> getOriginalSettingsMap(@NotNull Map<String, Object> objectMap) {
        Set<String> originalKeys = Set.of(
            DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_SYSTEM_OBJECTS,
            DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_UTIL_OBJECTS,
            DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_ONLY_ENTITIES,
            DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_FOLDERS,
            DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_SCHEMAS,
            DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_VIRTUAL,
            DataSourceSerializerModern.ATTR_NAVIGATOR_MERGE_ENTITIES
        );
        return objectMap.entrySet().stream()
            .filter(entry -> originalKeys.contains(entry.getKey()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }

    @Nullable
    public static DataSourceNavigatorSettings getUserNavigatorSettings(@NotNull DBPDataSourceContainer dataSource) {
        DBPObjectSettingsProvider settingsProvider = DBUtils.getAdapter(DBPObjectSettingsProvider.class, dataSource.getProject());
        if (settingsProvider == null) {
            return null;
        }
        Map<String, String> settings = settingsProvider.getObjectSettings(dataSource.getId());
        if (settings == null) {
            return null;
        }
        Map<String, Object> navigatorSettingsMap = settings.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(PARAM_ID_NAVIGATOR_SETTINGS))
            .map((entry) -> Map.entry(entry.getKey().substring((PARAM_ID_NAVIGATOR_SETTINGS).length()), entry.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));

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
        Map<String, String> settingsMap = toMap(settings).entrySet().stream()
            .map((entry) -> Map.entry(PARAM_ID_NAVIGATOR_SETTINGS + entry.getKey(), CommonUtils.toString(entry.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        settingsProvider.setObjectSettings(
            dataSource.getId(),
            settingsMap
        );
        dsd.getNavigatorSettings().setUserSettings(settings);
    }

    @NotNull
    private static Map<String, Object> toMap(@NotNull DataSourceNavigatorSettings navSettings) throws DBException {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        try {
            jsonWriter.beginObject();
            DataSourceNavigatorSettings.saveSettingsToMap(jsonWriter, navSettings, false);
            jsonWriter.endObject();
            jsonWriter.flush();
            return GSON.fromJson(writer.toString(), JSONUtils.MAP_TYPE_TOKEN);
        } catch (IOException e) {
            throw new DBException("Error serializing navigator settings", e);
        }
    }


}
