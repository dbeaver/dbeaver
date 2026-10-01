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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileManager;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileUsageProvider.ProjectConnections;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LegacyNetworkProfileUsageProviderTest {
    @Test
    void excludesLocalOverridesAndExternalProfiles() throws DBException {
        DBPWorkspace workspace = mock(DBPWorkspace.class);
        DBPProject localProject = project("local", "Local project");
        DBPProject globalProject = project("global", "Global project");
        when(workspace.getProjects()).thenAnswer(invocation -> List.of(localProject, globalProject));

        DBPDataSourceRegistry localRegistry = registry(localProject);
        DBWNetworkProfile localProfile = new DBWNetworkProfile(localProject);
        localProfile.setProfileName("shared");
        when(localRegistry.getNetworkProfiles().getProfiles()).thenReturn(List.of(localProfile));
        DBPDataSourceRegistry globalRegistry = registry(globalProject);
        when(globalRegistry.getDataSources()).thenAnswer(invocation -> List.of(
            connection("global-connection", "Global connection", "shared", null),
            connection("external-connection", "External connection", "shared", "external"),
            connection("other-connection", "Other connection", "other", null)
        ));

        LegacyNetworkProfileUsageProvider provider = new LegacyNetworkProfileUsageProvider(workspace);
        assertEquals(Map.of("local", "Local project"), provider.findLocalProfileConflicts("shared"));
        assertEquals(
            List.of(new ProjectConnections(
                "global", "Global project",
                Map.of("global-connection", "Global connection")
            )), provider.findGlobalProfileConnections("shared")
        );
        verify(localRegistry, never()).getDataSources();
    }

    @Test
    void failsWhenProjectRegistryCannotLoad() throws DBException {
        DBPWorkspace workspace = mock(DBPWorkspace.class);
        DBPProject project = project("broken", "Broken project");
        when(workspace.getProjects()).thenAnswer(invocation -> List.of(project));
        DBPDataSourceRegistry registry = registry(project);
        doThrow(new DBException("Failed to load project")).when(registry).checkForErrors();

        LegacyNetworkProfileUsageProvider provider = new LegacyNetworkProfileUsageProvider(workspace);
        assertThrows(DBException.class, () -> provider.findLocalProfileConflicts("shared"));
        assertThrows(DBException.class, () -> provider.findGlobalProfileConnections("shared"));
    }

    private static DBPProject project(String id, String name) {
        DBPProject project = mock(DBPProject.class);
        when(project.getId()).thenReturn(id);
        when(project.getName()).thenReturn(name);
        return project;
    }

    private static DBPDataSourceRegistry registry(DBPProject project) {
        DBPDataSourceRegistry registry = mock(DBPDataSourceRegistry.class);
        DBWNetworkProfileManager profiles = mock(DBWNetworkProfileManager.class);
        when(project.getDataSourceRegistry()).thenReturn(registry);
        when(registry.getNetworkProfiles()).thenReturn(profiles);
        return registry;
    }

    private static DBPDataSourceContainer connection(String id, String name, String profileName, String source) {
        DBPDataSourceContainer connection = mock(DBPDataSourceContainer.class);
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setConfigProfileName(profileName);
        configuration.setConfigProfileSource(source);
        when(connection.getId()).thenReturn(id);
        when(connection.getName()).thenReturn(name);
        when(connection.getConnectionConfiguration()).thenReturn(configuration);
        return connection;
    }
}
