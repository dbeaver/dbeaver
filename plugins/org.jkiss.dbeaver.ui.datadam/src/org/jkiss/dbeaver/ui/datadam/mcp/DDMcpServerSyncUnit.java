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
package org.jkiss.dbeaver.ui.datadam.mcp;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sync.DBPSyncScope;
import org.jkiss.dbeaver.model.sync.DBPSyncTarget;
import org.jkiss.dbeaver.model.sync.DBPSyncUnit;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class DDMcpServerSyncUnit implements DBPSyncUnit {

    private static final String RESOURCE_NAME = "mcp-server.json";
    private static final String READ_COMMAND = "com.dbeaver.ai.mcp.server.configuration.read";
    private static final String APPLY_COMMAND = "com.dbeaver.ai.mcp.server.configuration.apply";
    private static final String MONITOR_VARIABLE = "com.dbeaver.ai.mcp.server.configuration.monitor";

    @NotNull
    @Override
    public String getId() {
        return "mcp-server";
    }

    @NotNull
    @Override
    public DBPSyncScope getScope() {
        return DBPSyncScope.WORKSPACE;
    }

    boolean isAvailable() {
        ICommandService commands = PlatformUI.getWorkbench().getService(ICommandService.class);
        return commands != null && commands.getCommand(READ_COMMAND).isDefined() && commands.getCommand(APPLY_COMMAND).isDefined();
    }

    @NotNull
    @Override
    public Map<String, byte[]> read(@NotNull DBPSyncTarget target) throws DBException {
        return Map.of(RESOURCE_NAME, execute(READ_COMMAND, Map.of(), null));
    }

    @Override
    public void write(@NotNull DBPSyncTarget target, @NotNull Map<String, byte[]> resources) throws DBException {
        execute(APPLY_COMMAND, Map.of("configuration", configuration(resources)), null);
    }

    @Override
    public void writeIfUnchanged(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPSyncTarget target,
        @NotNull Map<String, byte[]> expected,
        @NotNull Map<String, byte[]> resources
    ) throws DBException {
        execute(APPLY_COMMAND, Map.of(
            "configuration", configuration(resources),
            "expectedConfiguration", configuration(expected)
        ), monitor);
    }

    @NotNull
    private static String configuration(@NotNull Map<String, byte[]> resources) throws DBException {
        byte[] configuration = resources.get(RESOURCE_NAME);
        if (resources.size() != 1 || configuration == null) {
            throw new DBException("MCP server configuration resource is missing or unsupported");
        }
        return new String(configuration, StandardCharsets.UTF_8);
    }

    @NotNull
    private static byte[] execute(
        @NotNull String commandId,
        @NotNull Map<String, String> parameters,
        @Nullable DBRProgressMonitor monitor
    ) throws DBException {
        try {
            return UIUtils.getDisplay().syncCall(() -> {
                if (monitor != null && monitor.isCanceled()) {
                    throw new OperationCanceledException("MCP server configuration application was canceled");
                }
                var workbench = PlatformUI.getWorkbench();
                ICommandService commands = workbench.getService(ICommandService.class);
                IHandlerService handlers = workbench.getService(IHandlerService.class);
                if (commands == null || handlers == null) {
                    throw new DBException("MCP server configuration API is unavailable");
                }
                var definition = commands.getCommand(commandId);
                if (!definition.isDefined() || (monitor != null && definition.getParameter("expectedConfiguration") == null)) {
                    throw new DBException("MCP server configuration API is unavailable");
                }
                var command = ParameterizedCommand.generateCommand(definition, parameters);
                if (command == null) {
                    throw new DBException("Invalid MCP server configuration command parameters");
                }
                Object result;
                if (monitor == null) {
                    result = handlers.executeCommand(command, null);
                } else {
                    var context = handlers.createContextSnapshot(false);
                    context.addVariable(MONITOR_VARIABLE, monitor);
                    result = handlers.executeCommandInContext(command, null, context);
                }
                if (!(result instanceof byte[] configuration)) {
                    throw new DBException("MCP server configuration command returned no configuration");
                }
                return configuration;
            });
        } catch (DBException e) {
            if (CommonUtils.getRootCause(e) instanceof OperationCanceledException canceled) {
                throw canceled;
            }
            throw e;
        } catch (Exception e) {
            if (CommonUtils.getRootCause(e) instanceof OperationCanceledException canceled) {
                throw canceled;
            }
            throw new DBException("Error executing MCP server configuration command", e);
        }
    }
}
