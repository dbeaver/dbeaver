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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectSettingsProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.security.SMObjectType;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UserDBSObjectFilterUtils {

    private static final Log log = Log.getLog(UserDBSObjectFilterUtils.class);

    public static final String USER_FILTER_KEY = "navigator-filters";

    private static final FilterSerializer<DataSourceDescriptor> filterSerializer = new FilterSerializer<>();

    public static boolean hasUserFilters(@NotNull DBPDataSourceContainer dataSource) {
        DBPObjectSettingsProvider settingsProvider = DBUtils.getAdapter(DBPObjectSettingsProvider.class, dataSource.getProject());
        if (settingsProvider == null) {
            return false;
        }
        Map<String, String> settings = settingsProvider.getObjectSettings(SMObjectType.datasource, dataSource.getId());
        return settings != null && settings.containsKey(USER_FILTER_KEY);
    }

    public static void clearUserObjectFilters(@NotNull DBPDataSourceContainer dataSource) throws DBException {
        DBPObjectSettingsProvider settingsProvider = DBUtils.getAdapter(DBPObjectSettingsProvider.class, dataSource.getProject());
        if (settingsProvider == null) {
            return;
        }
        settingsProvider.clearObjectSettings(
            SMObjectType.datasource,
            dataSource.getId(),
            Set.of(USER_FILTER_KEY)
        );
    }

    public static void updateUserObjectFilters(@NotNull DBPDataSourceContainer dataSource) throws DBException {
        DBPObjectSettingsProvider settingsProvider = DBUtils.getAdapter(DBPObjectSettingsProvider.class, dataSource.getProject());
        if (settingsProvider == null || !(dataSource instanceof DataSourceDescriptor dataSourceDescriptor)) {
            return;
        }
        try {
            settingsProvider.setObjectSettings(
                SMObjectType.datasource,
                dataSource.getId(),
                Map.of(USER_FILTER_KEY, filterSerializer.serializeCustomUserFilters(dataSourceDescriptor))
            );
        } catch (IOException logged) {
            log.warn("Error while serializing filter object settings", logged);
        }

    }

    public static void setUserObjectFilters(@NotNull DataSourceDescriptor dataSourceDescriptor, @NotNull Map<String, String> userSettings) {
        userSettings
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(USER_FILTER_KEY))
            .map(Map.Entry::getValue)
            .forEach(filterCfgString -> setUserObjectFilter(dataSourceDescriptor, filterCfgString));
    }

    public static void objectSettingUpdated(
        @NotNull DBPProject project,
        @NotNull String objectId,
        @NotNull Collection<String> settingIds
    ) {
        DBPDataSourceContainer dataSourceContainer = project.getDataSourceRegistry().getDataSource(objectId);
        if (dataSourceContainer == null) {
            log.warn("Data source container '" + objectId + "' not found in registry");
            return;
        }
        if (settingIds.stream().noneMatch(UserDBSObjectFilterUtils.USER_FILTER_KEY::contains)) {
            // No relevant settings changed
            return;
        }

        dataSourceContainer.getRegistry().refreshConfig(List.of(dataSourceContainer.getId()));
    }

    private static void setUserObjectFilter(@NotNull DataSourceDescriptor dataSourceDescriptor, @NotNull String filterConfigJson) {
        filterSerializer.deserializeObjectFilterConfig(filterConfigJson)
            .stream()
            .filter(FilterSerializer.FilterConfiguration::typeNamePresent)
            .peek(f -> f.filter().setUserFilter(true))
            .forEach(fc -> dataSourceDescriptor.updateObjectFilter(
                fc.typeName(),
                fc.objectID(),
                fc.filter()
            ));
    }
}
