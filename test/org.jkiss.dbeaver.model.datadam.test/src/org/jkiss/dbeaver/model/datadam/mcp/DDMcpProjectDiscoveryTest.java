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
package org.jkiss.dbeaver.model.datadam.mcp;

import com.dbeaver.datadam.mcp.api.model.DDMcpListDatasourcesResult;
import com.dbeaver.datadam.mcp.api.model.DDMcpListProjectsResult;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.datadam.mcp.function.DDMcpListDatasourcesFunction;
import org.jkiss.dbeaver.model.datadam.mcp.function.DDMcpListProjectsFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

class DDMcpProjectDiscoveryTest {
    @Test
    void listsOnlyProjectsFromAllowedConnectionsWithoutDuplicates() throws Exception {
        var catalog = Mockito.mock(DDMcpConnectionCatalog.class);
        try (var singleton = Mockito.mockStatic(DDMcpConnectionCatalog.class)) {
            singleton.when(DDMcpConnectionCatalog::getInstance).thenReturn(catalog);
            var first = connection("first");
            var second = connection("second");
            Mockito.when(catalog.getAllowedConnections()).thenReturn(List.of(first, first, second));

            var projects = Assertions.assertInstanceOf(DDMcpListProjectsResult.class,
                new DDMcpListProjectsFunction().call(Map.of()).value()).projects();

            Assertions.assertEquals(2, projects.size());
            Assertions.assertEquals("first", projects.get(0).id());
            Assertions.assertEquals("Project first", projects.get(0).name());
            Assertions.assertNull(projects.get(0).description());
            Assertions.assertEquals("second", projects.get(1).id());

            Mockito.when(catalog.getAllowedConnections()).thenReturn(List.of());
            Assertions.assertTrue(Assertions.assertInstanceOf(DDMcpListProjectsResult.class,
                new DDMcpListProjectsFunction().call(Map.of()).value()).projects().isEmpty());
        }
    }

    @Test
    void filtersDatasourcesByProjectAndPreservesUnfilteredCalls() throws Exception {
        var catalog = Mockito.mock(DDMcpConnectionCatalog.class);
        try (var singleton = Mockito.mockStatic(DDMcpConnectionCatalog.class)) {
            singleton.when(DDMcpConnectionCatalog::getInstance).thenReturn(catalog);
            var first = connection("first");
            var second = connection("second");
            Mockito.when(catalog.getAllowedConnections(null)).thenReturn(List.of(first, second));
            Mockito.when(catalog.getAllowedConnections("second")).thenReturn(List.of(second));
            Mockito.when(catalog.getAllowedConnections("unknown")).thenReturn(List.of());
            var function = new DDMcpListDatasourcesFunction();

            Assertions.assertEquals(2, Assertions.assertInstanceOf(DDMcpListDatasourcesResult.class,
                function.call().value()).datasources().size());
            Assertions.assertEquals(function.call(), function.call(Map.of()));
            var filtered = Assertions.assertInstanceOf(DDMcpListDatasourcesResult.class,
                function.call(Map.of("projectId", "second")).value()).datasources();
            Assertions.assertEquals(1, filtered.size());
            Assertions.assertEquals("second", filtered.get(0).projectId());
            Assertions.assertTrue(Assertions.assertInstanceOf(DDMcpListDatasourcesResult.class,
                function.call(Map.of("projectId", "unknown")).value()).datasources().isEmpty());
            Mockito.verify(catalog).getAllowedConnections("second");
            Mockito.verify(catalog).getAllowedConnections("unknown");
        }
    }

    @NotNull
    private DDMcpConnectionCatalog.AvailableConnection connection(@NotNull String projectId) {
        var project = Mockito.mock(DBPProject.class);
        Mockito.when(project.getId()).thenReturn(projectId);
        Mockito.when(project.getName()).thenReturn("Project " + projectId);
        var connection = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(connection.getId()).thenReturn("connection-" + projectId);
        Mockito.when(connection.getName()).thenReturn("Connection " + projectId);
        Mockito.when(connection.getDriver()).thenReturn(Mockito.mock(DBPDriver.class));
        return new DDMcpConnectionCatalog.AvailableConnection(project, connection, true);
    }
}
