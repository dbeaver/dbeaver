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
package org.jkiss.dbeaver.model.impl.dialects;

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
import java.util.stream.Collectors;

public class SQLDialectDescriptorSerializer {
    private static final Log log = Log.getLog(SQLDialectDescriptorSerializer.class);


    public static final String ATTR_DIALECTS_VALUE_KEYWORDS = "keywords"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_VALUE_KEYWORDS_EXEC = "execKeywords"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_VALUE_KEYWORDS_DDL = "ddlKeywords"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_TXN_KEYWORDS = "txnKeywords"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_VALUE_KEYWORDS_DML = "dmlKeywords"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_VALUE_SCRIPT_SEPARATOR = "scriptSeparator";
    public static final String ATTR_DIALECTS_VALUE_FUNCTIONS = "functions"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_VALUE_TYPES = "types"; //$NON-NLS-1$


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
    public static void serializeDialects(@NotNull OutputStream os, @NotNull Collection<SQLDialectDescriptor> dialects) {
        try (OutputStreamWriter osw = new OutputStreamWriter(os)) {
            try (JsonWriter jsonWriter = CONFIG_GSON.newJsonWriter(osw)) {
                jsonWriter.setIndent("\t");
                jsonWriter.beginObject();
                for (SQLDialectDescriptor dialect : dialects) {
                    serializeDialect(jsonWriter, dialect);
                }
                jsonWriter.endObject();
            }
        } catch (IOException e) {
            log.error("IO error while saving dialects json", e);
        }
    }

    private static void serializeDialect(
        @NotNull JsonWriter jsonWriter,
        @NotNull SQLDialectDescriptor dialectDescriptor
    ) throws IOException {
        jsonWriter.name(dialectDescriptor.getId());
        jsonWriter.beginObject();
        JSONUtils.field(jsonWriter, ATTR_DIALECTS_VALUE_SCRIPT_SEPARATOR, dialectDescriptor.getScriptDelimiter());
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_KEYWORDS,
            dialectDescriptor.getReservedWords(false));
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_KEYWORDS_EXEC,
            dialectDescriptor.getReservedWords(false)
        );
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_KEYWORDS_DDL,
            dialectDescriptor.getDDLKeywords(false));
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_KEYWORDS_DML,
            dialectDescriptor.getDMLKeywords(false));
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_FUNCTIONS, dialectDescriptor.getFunctions(false));
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_TYPES, dialectDescriptor.getDataTypes(false));
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_TXN_KEYWORDS,
            dialectDescriptor.getTransactionKeywords(false));
        jsonWriter.endObject();
    }

    /**
     * Update SQLDialectRegistry from config file
     *
     * @param config configuration file
     */
    public static void parseDialects(@NotNull StringReader config) {
        SQLDialectRegistry instance = SQLDialectRegistry.getInstance();
        for (Map.Entry<String, Object> dialect : JSONUtils.parseMap(CONFIG_GSON, config).entrySet()) {
            SQLDialectDescriptor sqlDialectDescriptor =
                new SQLDialectDescriptor(dialect.getKey());
            sqlDialectDescriptor.setKeywords(convertToStringSet(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_KEYWORDS
            )));
            sqlDialectDescriptor.setExecKeywords(convertToStringSet(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_KEYWORDS_EXEC
            )));
            sqlDialectDescriptor.setDdlKeywords(convertToStringSet(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_KEYWORDS_DDL
            )));
            sqlDialectDescriptor.setDmlKeywords(convertToStringSet(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_KEYWORDS_DML
            )));
            sqlDialectDescriptor.setFunctions(convertToStringSet(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_FUNCTIONS)));
            sqlDialectDescriptor.setTypes(convertToStringSet(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_TYPES)));
            sqlDialectDescriptor.setTxnKeywords(convertToStringSet(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_TXN_KEYWORDS)));
            sqlDialectDescriptor.setScriptDelimiter(JSONUtils.getObjectProperty(dialect.getValue(), ATTR_DIALECTS_VALUE_SCRIPT_SEPARATOR));
            instance.applyDialectCustomisation(sqlDialectDescriptor);
            instance.getCustomDialects().put(sqlDialectDescriptor.getId(),
                sqlDialectDescriptor);
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
