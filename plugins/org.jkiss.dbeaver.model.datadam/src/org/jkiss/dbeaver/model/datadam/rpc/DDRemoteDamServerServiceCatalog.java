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
package org.jkiss.dbeaver.model.datadam.rpc;

import com.dbeaver.datadam.mcp.api.service.web.DDMcpWebStatusService;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

public class DDRemoteDamServerServiceCatalog {
    @Nullable
    private static DDRemoteDamServerServiceCatalog instance;

    private final DDRemoteDamDesktopSessionService session;
    @Nullable
    private MessagingRpcEndpoint endpoint;
    @Nullable
    private DDMcpWebStatusService mcpStatusService;

    private DDRemoteDamServerServiceCatalog() throws DBException {
        session = DDRemoteDamDesktopSessionService.getInstance();
    }

    @NotNull
    public static synchronized DDRemoteDamServerServiceCatalog getInstance() throws DBException {
        if (instance == null) {
            instance = new DDRemoteDamServerServiceCatalog();
        }
        return instance;
    }

    @NotNull
    public DDMcpWebStatusService getMcpStatusService() throws DBException {
        MessagingRpcEndpoint current = session.getRpc();
        DDMcpWebStatusService service = mcpStatusService;
        if (service == null || endpoint != current) {
            service = current.proxy(DDMcpWebStatusService.SERVICE_ID, DDMcpWebStatusService.class);
            mcpStatusService = service;
            endpoint = current;
        }
        return service;
    }
}
