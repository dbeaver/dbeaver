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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectSettingsProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.security.SMObjectType;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.UserDBSObjectFilter;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.Map;

public class UserDBSObjectFilerUtils {

    public static final String USER_FILTER_KEY = "navigator-filters-";

    protected static final FilterSerializer<DataSourceDescriptor> filterSerializer = new FilterSerializer<>() {
        @NotNull
        @Override
        public DBSObjectFilter deserializeObjectFiler(@NotNull Map<String, Object> map) {
            return new UserDBSObjectFilter(super.deserializeObjectFiler(map));
        }
    };

    public static void updateUserObjectFilters(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull String filterGroupName
    ) {
        DBPObjectSettingsProvider settingsProvider = DBUtils.getAdapter(DBPObjectSettingsProvider.class, dataSource.getProject());
        if (settingsProvider == null || !(dataSource instanceof DataSourceDescriptor dataSourceDescriptor)) {
            return;
        }
        try {
            settingsProvider.setObjectSettings(
                SMObjectType.datasource,
                dataSource.getId(),
                Map.of(USER_FILTER_KEY + filterGroupName, filterSerializer.serializeFilters(dataSourceDescriptor))
            );
        } catch (DBException e) {
            throw new RuntimeException(e);
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

    private static void setUserObjectFilter(@NotNull DataSourceDescriptor dataSourceDescriptor, @NotNull String filterConfigJson) {
        filterSerializer.deserializeObjectFilterConfig(filterConfigJson)
            .stream()
            .filter(FilterSerializer.FilterConfiguration::typeNamePresent)
            .forEach(fc -> dataSourceDescriptor.updateObjectFilter(
                fc.typeName(),
                fc.objectID(),
                fc.filter()
            ));
    }


    public static boolean notCustomUserFilter(@Nullable DBSObjectFilter filter) {
        return !isCustomUserFilter(filter);
    }

    public static boolean isCustomUserFilter(@Nullable DBSObjectFilter filter) {
        return DBWorkbench.isDistributed()
            && filter instanceof UserDBSObjectFilter userDBSObjectFilter
            && userDBSObjectFilter.isCustomUserFilter();
    }
}
