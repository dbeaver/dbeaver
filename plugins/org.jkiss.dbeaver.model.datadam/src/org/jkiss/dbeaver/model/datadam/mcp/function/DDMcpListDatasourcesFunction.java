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
package org.jkiss.dbeaver.model.datadam.mcp.function;

import com.dbeaver.datadam.mcp.api.exception.DDMcpFunctionException;
import com.dbeaver.datadam.mcp.api.model.DDMcpDatasource;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionErrorCode;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionResult;
import com.dbeaver.datadam.mcp.api.model.DDMcpListDatasourcesResult;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPDataSourceType;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpConnectionCatalog;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DDMcpListDatasourcesFunction implements DDMcpFunction {
    private final DDMcpConnectionCatalog connections = DDMcpConnectionCatalog.getInstance();

    @NotNull
    @Override
    public DDMcpFunctionResult call(@NotNull Map<String, Object> arguments) throws DDMcpFunctionException {
        Object projectId = arguments.get("projectId");
        if (projectId != null && (!(projectId instanceof String string) || string.isBlank())) {
            throw new DDMcpFunctionException(DDMcpFunctionErrorCode.INVALID_ARGUMENTS, "Invalid projectId");
        }
        List<DDMcpDatasource> datasources = new ArrayList<>();
        for (DDMcpConnectionCatalog.AvailableConnection available : connections.getAllowedConnections((String) projectId)) {
            datasources.add(describeDatasource(available));
        }
        return new DDMcpFunctionResult(new DDMcpListDatasourcesResult(datasources), false);
    }

    @NotNull
    public DDMcpFunctionResult call() throws DDMcpFunctionException {
        return call(Map.of());
    }

    @NotNull
    private DDMcpDatasource describeDatasource(@NotNull DDMcpConnectionCatalog.AvailableConnection available) {
        DBPProject project = available.project();
        DBPDataSourceContainer connection = available.connection();
        DBPDataSourceType type = connection.getDriver().getDataSourceType();
        return new DDMcpDatasource(
            connection.getId(), connection.getName(), project.getId(), project.getName(),
            type == null ? connection.getDriver().getName() : type.getName(), available.requiresManualConnection()
        );
    }
}
