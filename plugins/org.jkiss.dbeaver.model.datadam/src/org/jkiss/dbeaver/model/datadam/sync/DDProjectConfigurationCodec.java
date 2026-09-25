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
package org.jkiss.dbeaver.model.datadam.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DDProjectConfigurationCodec {
    private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    private DDProjectConfigurationCodec() {
    }

    @NotNull
    public static Map<String, byte[]> toPortable(@NotNull DBPProject project, @NotNull Map<String, byte[]> files) throws DBException {
        Map<String, byte[]> portable = new LinkedHashMap<>();
        for (var file : files.entrySet()) {
            byte[] contents = encryptedLocally(project, file.getKey())
                ? project.getValueEncryptor().decryptValue(file.getValue()) : file.getValue();
            portable.put(file.getKey(), normalize(file.getKey(), contents));
        }
        return portable;
    }

    @NotNull
    public static Map<String, byte[]> toLocal(@NotNull DBPProject project, @NotNull Map<String, byte[]> files) throws DBException {
        Map<String, byte[]> local = new LinkedHashMap<>();
        for (var file : files.entrySet()) {
            byte[] contents = normalize(file.getKey(), file.getValue());
            local.put(file.getKey(), encryptedLocally(project, file.getKey())
                ? project.getValueEncryptor().encryptValue(contents) : contents);
        }
        return local;
    }

    @NotNull
    private static byte[] normalize(@NotNull String name, @NotNull byte[] contents) throws DBException {
        try {
            String json = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(contents)).toString();
            JsonObject object = GSON.fromJson(json, JsonObject.class);
            if (object != null) {
                return GSON.toJson(object).getBytes(StandardCharsets.UTF_8);
            }
        } catch (CharacterCodingException | RuntimeException e) {
            // parser exceptions can contain credentials; expose only the file name
            throw new DBException("Invalid project configuration file: " + name);
        }
        throw new DBException("Project configuration must be a JSON object: " + name);
    }

    private static boolean encryptedLocally(@NotNull DBPProject project, @NotNull String name) {
        return DBPDataSourceRegistry.CREDENTIALS_CONFIG_FILE_NAME.equals(name)
            || (DBPDataSourceRegistry.MODERN_CONFIG_FILE_NAME.equals(name) && project.isEncryptedProject());
    }
}
