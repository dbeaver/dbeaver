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
package org.jkiss.dbeaver.registry;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FilterSerializer<T extends DataSourceDescriptor> {

    protected static final Gson CONFIG_GSON = DataSourceSerializerModern.CONFIG_GSON;

    @NotNull
    public List<FilterConfiguration> deserializeObjectFilterConfig(@NotNull String filterCfgJsonStringArray) {
        var filterConfigs = JSONUtils.parseList(CONFIG_GSON, new StringReader(filterCfgJsonStringArray));
        return filterConfigs.stream()
            .map(this::deserializeObjectFilterConfig)
            .toList();
    }

    @NotNull
    public FilterConfiguration deserializeObjectFilterConfig(@NotNull Map<String, Object> filterCfg) {
        String typeName = JSONUtils.getString(filterCfg, RegistryConstants.ATTR_TYPE);
        String objectID = JSONUtils.getString(filterCfg, RegistryConstants.ATTR_ID);
        DBSObjectFilter filter = deserializeObjectFilter(filterCfg);
        return new FilterConfiguration(typeName, objectID, filter);
    }

    @NotNull
    public DBSObjectFilter deserializeObjectFilter(@NotNull Map<String, Object> map) {
        DBSObjectFilter filter = new DBSObjectFilter();
        filter.setName(JSONUtils.getString(map, RegistryConstants.ATTR_NAME));
        filter.setDescription(JSONUtils.getString(map, RegistryConstants.ATTR_DESCRIPTION));
        filter.setEnabled(JSONUtils.getBoolean(map, RegistryConstants.ATTR_ENABLED));
        filter.setInclude(JSONUtils.deserializeStringList(map, RegistryConstants.TAG_INCLUDE));
        filter.setExclude(JSONUtils.deserializeStringList(map, RegistryConstants.TAG_EXCLUDE));
        return filter;
    }

    @NotNull
    public String serializeCustomUserFilters(@NotNull T dataSourceDescriptor) throws IOException {
        ByteArrayOutputStream dsConfigBuffer = new ByteArrayOutputStream(10000);
        try (OutputStreamWriter osw = new OutputStreamWriter(dsConfigBuffer, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = CONFIG_GSON.newJsonWriter(osw)) {
                jsonWriter.setFormattingStyle(FormattingStyle.COMPACT);
                jsonWriter.setIndent("");
                saveObjectFilters(jsonWriter, null, dataSourceDescriptor, true);
                jsonWriter.flush();
                return dsConfigBuffer.toString();
            }
        }
    }


    public void saveObjectFilters(
        @NotNull JsonWriter json,
        @Nullable String arrayName,
        @NotNull T dataSource,
        boolean serialiseCustomUserFilters
    ) throws IOException {
        Collection<FilterMapping> filterMappings = dataSource.getObjectFilters();
        if (!CommonUtils.isEmpty(filterMappings)) {
            if (arrayName != null) {
                json.name(arrayName);
            }
            json.beginArray();
            for (FilterMapping filter : filterMappings) {
                DBSObjectFilter defaultFilter = filter.defaultFilter;
                if (shouldSerializeFilter(defaultFilter, serialiseCustomUserFilters)) {
                    saveObjectFilter(json, filter.typeName, null, defaultFilter);
                }
                for (Map.Entry<String, DBSObjectFilter> cf : filter.customFilters.entrySet()) {
                    if (shouldSerializeFilter(cf.getValue(), serialiseCustomUserFilters)) {
                        saveObjectFilter(json, filter.typeName, cf.getKey(), cf.getValue());
                    }
                }
            }
            json.endArray();
        }
    }

    private boolean shouldSerializeFilter(@Nullable DBSObjectFilter filter, boolean useCustomUserFilters) {
        if (filter != null) {
            boolean emptySerializationCheck = useCustomUserFilters || !filter.isEmpty();
            return emptySerializationCheck
                && filter.isUserFilter() == useCustomUserFilters;
        } else {
            return false;
        }
    }

    public void saveObjectFilter(
        @NotNull JsonWriter json,
        @Nullable String typeName,
        @Nullable String objectID,
        @NotNull DBSObjectFilter filter
    ) throws IOException {
        json.beginObject();
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_ID, objectID);
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_TYPE, typeName);
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_NAME, filter.getName());
        JSONUtils.fieldNE(json, RegistryConstants.ATTR_DESCRIPTION, filter.getDescription());
        JSONUtils.field(json, RegistryConstants.ATTR_ENABLED, filter.isEnabled());
        JSONUtils.serializeStringList(json, RegistryConstants.TAG_INCLUDE, filter.getInclude());
        JSONUtils.serializeStringList(json, RegistryConstants.TAG_EXCLUDE, filter.getExclude());
        json.endObject();
    }

    public record FilterConfiguration(@Nullable String typeName, @Nullable String objectID, @NotNull DBSObjectFilter filter) {
        public boolean typeNamePresent() {
            return !CommonUtils.isEmpty(typeName);
        }
    }
}
