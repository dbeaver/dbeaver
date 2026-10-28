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
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionDescriptor;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPDataSourceType;

import java.util.List;

public class DDMcpListDatasourcesFunction {
    private final DDMcpConnectionAccess connectionAccess = new DDMcpConnectionAccess();
    private final DDMcpFunctionDescriptor descriptor = new DDMcpFunctionDescriptor(
        "listDatasources",
        "List datasources",
        "Lists database connections allowed for DataDam MCP calls",
        List.of()
    );

    @NotNull
    public DDMcpFunctionDescriptor getDescriptor() {
        return descriptor;
    }

    @NotNull
    public DDMcpFunctionResult call() throws DDMcpFunctionException {
        JsonArray datasources = new JsonArray();
        for (DBPDataSourceContainer connection : connectionAccess.getAllowedConnections()) {
            datasources.add(describeDatasource(connection.getProject(), connection));
        }
        JsonObject result = new JsonObject();
        result.add("datasources", datasources);
        return new DDMcpFunctionResult(result, false);
    }

    @NotNull
    private JsonObject describeDatasource(@NotNull DBPProject project, @NotNull DBPDataSourceContainer connection) {
        JsonObject datasource = new JsonObject();
        datasource.addProperty("id", connection.getId());
        datasource.addProperty("name", connection.getName());
        datasource.addProperty("project_id", project.getId());
        datasource.addProperty("project_name", project.getName());
        DBPDataSourceType type = connection.getDriver().getDataSourceType();
        datasource.addProperty("database_type", type == null ? connection.getDriver().getName() : type.getName());
        return datasource;
    }
}
