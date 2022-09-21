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

    public static final String ATTR_DIALECTS_ROOT = "dialects"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_PARENT = "parent"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_ID = "id"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_LABEL = "label"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_CLASS = "class"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_DESCRIPTION = "description"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_ICON = "icon"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_VALUE_KEYWORDS = "keywords"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_VALUE = "values";
    public static final String ATTR_DIALECTS_VALUE_EXECKEYWORDS = "execKeywords"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_VALUE_DDLKEYWORDS = "ddlKeywords"; //$NON-NLS-1$
    public static final String ATTR_DIALECTS_VALUE_DMLKEYWORDS = "dmlKeywords"; //$NON-NLS-1$
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
        JSONUtils.field(jsonWriter, ATTR_DIALECTS_ID, dialectDescriptor.getId());
        JSONUtils.fieldNE(jsonWriter, ATTR_DIALECTS_LABEL, dialectDescriptor.getLabel());
        JSONUtils.fieldNE(jsonWriter, ATTR_DIALECTS_DESCRIPTION, dialectDescriptor.getDescription());
        JSONUtils.fieldNE(jsonWriter, ATTR_DIALECTS_PARENT, dialectDescriptor.getParentDialect() != null ?
                                                          dialectDescriptor.getParentDialect().getId() : "");
        JSONUtils.fieldNE(jsonWriter, ATTR_DIALECTS_CLASS, dialectDescriptor.getImplClass().getImplName());
        JSONUtils.fieldNE(jsonWriter, ATTR_DIALECTS_ICON, dialectDescriptor.getIcon().getLocation());
        jsonWriter.name(ATTR_DIALECTS_VALUE);
        jsonWriter.beginObject();
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_KEYWORDS, dialectDescriptor.getReservedWords());
        JSONUtils.serializeStringList(jsonWriter,
            ATTR_DIALECTS_VALUE_EXECKEYWORDS,
            dialectDescriptor.getReservedWords()
        );
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_DDLKEYWORDS, dialectDescriptor.getDDLKeywords());
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_DMLKEYWORDS, dialectDescriptor.getDMLKeywords());
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_FUNCTIONS, dialectDescriptor.getFunctions());
        JSONUtils.serializeStringList(jsonWriter, ATTR_DIALECTS_VALUE_TYPES, dialectDescriptor.getDataTypes());
        jsonWriter.endObject();
        jsonWriter.endObject();
    }

    /**
     * Update SQLDialectRegistry from config file
     * @param config configuration file
     */
    public static void parseDialects(@NotNull StringReader config) {
        SQLDialectRegistry instance = SQLDialectRegistry.getInstance();
        Map<String, Object> stringObjectMap = JSONUtils.parseMap(CONFIG_GSON, config);
        for (Map<String, Object> dialect : JSONUtils.getObjectList(stringObjectMap, ATTR_DIALECTS_ROOT)) {
            SQLDialectDescriptor sqlDialectDescriptor =
                new SQLDialectDescriptor(JSONUtils.getString(dialect, ATTR_DIALECTS_ID));
            sqlDialectDescriptor.setDescription(JSONUtils.getString(dialect, ATTR_DIALECTS_DESCRIPTION));
            sqlDialectDescriptor.setLabel(JSONUtils.getString(dialect, ATTR_DIALECTS_LABEL));
            sqlDialectDescriptor.setImplClass(JSONUtils.getString(dialect, ATTR_DIALECTS_CLASS));

            sqlDialectDescriptor.setIcon(sqlDialectDescriptor.iconToImage(JSONUtils.getString(dialect, ATTR_DIALECTS_ICON)));
            Map<String, Object> descriptorValues = JSONUtils.getObject(dialect, ATTR_DIALECTS_VALUE);
            sqlDialectDescriptor.setExecKeywords(JSONUtils.getStringList(descriptorValues,
                ATTR_DIALECTS_VALUE_EXECKEYWORDS));
            sqlDialectDescriptor.setDdlKeywords(JSONUtils.getStringList(descriptorValues,
                ATTR_DIALECTS_VALUE_DDLKEYWORDS));
            sqlDialectDescriptor.setDmlKeywords(JSONUtils.getStringList(descriptorValues,
                ATTR_DIALECTS_VALUE_DMLKEYWORDS));
            sqlDialectDescriptor.setFunctions(JSONUtils.getStringList(descriptorValues,
                ATTR_DIALECTS_VALUE_FUNCTIONS));
            sqlDialectDescriptor.setTypes(JSONUtils.getStringList(descriptorValues,
                ATTR_DIALECTS_VALUE_TYPES));
            instance.updateDialect(sqlDialectDescriptor);
        }
    }

}
