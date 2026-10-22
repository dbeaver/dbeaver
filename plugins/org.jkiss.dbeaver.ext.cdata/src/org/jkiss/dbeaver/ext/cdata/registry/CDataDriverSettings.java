/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cdata.registry;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

record CDataDriverSettings(
    @SerializedName("datatype_id") @NotNull String dataTypeId,
    @SerializedName("icon_driver") @Nullable String iconDriver
) {
    static final String FILE_NAME = "cdata-driver-settings.json";
    private static final Log log = Log.getLog(CDataDriverSettings.class);

    @NotNull
    static Map<String, CDataDriverSettings> load(@NotNull Path file) {
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            return read(reader);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            log.warn("Error reading CData driver settings", e);
            return Map.of();
        }
    }

    @NotNull
    private static Map<String, CDataDriverSettings> read(@NotNull Reader reader) {
        try {
            Map<String, CDataDriverSettings> settings = JSONUtils.GSON.fromJson(
                reader, new TypeToken<Map<String, CDataDriverSettings>>() { }.getType());
            if (settings == null) {
                throw new IllegalStateException("CData driver settings are empty");
            }
            for (var entry : settings.entrySet()) {
                var value = entry.getValue();
                if (entry.getKey().isBlank() || value == null || value.dataTypeId() == null || value.dataTypeId().isBlank()
                    || value.iconDriver() != null && !value.iconDriver().matches("[^:]+:[^:]+")) {
                    throw new IllegalStateException("Invalid CData driver settings: " + entry.getKey());
                }
            }
            return Map.copyOf(settings);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Error parsing CData driver settings", e);
        }
    }
}
