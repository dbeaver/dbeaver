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
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPDataSourceType;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpConnectionCatalog;
import org.jkiss.dbeaver.model.datadam.mcp.DDMcpFunction;

import java.util.Map;

public class DDMcpListDatasourcesFunction implements DDMcpFunction {
    private final DDMcpConnectionCatalog connections = DDMcpConnectionCatalog.getInstance();

    @NotNull
    @Override
    public DDMcpFunctionResult call(@NotNull Map<String, Object> arguments) throws DDMcpFunctionException {
        return call();
    }

    @NotNull
    public DDMcpFunctionResult call() throws DDMcpFunctionException {
        JsonArray datasources = new JsonArray();
        for (DDMcpConnectionCatalog.AvailableConnection available : connections.getAllowedConnections()) {
            datasources.add(describeDatasource(available));
        }
        JsonObject result = new JsonObject();
        result.add("datasources", datasources);
        return new DDMcpFunctionResult(result, false);
    }

    @NotNull
    private JsonObject describeDatasource(@NotNull DDMcpConnectionCatalog.AvailableConnection available) {
        DBPProject project = available.project();
        DBPDataSourceContainer connection = available.connection();
        JsonObject datasource = new JsonObject();
        datasource.addProperty("id", connection.getId());
        datasource.addProperty("name", connection.getName());
        datasource.addProperty("project_id", project.getId());
        datasource.addProperty("project_name", project.getName());
        DBPDataSourceType type = connection.getDriver().getDataSourceType();
        datasource.addProperty("database_type", type == null ? connection.getDriver().getName() : type.getName());
        datasource.addProperty("requires_manual_connection", available.requiresManualConnection());
        return datasource;
    }
}
