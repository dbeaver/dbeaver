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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class DataSourceNavigatorSettingsUtils {
    public static final String PARAM_ID_NAVIGATOR_SETTINGS = "navigator.settings";

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

    public static void saveSettingsToMap(
        @NotNull JsonWriter json,
        @NotNull DataSourceNavigatorSettings navSettings
    ) throws IOException {
        if (navSettings.isShowSystemObjects()) {
            JSONUtils.field(json, DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_SYSTEM_OBJECTS, true);
        }
        if (navSettings.isShowUtilityObjects()) {
            JSONUtils.field(json, DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_UTIL_OBJECTS, true);
        }
        if (navSettings.isShowOnlyEntities()) {
            JSONUtils.field(json, DataSourceSerializerModern.ATTR_NAVIGATOR_SHOW_ONLY_ENTITIES, true);
        }
        if (navSettings.isHideFolders()) {
            JSONUtils.field(json, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_FOLDERS, true);
        }
        if (navSettings.isHideSchemas()) {
            JSONUtils.field(json, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_SCHEMAS, true);
        }
        if (navSettings.isHideVirtualModel()) {
            JSONUtils.field(json, DataSourceSerializerModern.ATTR_NAVIGATOR_HIDE_VIRTUAL, true);
        }
        if (navSettings.isMergeEntities()) {
            JSONUtils.field(json, DataSourceSerializerModern.ATTR_NAVIGATOR_MERGE_ENTITIES, true);
        }
    }

    @NotNull
    public static String serializeSettingsToJson(@NotNull DataSourceNavigatorSettings navSettings) throws DBException {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        try {
            jsonWriter.beginObject();
            saveSettingsToMap(jsonWriter, navSettings);
            jsonWriter.endObject();
            jsonWriter.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new DBException("Error serializing navigator settings", e);
        }
    }
}
