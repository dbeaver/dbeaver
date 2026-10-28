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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectSettingsProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.auth.SMObjectType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class UserDBSObjectFilterUtils {

    private static final Log log = Log.getLog(UserDBSObjectFilterUtils.class);

    public static final String USER_FILTER_KEY = "navigator-filters";

    private static final FilterSerializer<DataSourceDescriptor> FILTER_SERIALIZER = new FilterSerializer<>();

    private UserDBSObjectFilterUtils() {
    }

    public static boolean hasUserFilters(@NotNull DBPDataSourceContainer dataSource) {
        return !readUserObjectFilters(dataSource).isEmpty();
    }

    @Nullable
    public static DBSObjectFilter getUserObjectFilter(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull Class<?> type,
        @Nullable DBSObject parentObject,
        boolean firstMatch
    ) {
        Map<String, FilterMapping> filters = readUserObjectFilters(dataSource);
        FilterMapping filterMapping = FilterMapping.resolveUser(filters, type, parentObject, firstMatch);
        return filterMapping == null ? null : filterMapping.getFilter(parentObject, firstMatch);
    }

    public static void setUserObjectFilter(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull Class<?> type,
        @Nullable DBSObject parentObject,
        @Nullable DBSObjectFilter filter
    ) throws DBException {
        DBPObjectSettingsProvider settingsProvider = getSettingsProvider(dataSource);
        if (settingsProvider == null) {
            return;
        }

        Map<String, FilterMapping> filters = readUserObjectFilters(dataSource);
        String objectId = toObjectId(parentObject);
        FilterMapping filterMapping = objectId == null ? null : FilterMapping.findCustomMapping(filters, type, objectId);
        if (filterMapping == null) {
            filterMapping = FilterMapping.findMapping(filters, type);
        }
        String typeName = filterMapping == null ? type.getName() : filterMapping.typeName;
        setUserObjectFilter(filters, typeName, objectId, filter);
        saveUserObjectFilters(dataSource, settingsProvider, filters.values());
    }

    public static void clearUserObjectFilters(@NotNull DBPDataSourceContainer dataSource) throws DBException {
        DBPObjectSettingsProvider settingsProvider = getSettingsProvider(dataSource);
        if (settingsProvider == null) {
            return;
        }
        Map<String, String> settings = settingsProvider.getObjectSettings(SMObjectType.datasource, dataSource.getId());
        Set<String> filterSettingIds = settings == null
            ? Set.of(USER_FILTER_KEY)
            : getFilterSettingIds(settings);
        if (!filterSettingIds.isEmpty()) {
            settingsProvider.clearObjectSettings(SMObjectType.datasource, dataSource.getId(), filterSettingIds);
        }
    }

    @NotNull
    private static Map<String, FilterMapping> readUserObjectFilters(@NotNull DBPDataSourceContainer dataSource) {
        DBPObjectSettingsProvider settingsProvider = getSettingsProvider(dataSource);
        if (settingsProvider == null) {
            return new LinkedHashMap<>();
        }
        Map<String, String> settings = settingsProvider.getObjectSettings(SMObjectType.datasource, dataSource.getId());
        if (settings == null) {
            return new LinkedHashMap<>();
        }

        Map<String, FilterMapping> filters = new LinkedHashMap<>();
        Collection<Map.Entry<String, String>> filterSettings = settings.containsKey(USER_FILTER_KEY)
            ? settings.entrySet().stream().filter(entry -> entry.getKey().equals(USER_FILTER_KEY)).toList()
            : settings.entrySet();
        filterSettings.stream()
            .filter(entry -> entry.getKey().startsWith(USER_FILTER_KEY))
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(Map.Entry::getValue)
            .forEach(filterConfig -> deserializeUserObjectFilters(filters, filterConfig));
        return filters;
    }

    private static void deserializeUserObjectFilters(
        @NotNull Map<String, FilterMapping> filters,
        @NotNull String filterConfig
    ) {
        FILTER_SERIALIZER.deserializeObjectFilterConfig(filterConfig).stream()
            .filter(FilterSerializer.FilterConfiguration::typeNamePresent)
            .forEach(configuration -> setUserObjectFilter(
                filters,
                configuration.typeName(),
                configuration.objectID(),
                configuration.filter()
            ));
    }

    private static void setUserObjectFilter(
        @NotNull Map<String, FilterMapping> filters,
        @NotNull String typeName,
        @Nullable String objectId,
        @Nullable DBSObjectFilter filter
    ) {
        FilterMapping mapping = filters.get(typeName);
        if (mapping == null && filter != null) {
            mapping = new FilterMapping(typeName);
            filters.put(typeName, mapping);
        }
        if (mapping == null) {
            return;
        }
        if (filter != null) {
            filter.setUserFilter(true);
        }
        if (objectId == null) {
            mapping.globalFilter = filter;
        } else if (filter == null) {
            mapping.customFilters.remove(objectId);
        } else {
            mapping.customFilters.put(objectId, filter);
        }
        if (mapping.globalFilter == null && mapping.customFilters.isEmpty()) {
            filters.remove(typeName);
        }
    }

    private static void saveUserObjectFilters(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPObjectSettingsProvider settingsProvider,
        @NotNull Collection<FilterMapping> filters
    ) throws DBException {
        if (filters.isEmpty()) {
            clearUserObjectFilters(dataSource);
            return;
        }
        try {
            settingsProvider.setObjectSettings(
                SMObjectType.datasource,
                dataSource.getId(),
                Map.of(USER_FILTER_KEY, FILTER_SERIALIZER.serializeObjectFilters(filters))
            );
            Map<String, String> settings = settingsProvider.getObjectSettings(SMObjectType.datasource, dataSource.getId());
            Set<String> legacySettingIds = getFilterSettingIds(settings).stream()
                .filter(key -> !key.equals(USER_FILTER_KEY))
                .collect(Collectors.toSet());
            if (!legacySettingIds.isEmpty()) {
                settingsProvider.clearObjectSettings(SMObjectType.datasource, dataSource.getId(), legacySettingIds);
            }
        } catch (IOException e) {
            log.warn("Error while serializing filter object settings", e);
        }
    }

    @NotNull
    private static Set<String> getFilterSettingIds(@Nullable Map<String, String> settings) {
        if (settings == null) {
            return Set.of();
        }
        return settings.keySet().stream()
            .filter(key -> key.startsWith(USER_FILTER_KEY))
            .collect(Collectors.toSet());
    }

    @Nullable
    private static DBPObjectSettingsProvider getSettingsProvider(@NotNull DBPDataSourceContainer dataSource) {
        return DBUtils.getAdapter(DBPObjectSettingsProvider.class, dataSource.getProject());
    }

    @Nullable
    private static String toObjectId(@Nullable DBSObject parentObject) {
        return parentObject == null ? null : FilterMapping.getFilterContainerUniqueID(parentObject);
    }
}
