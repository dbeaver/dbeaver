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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.DDWorkspace;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;

import java.util.List;

public class DDMcpConnectionCatalog {
    private static final Log log = Log.getLog(DDMcpConnectionCatalog.class);
    private static final String ENABLED_EXTENSION = "datadam.mcp.enabled";

    @Nullable
    private static DDMcpConnectionCatalog instance;

    private final DDWorkspace workspace = DDWorkspace.getInstance();

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
        if (DDDesktopAccessService.getInstance().getStatus() != DDDesktopAccessService.Status.SIGNED_IN) {
            return List.of();
        }
        return getConfiguredConnections();
    }

    @NotNull
    public List<AvailableConnection> getConfiguredConnections() {
        return workspace.getProjects().stream()
            .flatMap(project -> getMCPConnections(project).stream())
            .toList();
    }

    @NotNull
    public List<AvailableConnection> getMCPConnections(@NotNull DBPProject project) {
        if (!canReadProjectConnections(project)) {
            return List.of();
        }
        return project.getDataSourceRegistry().getDataSources().stream()
            .filter(this::isEnabled)
            .map(this::getAvailableConnection)
            .toList();
    }

    public boolean isEnabled(@NotNull DBPDataSourceContainer connection) {
        return Boolean.TRUE.equals(connection.getExtension(ENABLED_EXTENSION));
    }

    public void setEnabled(@NotNull DBPDataSourceContainer connection, boolean enabled) {
        connection.setExtension(ENABLED_EXTENSION, enabled ? Boolean.TRUE : null);
    }

    @NotNull
    public AvailableConnection getAvailableConnection(@NotNull DBPDataSourceContainer connection) {
        return new AvailableConnection(connection.getProject(), connection, requiresManualConnection(connection));
    }

    private boolean canReadProjectConnections(@NotNull DBPProject project) {
        return project.isOpen() && (project.isRegistryLoaded() || !project.isPasswordProtected());
    }

    private boolean requiresManualConnection(@NotNull DBPDataSourceContainer connection) {
        if (connection.isConnected() || connection.getDriver().isAnonymousAccess()) {
            return false;
        }
        try {
            return !connection.isCredentialsSaved();
        } catch (DBException e) {
            log.error("Cannot check saved DataDam connection credentials", e);
            return true;
        }
    }

    @NotNull
    public AvailableConnection getAllowedConnection(@NotNull String projectId, @NotNull String datasourceId)
        throws DDMcpFunctionException {
        for (AvailableConnection available : getAllowedConnections()) {
            if (available.project().getId().equals(projectId) && available.connection().getId().equals(datasourceId)) {
                return available;
            }
        }
        throw new DDMcpFunctionException(DDMcpFunctionErrorCode.ACCESS_DENIED, "Datasource is unavailable", null);
    }

    public record AvailableConnection(
        @NotNull DBPProject project,
        @NotNull DBPDataSourceContainer connection,
        boolean requiresManualConnection
    ) {
    }
}
