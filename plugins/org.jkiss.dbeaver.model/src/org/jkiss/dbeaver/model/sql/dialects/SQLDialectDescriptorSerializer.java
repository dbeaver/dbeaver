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
package org.jkiss.dbeaver.model.sql.dialects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;


import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.*;

public class SQLDialectDescriptorSerializer {
    private static final Log log = Log.getLog(SQLDialectDescriptorSerializer.class);





    public static final String DIALECTS_FILE_NAME = "dialects.json";

    private static final Gson CONFIG_GSON = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    /**
     * Serializes the list of SQLDialectDescriptors to JSON format
     *
     * @param os Output stream to write into
     * @param dialects dialect list
     */
    public static void serializeDialectModifications(@NotNull OutputStream os, @NotNull Collection<SQLDialectDescriptor> dialects) {
        try (OutputStreamWriter osw = new OutputStreamWriter(os)) {
            try (JsonWriter jsonWriter = CONFIG_GSON.newJsonWriter(osw)) {
                jsonWriter.setIndent("\t");
                jsonWriter.beginObject();
                for (SQLDialectDescriptor dialect : dialects) {
                    if (dialect.getTransformer() != null) {
                        serializeDialectModification(jsonWriter, dialect.getTransformer());
                    }
                }
                jsonWriter.endObject();
            }
        } catch (IOException e) {
            log.error("IO error while saving dialects json", e);
        }
    }

    private static void serializeDialectModification(
        @NotNull JsonWriter jsonWriter,
        @NotNull SQLDialectDescriptorTransformer dialectDescriptor
    ) throws IOException {
        jsonWriter.name(dialectDescriptor.getId());
        JSONUtils.serializeMap(jsonWriter, dialectDescriptor.getModifications());
    }

    /**
     * Update SQLDialectRegistry from config file
     *
     * @param config configuration file
     */
    @SuppressWarnings("unchecked")
    public static void parseDialectModifications(@NotNull StringReader config) {

        for (Map.Entry<String, Object> dialectModification : JSONUtils.parseMap(CONFIG_GSON, config).entrySet()) {
            SQLDialectDescriptorTransformer sqlDialectDescriptorTransformer =
                new SQLDialectDescriptorTransformer(dialectModification.getKey());
            if (dialectModification.getValue() instanceof Map) {
                Map<String, Object> value = (Map<String, Object>) dialectModification.getValue();
                for (SQLDialectDescriptor.WordType wordType : SQLDialectDescriptor.WordType.values()) {
                    Object words = value.get(wordType.getTypeName());
                    if (words != null) {
                        sqlDialectDescriptorTransformer.putExcludedWords(wordType.getTypeName(),
                            ((Map<SQLDialectDescriptorTransformer.FilterType, Set<String>>) words).get(
                                SQLDialectDescriptorTransformer.FilterType.EXCLUDES));
                        sqlDialectDescriptorTransformer.putIncludedWords(wordType.getTypeName(),
                            ((Map<SQLDialectDescriptorTransformer.FilterType, Set<String>>) words).get(
                                SQLDialectDescriptorTransformer.FilterType.INCLUDES));
                    }
                }
            }
            SQLDialectRegistry instance = SQLDialectRegistry.getInstance();
            SQLDialectDescriptor dialect = instance.getDialect(dialectModification.getKey());
            dialect.setTransformer(sqlDialectDescriptorTransformer);
        }
    }

    @NotNull
    private static Set<String> convertToStringSet(@Nullable Object o) {
        if (o instanceof List) {
            return new HashSet<>(((List<String>) o));
        }
        if (o instanceof String) {
            return Set.of(((String) o));
        }
        if (o instanceof Set) {
            return (Set<String>) o;
        }
        return Collections.emptySet();
    }
}
