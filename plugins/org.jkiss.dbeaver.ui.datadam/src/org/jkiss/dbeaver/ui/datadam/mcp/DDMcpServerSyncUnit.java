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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.sync.DBPSyncScope;
import org.jkiss.dbeaver.model.sync.DBPSyncTarget;
import org.jkiss.dbeaver.model.sync.DBPSyncUnit;
import org.jkiss.dbeaver.ui.UIUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class DDMcpServerSyncUnit implements DBPSyncUnit {

    private static final String RESOURCE_NAME = "mcp-server.json";
    private static final String READ_COMMAND = "com.dbeaver.ai.mcp.server.configuration.read";
    private static final String APPLY_COMMAND = "com.dbeaver.ai.mcp.server.configuration.apply";

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

    @NotNull
    @Override
    public Map<String, byte[]> read(@NotNull DBPSyncTarget target) throws DBException {
        return Map.of(RESOURCE_NAME, execute(READ_COMMAND, Map.of()));
    }

    @Override
    public void write(@NotNull DBPSyncTarget target, @NotNull Map<String, byte[]> resources) throws DBException {
        byte[] configuration = resources.get(RESOURCE_NAME);
        if (resources.size() != 1 || configuration == null) {
            throw new DBException("MCP server configuration resource is missing or unsupported");
        }
        execute(APPLY_COMMAND, Map.of("configuration", new String(configuration, StandardCharsets.UTF_8)));
    }

    @NotNull
    private static byte[] execute(@NotNull String commandId, @NotNull Map<String, String> parameters) throws DBException {
        try {
            return UIUtils.getDisplay().syncCall(() -> {
                var workbench = PlatformUI.getWorkbench();
                ICommandService commands = workbench.getService(ICommandService.class);
                IHandlerService handlers = workbench.getService(IHandlerService.class);
                if (commands == null || handlers == null) {
                    throw new DBException("MCP server configuration API is unavailable");
                }
                var definition = commands.getCommand(commandId);
                if (!definition.isDefined()) {
                    throw new DBException("MCP server configuration API is unavailable");
                }
                var command = ParameterizedCommand.generateCommand(definition, parameters);
                if (command == null) {
                    throw new DBException("Invalid MCP server configuration command parameters");
                }
                Object result = handlers.executeCommand(command, null);
                if (!(result instanceof byte[] configuration)) {
                    throw new DBException("MCP server configuration command returned no configuration");
                }
                return configuration;
            });
        } catch (DBException e) {
            throw e;
        } catch (Exception e) {
            throw new DBException("Error executing MCP server configuration command", e);
        }
    }
}
