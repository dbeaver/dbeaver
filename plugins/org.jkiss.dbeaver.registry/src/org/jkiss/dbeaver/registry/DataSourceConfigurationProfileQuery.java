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
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Queries saved datasource configurations without constructing a datasource registry.
 */
public final class DataSourceConfigurationProfileQuery {
    private static final Log log = Log.getLog(DataSourceConfigurationProfileQuery.class);

    private DataSourceConfigurationProfileQuery() {
    }

    public static boolean hasLocalProfile(@NotNull DBPProject project, @NotNull String profileName) throws DBException {
        DataSourceConfigurationManager manager = new DataSourceConfigurationManagerNIO(project);
        for (DBPDataSourceConfigurationStorage storage : getModernStorages(project, manager)) {
            if (storage.isDefault()) {
                return hasLocalProfile(read(project, manager, storage), profileName);
            }
        }
        return false;
    }

    private static boolean hasLocalProfile(@NotNull Map<String, Object> config, @NotNull String profileName) {
        for (var profile : JSONUtils.getNestedObjects(config, "network-profiles")) {
            if (profileName.equals(profile.getKey())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static Map<String, String> findGlobalProfileConnections(
        @NotNull DBPProject project, @NotNull String profileName
    ) throws DBException {
        DataSourceConfigurationManager manager = new DataSourceConfigurationManagerNIO(project);
        List<DBPDataSourceConfigurationStorage> storages = getModernStorages(project, manager);
        Map<String, String> connections = new LinkedHashMap<>();
        for (DBPDataSourceConfigurationStorage storage : storages) {
            if (storage.isDefault()) {
                Map<String, Object> config = read(project, manager, storage);
                if (hasLocalProfile(config, profileName)) {
                    return Map.of();
                }
                addConnections(config, profileName, connections);
                break;
            }
        }
        for (DBPDataSourceConfigurationStorage storage : storages) {
            if (!storage.isDefault()) {
                addConnections(read(project, manager, storage), profileName, connections);
            }
        }
        return connections;
    }

    @NotNull
    private static List<DBPDataSourceConfigurationStorage> getModernStorages(
        @NotNull DBPProject project,
        @NotNull DataSourceConfigurationManager manager
    ) {
        return manager.getConfigurationStorages().stream()
            .filter(storage -> {
                if (storage.getStorageName().endsWith(DBPDataSourceRegistry.LEGACY_CONFIG_FILE_EXT)) {
                    log.warn("Skipping legacy datasource configuration '" + storage.getStorageName() +
                        "' in project '" + project.getName() + "'");
                    return false;
                }
                return true;
            })
            .toList();
    }

    private static void addConnections(
        @NotNull Map<String, Object> config,
        @NotNull String profileName,
        @NotNull Map<String, String> connections
    ) {
        for (var connection : JSONUtils.getNestedObjects(config, "connections")) {
            Map<String, Object> configuration = JSONUtils.getObject(connection.getValue(), "configuration");
            if (profileName.equals(JSONUtils.getString(configuration, "config-profile")) &&
                CommonUtils.isEmpty(JSONUtils.getString(configuration, "config-profile-source"))) {
                connections.put(connection.getKey(),
                    JSONUtils.getString(connection.getValue(), "name", connection.getKey()));
            }
        }
    }

    @NotNull
    private static Map<String, Object> read(
        @NotNull DBPProject project,
        @NotNull DataSourceConfigurationManager manager,
        @NotNull DBPDataSourceConfigurationStorage storage
    ) throws DBException {
        try {
            Map<String, Object> config = DataSourceSerializerModern.readConfiguration(project, storage, manager, null);
            return config == null ? Map.of() : config;
        } catch (IOException | RuntimeException e) {
            throw new DBException("Cannot read network profile usage for project " + project.getName(), e);
        }
    }
}
