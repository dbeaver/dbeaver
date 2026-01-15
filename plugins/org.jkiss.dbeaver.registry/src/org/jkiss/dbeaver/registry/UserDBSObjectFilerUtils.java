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

import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectSettingsProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.security.SMObjectType;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.UserDBSObjectFilter;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UserDBSObjectFilerUtils {

    public static final String USER_FILTER_KEY = "navigator-filters-";

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
                Map.of(USER_FILTER_KEY + filterGroupName, filtersToArray(dataSourceDescriptor))
            );
        } catch (DBException e) {
            throw new RuntimeException(e);
        }
    }

    private static String filtersToArray(@NotNull DataSourceDescriptor dataSourceDescriptor) {
        ByteArrayOutputStream dsConfigBuffer = new ByteArrayOutputStream(10000);
        try (OutputStreamWriter osw = new OutputStreamWriter(dsConfigBuffer, StandardCharsets.UTF_8)) {
            try (JsonWriter jsonWriter = DataSourceSerializerModern.CONFIG_GSON.newJsonWriter(osw)) {
                jsonWriter.setIndent(JSONUtils.EMPTY_INDENT);
                DataSourceSerializerModern.saveObjectFilters(jsonWriter, null, dataSourceDescriptor, true);
                jsonWriter.flush();
                return dsConfigBuffer.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
