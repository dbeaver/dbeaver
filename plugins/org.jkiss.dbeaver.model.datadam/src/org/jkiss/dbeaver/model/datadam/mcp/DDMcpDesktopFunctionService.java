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
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionCall;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionDescriptor;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionErrorCode;
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionResult;
import com.dbeaver.datadam.mcp.api.service.DDMcpFunctionService;
import com.google.gson.JsonPrimitive;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.datadam.mcp.function.DDMcpAiFunctionCatalog;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DDMcpDesktopFunctionService implements DDMcpFunctionService {
    private static final Log log = Log.getLog(DDMcpDesktopFunctionService.class);

    private final DDMcpFunctionRegistry registry = DDMcpFunctionRegistry.getInstance();
    private final DDMcpConnectionCatalog connections = DDMcpConnectionCatalog.getInstance();
    private final DDMcpAiFunctionCatalog catalog = new DDMcpAiFunctionCatalog(connections);

    @NotNull
    @Override
    public List<DDMcpFunctionDescriptor> getFunctions() throws DDMcpFunctionException {
        List<DDMcpFunctionDescriptor> functions = new ArrayList<>(registry.getDescriptors());
        for (DDMcpFunctionDescriptor function : catalog.getFunctions()) {
            if (registry.getDescriptor(function.id()) == null) {
                functions.add(function);
            } else {
                log.warn("Skipping DBeaver MCP function with duplicate ID: " + function.id());
            }
        }
        return functions;
    }

    @NotNull
    @Override
    public DDMcpFunctionResult callFunction(@NotNull DDMcpFunctionCall call) throws DDMcpFunctionException {
        log.debug("Calling DataDam MCP function " + call.functionId());
        DDMcpFunctionExtensionDescriptor descriptor = registry.getDescriptor(call.functionId());
        if (descriptor != null) {
            return descriptor.getInstance().call(call.arguments());
        }
        AIFunctionDescriptor aiFunction = catalog.getFunction(call.functionId());
        if (aiFunction != null) {
            return callAiFunction(aiFunction, call.arguments());
        }
        throw new DDMcpFunctionException(DDMcpFunctionErrorCode.FUNCTION_NOT_FOUND, "Function not found", null);
    }

    @NotNull
    private DDMcpFunctionResult callAiFunction(
        @NotNull AIFunctionDescriptor function,
        @NotNull Map<String, Object> arguments
    ) throws DDMcpFunctionException {
        Map<String, Object> functionArguments = new LinkedHashMap<>(arguments);
        String projectId = requiredString(functionArguments, DDMcpAiFunctionCatalog.PROJECT_ID_PARAMETER);
        String datasourceId = requiredString(functionArguments, DDMcpAiFunctionCatalog.DATASOURCE_ID_PARAMETER);
        var monitor = new VoidProgressMonitor();
        try {
            AIDatabaseContext databaseContext = createDatabaseContext(projectId, datasourceId, monitor);
            if (databaseContext == null) {
                log.debug("No database context available for DataDam MCP function " + function.getFullId());
            }
            AIFunctionContext context = new AIFunctionContext(monitor, databaseContext, new EmptyPromptGenerator());
            AIFunctionResult result = function.getInstance().callFunction(context, functionArguments);
            return new DDMcpFunctionResult(new JsonPrimitive(result.getValue().toString()), false);
        } catch (DBException e) {
            log.debug("Cannot call DBeaver function through DataDam MCP", e);
            throw new DDMcpFunctionException(DDMcpFunctionErrorCode.INTERNAL_ERROR, "Cannot call DBeaver function", null);
        }
    }

    @Nullable
    private AIDatabaseContext createDatabaseContext(
        @NotNull String projectId,
        @NotNull String datasourceId,
        @NotNull DBRProgressMonitor monitor
    ) throws DDMcpFunctionException {
        DBPDataSourceContainer connection = connections.getAllowedConnection(projectId, datasourceId).connection();
        try {
            if (!connectOrWait(connection, monitor)) {
                log.debug("Could not connect DataDam MCP datasource " + connection.getId());
                return null;
            }
            DBPDataSource dataSource = connection.getDataSource();
            if (dataSource == null) {
                log.debug("DataDam MCP datasource is unavailable after connection: " + connection.getId());
                return null;
            }
            DBCExecutionContext executionContext = DBUtils.getOrOpenDefaultContext(dataSource, false);
            return new AIDatabaseContext.Builder(new DBSLogicalDataSource(connection))
                .setExecutionContext(executionContext)
                .setScope(AIDatabaseScope.CURRENT_DATASOURCE)
                .build();
        } catch (DBException e) {
            log.debug("Cannot create DataDam MCP database context", e);
            return null;
        }
    }

    private boolean connectOrWait(@NotNull DBPDataSourceContainer connection, @NotNull DBRProgressMonitor monitor)
        throws DBException, DDMcpFunctionException {
        if (!connection.isConnected() && !connection.isConnecting()) {
            connection.connect(monitor, true, false);
        }
        if (connection.isConnecting()) {
            waitForConnection(connection);
        }
        return connection.isConnected();
    }

    private void waitForConnection(@NotNull DBPDataSourceContainer connection) throws DDMcpFunctionException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (connection.isConnecting() && System.nanoTime() < deadline) {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DDMcpFunctionException(DDMcpFunctionErrorCode.INTERNAL_ERROR,
                    "Waiting for database connection was interrupted", null);
            }
        }
        if (!connection.isConnected()) {
            throw new DDMcpFunctionException(DDMcpFunctionErrorCode.INTERNAL_ERROR,
                connection.isConnecting() ? "Timed out waiting for database connection" : "Database connection is unavailable",
                null);
        }
    }

    @NotNull
    private String requiredString(@NotNull Map<String, Object> arguments, @NotNull String name)
    throws DDMcpFunctionException {
        Object value = arguments.remove(name);
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
