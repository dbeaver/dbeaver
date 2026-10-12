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
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileManager;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileUsageProvider.ProjectConnections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

class LegacyNetworkProfileUsageProviderTest {
    @Test
    void excludesLocalOverridesAndExternalProfiles() throws DBException {
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        DBPProject localProject = project("local", "Local project");
        DBPProject globalProject = project("global", "Global project");
        Mockito.when(workspace.getProjects()).thenAnswer(invocation -> List.of(localProject, globalProject));

        DBPDataSourceRegistry localRegistry = registry(localProject);
        DBWNetworkProfile localProfile = new DBWNetworkProfile(localProject);
        localProfile.setProfileName("shared");
        Mockito.when(localRegistry.getNetworkProfiles().getProfiles()).thenReturn(List.of(localProfile));
        DBPDataSourceRegistry globalRegistry = registry(globalProject);
        Mockito.when(globalRegistry.getDataSources()).thenAnswer(invocation -> List.of(
            connection("global-connection", "Global connection", "shared", null),
            connection("external-connection", "External connection", "shared", "external"),
            connection("other-connection", "Other connection", "other", null)
        ));

        LegacyNetworkProfileUsageProvider provider = new LegacyNetworkProfileUsageProvider(workspace);
        Assertions.assertEquals(Map.of("local", "Local project"), provider.findLocalProfileConflicts("shared"));
        Assertions.assertEquals(
            List.of(new ProjectConnections(
                "global", "Global project",
                Map.of("global-connection", "Global connection")
            )), provider.findGlobalProfileConnections("shared")
        );
        Mockito.verify(localRegistry, Mockito.never()).getDataSources();
    }

    @Test
    void failsWhenProjectRegistryCannotLoad() throws DBException {
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        DBPProject project = project("broken", "Broken project");
        Mockito.when(workspace.getProjects()).thenAnswer(invocation -> List.of(project));
        DBPDataSourceRegistry registry = registry(project);
        Mockito.doThrow(new DBException("Failed to load project")).when(registry).checkForErrors();

        LegacyNetworkProfileUsageProvider provider = new LegacyNetworkProfileUsageProvider(workspace);
        Assertions.assertThrows(DBException.class, () -> provider.findLocalProfileConflicts("shared"));
        Assertions.assertThrows(DBException.class, () -> provider.findGlobalProfileConnections("shared"));
    }

    @NotNull
    private static DBPProject project(@NotNull String id, @NotNull String name) {
        DBPProject project = Mockito.mock(DBPProject.class);
        Mockito.when(project.getId()).thenReturn(id);
        Mockito.when(project.getName()).thenReturn(name);
        return project;
    }

    @NotNull
    private static DBPDataSourceRegistry registry(@NotNull DBPProject project) {
        DBPDataSourceRegistry registry = Mockito.mock(DBPDataSourceRegistry.class);
        DBWNetworkProfileManager profiles = Mockito.mock(DBWNetworkProfileManager.class);
        Mockito.when(project.getDataSourceRegistry()).thenReturn(registry);
        Mockito.when(registry.getNetworkProfiles()).thenReturn(profiles);
        return registry;
    }

    @NotNull
    private static DBPDataSourceContainer connection(
        @NotNull String id,
        @NotNull String name,
        @NotNull String profileName,
        @Nullable String source
    ) {
        DBPDataSourceContainer connection = Mockito.mock(DBPDataSourceContainer.class);
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setConfigProfileName(profileName);
        configuration.setConfigProfileSource(source);
        Mockito.when(connection.getId()).thenReturn(id);
        Mockito.when(connection.getName()).thenReturn(name);
        Mockito.when(connection.getConnectionConfiguration()).thenReturn(configuration);
        return connection;
    }
}
