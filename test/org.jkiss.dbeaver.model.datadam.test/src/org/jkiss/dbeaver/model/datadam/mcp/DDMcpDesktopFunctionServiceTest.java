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
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionErrorCode;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIFunction;
import org.jkiss.dbeaver.model.ai.AIFunctionDescriptor;
import org.jkiss.dbeaver.model.ai.AIFunctionPurpose;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.AIFunctionType;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.mcp.function.DDMcpAiFunctionCatalog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.Map;

class DDMcpDesktopFunctionServiceTest {
    @Test
    void callsDamFunctionsWithoutAConnectionAndPreservesMcpErrors() throws Exception {
        var descriptor = Mockito.mock(AIFunctionDescriptor.class);
        Mockito.when(descriptor.getPurpose()).thenReturn(AIFunctionPurpose.DAM);
        var function = Mockito.mock(AIFunction.class);
        Mockito.when(descriptor.getInstance()).thenReturn(function);
        Mockito.when(function.callFunction(Mockito.any(), Mockito.anyMap()))
            .thenReturn(new AIFunctionResult(AIFunctionType.INFORMATION, "projects"))
            .thenThrow(new DBException("Invalid projectId",
                new DDMcpFunctionException(DDMcpFunctionErrorCode.INVALID_ARGUMENTS, "Invalid projectId")));
        try (var connections = Mockito.mockStatic(DDMcpConnectionCatalog.class);
             var functions = Mockito.mockConstruction(DDMcpAiFunctionCatalog.class, (catalog, context) ->
                 Mockito.when(catalog.getFunction("listProjects")).thenReturn(descriptor))) {
            var service = new DDMcpDesktopFunctionService();
            var call = new DDMcpFunctionCall("listProjects", Map.of());

            Assertions.assertEquals("projects", service.callFunction(call).value());
            var error = Assertions.assertThrows(DDMcpFunctionException.class, () -> service.callFunction(call));
            Assertions.assertEquals(DDMcpFunctionErrorCode.INVALID_ARGUMENTS, error.getCode());
            Assertions.assertEquals("Invalid projectId", error.getMessage());
        }
    }

    @Test
    void reportsFunctionErrorsWithAllCauseMessagesButNoStackTrace() throws Exception {
        var connections = Mockito.mock(DDMcpConnectionCatalog.class);
        var connection = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(connections.getAllowedConnection("project", "connection")).thenReturn(
            new DDMcpConnectionCatalog.AvailableConnection(Mockito.mock(DBPProject.class), connection, true));
        var function = Mockito.mock(AIFunction.class);
        var failure = new DBException("Function failed", new SQLException("SQL failed", new IllegalStateException("Permission denied")));
        Mockito.when(function.callFunction(Mockito.any(), Mockito.anyMap())).thenThrow(failure);
        var descriptor = Mockito.mock(AIFunctionDescriptor.class);
        Mockito.when(descriptor.getInstance()).thenReturn(function);
        try (var catalogs = Mockito.mockStatic(DDMcpConnectionCatalog.class);
             var functions = Mockito.mockConstruction(DDMcpAiFunctionCatalog.class, (catalog, context) ->
                 Mockito.when(catalog.getFunction("db_executeDQL")).thenReturn(descriptor))) {
            catalogs.when(DDMcpConnectionCatalog::getInstance).thenReturn(connections);
            var service = new DDMcpDesktopFunctionService();

            var error = Assertions.assertThrows(DDMcpFunctionException.class, () -> service.callFunction(
                new DDMcpFunctionCall("db_executeDQL", Map.of("project_id", "project", "datasource_id", "connection"))));

            Assertions.assertEquals(DDMcpFunctionErrorCode.FUNCTION_ERROR, error.getCode());
            Assertions.assertEquals("Function failed:\nSQL failed:\nPermission denied", error.getMessage());
            Mockito.verify(function).callFunction(Mockito.any(), Mockito.anyMap());
        }
    }
}
