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

import com.dbeaver.datadam.mcp.api.exception.DDMcpStatusException;
import com.dbeaver.datadam.mcp.api.model.DDMcpStatus;
import com.dbeaver.datadam.mcp.api.service.desktop.DDMcpDesktopStatusService;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.jkiss.code.NotNull;

public class DDMcpDesktopStatusServiceImpl implements DDMcpDesktopStatusService {
    private final DDMcpDesktopRpcRegistration mcpRegistration;
    private final MessagingRpcEndpoint endpoint;

    public DDMcpDesktopStatusServiceImpl(
        @NotNull DDMcpDesktopRpcRegistration mcpRegistration,
        @NotNull MessagingRpcEndpoint endpoint
    ) {
        this.mcpRegistration = mcpRegistration;
        this.endpoint = endpoint;
    }

    @Override
    public void setStatus(@NotNull DDMcpStatus status) throws DDMcpStatusException {
        try {
            mcpRegistration.updateRegistration(endpoint, status);
        } catch (Exception e) {
            throw new DDMcpStatusException("Cannot apply DataDam MCP status", e);
        }
    }
}
