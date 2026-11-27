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
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileUsageProvider;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Queries loaded project registries when the saved-configuration usage query is unavailable.
 */
public final class LegacyNetworkProfileUsageProvider implements DBWNetworkProfileUsageProvider {
    private final DBPWorkspace workspace;

    public LegacyNetworkProfileUsageProvider(@NotNull DBPWorkspace workspace) {
        this.workspace = workspace;
    }

    @NotNull
    @Override
    public Map<String, String> findLocalProfileConflicts(@NotNull String profileName) throws DBException {
        Map<String, String> result = new LinkedHashMap<>();
        for (var project : workspace.getProjects()) {
            var registry = project.getDataSourceRegistry();
            registry.checkForErrors();
            if (registry.getNetworkProfiles().getProfiles().stream()
                .anyMatch(profile -> profileName.equals(profile.getProfileName()))) {
                result.put(project.getId(), project.getName());
            }
        }
        return result;
    }

    @NotNull
    @Override
    public List<ProjectConnections> findGlobalProfileConnections(@NotNull String profileName) throws DBException {
        List<ProjectConnections> result = new ArrayList<>();
        for (var project : workspace.getProjects()) {
            var registry = project.getDataSourceRegistry();
            registry.checkForErrors();
            if (registry.getNetworkProfiles().getProfiles().stream()
                .anyMatch(profile -> profileName.equals(profile.getProfileName()))) {
                continue;
            }
            Map<String, String> connections = new LinkedHashMap<>();
            for (var connection : registry.getDataSources()) {
                var configuration = connection.getConnectionConfiguration();
                if (profileName.equals(configuration.getConfigProfileName()) &&
                    CommonUtils.isEmpty(configuration.getConfigProfileSource())) {
                    connections.put(connection.getId(), CommonUtils.notNull(connection.getName(), connection.getId()));
                }
            }
            if (!connections.isEmpty()) {
                result.add(new ProjectConnections(project.getId(), project.getName(), connections));
            }
        }
        return result;
    }
}
