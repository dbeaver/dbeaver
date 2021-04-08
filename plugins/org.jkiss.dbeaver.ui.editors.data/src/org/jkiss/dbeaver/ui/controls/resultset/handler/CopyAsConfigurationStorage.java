/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsActivator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

class CopyAsConfigurationStorage {
    private static final String COPY_AS_CFG = "CopyAsConfig";

    private static final Gson GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    @NotNull
    static Map<DataTransferProcessorDescriptor, Map<String, Object>> getProcessorProperties() {
        Map<DataTransferProcessorDescriptor, Map<String, Object>> propertiesMap = new HashMap<>();
        String json = getPreferenceStore().getString(COPY_AS_CFG);
        if (json == null || json.isEmpty()) {
            return propertiesMap;
        }
        Map<String, Object> jsonMap = JSONUtils.parseMap(GSON, new StringReader(json));
        for (Map.Entry<String, Map<String, Object>> entry : JSONUtils.getNestedObjects(jsonMap, COPY_AS_CFG)) {
            DataTransferProcessorDescriptor descriptor = DataTransferRegistry.getInstance().getProcessor(entry.getKey());
            if (descriptor != null) {
                propertiesMap.put(descriptor, entry.getValue());
            }
        }
        return propertiesMap;
    }

    static void saveProcessorProperties(@NotNull Map<DataTransferProcessorDescriptor, Map<String, Object>> properties) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = GSON.newJsonWriter(writer)) {
                jsonWriter.setIndent("    ");
                jsonWriter.beginObject();
                jsonWriter.name(COPY_AS_CFG);
                jsonWriter.beginObject();
                for (Map.Entry<DataTransferProcessorDescriptor, Map<String, Object>> entries: properties.entrySet()) {
                    JSONUtils.serializeProperties(jsonWriter, entries.getKey().getFullId(), entries.getValue());
                }
                jsonWriter.endObject();
                jsonWriter.endObject();
                jsonWriter.flush();
            }
        }
        String json = new String(os.toByteArray(), StandardCharsets.UTF_8);
        getPreferenceStore().setValue(COPY_AS_CFG, json);
    }

    private static DBPPreferenceStore getPreferenceStore() {
        return DataEditorsActivator.getDefault().getPreferences();
    }

    private CopyAsConfigurationStorage() {
        //intentionally left blank
    }
}
