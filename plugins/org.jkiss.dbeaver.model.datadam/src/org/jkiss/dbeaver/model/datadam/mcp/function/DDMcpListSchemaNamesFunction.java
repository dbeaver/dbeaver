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
import com.dbeaver.datadam.mcp.api.model.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIAssistant;
import org.jkiss.dbeaver.model.ai.AIDatabaseScope;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIPromptGenerator;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.ai.functions.AIFunctionParams;
import org.jkiss.dbeaver.model.ai.functions.ListSchemaNamesFunction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.util.List;
import java.util.Map;

public class DDMcpListSchemaNamesFunction {
    private static final Log log = Log.getLog(DDMcpListSchemaNamesFunction.class);

    private final DDMcpConnectionAccess connectionAccess = new DDMcpConnectionAccess();
    private final DDMcpFunctionDescriptor descriptor = new DDMcpFunctionDescriptor(
        "db_listSchemaNames",
        "List schema names",
        "Lists schemas in the selected datasource or catalog",
        List.of(
            new DDMcpFunctionParameter(
                "project_id", DDMcpFunctionParameter.Type.STRING,
                "Project ID returned by listDatasources", true, null, null
            ),
            new DDMcpFunctionParameter(
                "datasource_id", DDMcpFunctionParameter.Type.STRING,
                "Datasource ID returned by listDatasources", true, null, null
            ),
            new DDMcpFunctionParameter(
                AIFunctionParams.CATALOG_PARAM, DDMcpFunctionParameter.Type.STRING,
                "Catalog/database name, if applicable", false, null, null
            )
        )
    );

    @NotNull
    public DDMcpFunctionDescriptor getDescriptor() {
        return descriptor;
    }

    @NotNull
    public DDMcpFunctionResult call(@NotNull DDMcpFunctionCall call) throws DDMcpFunctionException {
        String projectId = requiredString(call, "project_id");
        String datasourceId = requiredString(call, "datasource_id");
        Object catalog = call.arguments().get(AIFunctionParams.CATALOG_PARAM);
        if (catalog != null && !(catalog instanceof String)) {
            throw new DDMcpFunctionException(DDMcpFunctionErrorCode.INVALID_ARGUMENTS, "Invalid catalog name", null);
        }

        var connection = connectionAccess.getAllowedConnection(projectId, datasourceId);
        var monitor = new VoidProgressMonitor();
        try {
            if (!connection.isConnected() && !connection.isCredentialsSaved() && !connection.getDriver().isAnonymousAccess()) {
                JsonObject error = new JsonObject();
                error.addProperty("code", "AUTHENTICATION_REQUIRED");
                error.addProperty("message", "Open connection '%s' in project '%s' in DBeaver to enter credentials, then retry"
                    .formatted(connection.getName(), connection.getProject().getName()));
                return new DDMcpFunctionResult(error, true);
            }
            if (!connection.isConnected() && !connection.connect(monitor, true, false)) {
                return new DDMcpFunctionResult(new JsonPrimitive("Cannot connect to datasource"), true);
            }
            DBPDataSource dataSource = connection.getDataSource();
            if (dataSource == null) {
                return new DDMcpFunctionResult(new JsonPrimitive("Cannot connect to datasource"), true);
            }
            DBCExecutionContext executionContext = DBUtils.getOrOpenDefaultContext(dataSource, false);
            AIDatabaseContext databaseContext = new AIDatabaseContext.Builder(new DBSLogicalDataSource(connection))
                .setExecutionContext(executionContext)
                .setScope(AIDatabaseScope.CURRENT_DATASOURCE)
                .build();
            AIFunctionContext context = new AIFunctionContext(monitor, databaseContext, new EmptyPromptGenerator());
            Map<String, Object> arguments = catalog == null ? Map.of() : Map.of(AIFunctionParams.CATALOG_PARAM, catalog);
            var result = new ListSchemaNamesFunction().callFunction(context, arguments);
            return new DDMcpFunctionResult(new JsonPrimitive(result.getValue().toString()), false);
        } catch (DBException e) {
            log.debug("Cannot list schema names through DataDam MCP", e);
            return new DDMcpFunctionResult(new JsonPrimitive("Cannot list schema names for this datasource"), true);
        }
    }

    @NotNull
    private String requiredString(@NotNull DDMcpFunctionCall call, @NotNull String name) throws DDMcpFunctionException {
        Object value = call.arguments().get(name);
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        throw new DDMcpFunctionException(DDMcpFunctionErrorCode.INVALID_ARGUMENTS, "Missing or invalid " + name, null);
    }

    private class EmptyPromptGenerator implements AIPromptGenerator {
        @NotNull
        @Override
        public String generatorId() {
            return "";
        }

        @NotNull
        @Override
        public String build(@NotNull AIAssistant assistant, @Nullable AIDatabaseContext context) {
            return "";
        }
    }
}
