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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads network profile usage from saved local project configurations.
 */
public final class LocalNetworkProfileUsageProvider implements DBWNetworkProfileUsageProvider {
    private final DBPWorkspace workspace;

    public LocalNetworkProfileUsageProvider(@NotNull DBPWorkspace workspace) {
        this.workspace = workspace;
    }

    @NotNull
    @Override
    public Map<String, String> findLocalProfileConflicts(@NotNull String profileName) throws DBException {
        Map<String, String> result = new LinkedHashMap<>();
        for (var project : workspace.getProjects()) {
            if (DataSourceConfigurationProfileQuery.hasLocalProfile(project, profileName)) {
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
            var connections = DataSourceConfigurationProfileQuery.findGlobalProfileConnections(project, profileName);
            if (!connections.isEmpty()) {
                result.add(new ProjectConnections(project.getId(), project.getName(), connections));
            }
        }
        return result;
    }
}
