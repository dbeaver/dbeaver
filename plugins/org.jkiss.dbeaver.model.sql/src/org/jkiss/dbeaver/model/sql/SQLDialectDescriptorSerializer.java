/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;

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


    public static final String DIALECTS_FILE_NAME = "dialect.json";

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

    private static void serializeDialect(@NotNull JsonWriter jsonWriter,
        @NotNull SQLDialectDescriptor dialectDescriptor) throws
            IOException {
        jsonWriter.name(dialectDescriptor.getId());
        jsonWriter.beginObject();
//        JSONUtils.fieldNE(jsonWriter, ATTR_DIALECTS_DESCRIPTION, dialectDescriptor.getDescription());
//        JSONUtils.fieldNE(jsonWriter, ATTR_DIALECTS_PARENT, dialectDescriptor.getParentDialect() != null ?
//                                                          dialectDescriptor.getParentDialect().getId() : "");
//        JSONUtils.fieldNE(jsonWriter, ATTR_DIALECTS_CLASS, dialectDescriptor.getImplClass().getImplName());
//        JSONUtils.fieldNE(jsonWriter, ATTR_DIALECTS_ICON, dialectDescriptor.getIcon().getLocation());

        JSONUtils.field(jsonWriter, ATTR_DIALECTS_VALUE_SCRIPT_SEPARATOR, dialectDescriptor.getScriptDelimiter());
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_KEYWORDS, dialectDescriptor.getReservedWords());
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_KEYWORDS_EXEC,
            dialectDescriptor.getReservedWords()
        );
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_KEYWORDS_DDL, dialectDescriptor.getDDLKeywords());
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_KEYWORDS_DML, dialectDescriptor.getDMLKeywords());
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_FUNCTIONS, dialectDescriptor.getFunctions());
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_TYPES, dialectDescriptor.getDataTypes());
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_TXN_KEYWORDS, dialectDescriptor.getTransactionKeywords());
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
            sqlDialectDescriptor.setKeywords(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_KEYWORDS
            ));
            sqlDialectDescriptor.setExecKeywords(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_KEYWORDS_EXEC
            ));
            sqlDialectDescriptor.setDdlKeywords(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_KEYWORDS_DDL
            ));
            sqlDialectDescriptor.setDmlKeywords(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_KEYWORDS_DML
            ));
            sqlDialectDescriptor.setFunctions(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_FUNCTIONS));
            sqlDialectDescriptor.setTypes(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_VALUE_TYPES));
            sqlDialectDescriptor.setTxnKeywords(JSONUtils.getObjectProperty(dialect.getValue(),
                ATTR_DIALECTS_TXN_KEYWORDS));
            sqlDialectDescriptor.setScriptDelimiter(JSONUtils.getObjectProperty(dialect.getValue(), ATTR_DIALECTS_VALUE_SCRIPT_SEPARATOR));
            instance.applyDialectCustomisation(sqlDialectDescriptor);
            instance.getCustomDialects().put(sqlDialectDescriptor.getId(),
                sqlDialectDescriptor);
        }
    }

}
