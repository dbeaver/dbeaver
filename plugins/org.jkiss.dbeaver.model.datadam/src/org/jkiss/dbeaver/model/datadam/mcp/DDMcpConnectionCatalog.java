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

import com.dbeaver.datadam.mcp.api.exception.DDMcpFunctionException;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionErrorCode;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIContextSettingsDataSource;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;

public class DDMcpConnectionCatalog {
    @Nullable
    private static DDMcpConnectionCatalog instance;

    private final DBDamAccessService accessService = DBDamAccessService.getInstance();
    private final DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();

    private DDMcpConnectionCatalog() {
    }

    @NotNull
    public static synchronized DDMcpConnectionCatalog getInstance() {
        if (instance == null) {
            instance = new DDMcpConnectionCatalog();
        }
        return instance;
    }

    @NotNull
    public List<AvailableConnection> getAllowedConnections() throws DDMcpFunctionException {
        return getAllowedConnections(null);
    }

    @NotNull
    public List<AvailableConnection> getAllowedConnections(@Nullable String projectId) throws DDMcpFunctionException {
        if (accessService.getStatus() != DBDamAccessService.Status.SIGNED_IN) {
            return List.of();
        }
        return workspace.getVisibleProjects().stream()
            .filter(project -> projectId == null || projectId.equals(project.getId()))
            .filter(this::canReadProjectConnections)
            .flatMap(project -> project.getDataSourceRegistry().getDataSources().stream()
                .filter(this::isAllowedConnection)
                .map(connection -> new AvailableConnection(project, connection,
                    !connection.isConnected())))
            .toList();
    }

    private boolean isAllowedConnection(@NotNull DBPDataSourceContainer connection) {
        AIContextSettingsDataSource settings = new AIContextSettingsDataSource(connection);
        return settings.isMetaTransferConfirmed() && settings.isDatadamMcpEnabled();
    }

    private boolean canReadProjectConnections(@NotNull DBPProject project) {
        return project.isOpen() && (project.isRegistryLoaded() || !project.isPasswordProtected());
    }

    @NotNull
    public AvailableConnection getAllowedConnection(@NotNull String projectId, @NotNull String datasourceId)
        throws DDMcpFunctionException {
        for (AvailableConnection available : getAllowedConnections(projectId)) {
            if (available.project().getId().equals(projectId) && available.connection().getId().equals(datasourceId)) {
                return available;
            }
        }
        throw new DDMcpFunctionException(DDMcpFunctionErrorCode.ACCESS_DENIED, "Datasource is unavailable");
    }

    public record AvailableConnection(
        @NotNull DBPProject project,
        @NotNull DBPDataSourceContainer connection,
        boolean requiresManualConnection
    ) {
    }
}
