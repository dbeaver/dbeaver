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

import com.dbeaver.datadam.mcp.api.model.DDMcpStatus;
import com.dbeaver.datadam.mcp.api.service.web.DDMcpWebStatusService;
import com.dbeaver.messaging.rpc.MessagingRpcException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamServerServiceCatalog;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.concurrent.*;

/**
 * Per-connection DataDam MCP permission stored with the connection configuration.
 */
public final class DDMcpConnectionSettings {
    private static final String ENABLED_EXTENSION = "datadam.mcp.enabled";

    private DDMcpConnectionSettings() {
    }

    @Nullable
    public static DDMcpWebStatusService getService() {
        try {
            return DDRemoteDamServerServiceCatalog.getInstance().getMcpStatusService();
        } catch (DBException | MessagingRpcException e) {
            return null;
        }
    }

    /**
     * Cancelling stops waiting for the RPC call, not its execution on the server.
     */
    @Nullable
    public static DDMcpStatus getGlobalStatus(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        return await(
            monitor, () -> {
                DDMcpWebStatusService service = getService();
                return service == null ? null : service.getStatus();
            }
        );
    }

    /**
     * Cancelling stops waiting for the RPC call, not its execution on the server.
     */
    public static void setGlobalStatus(@NotNull DDMcpStatus status, @NotNull DBRProgressMonitor monitor)
    throws DBException, InterruptedException {
        await(
            monitor, () -> {
                DDMcpWebStatusService service = getService();
                if (service == null) {
                    throw new DBException("DataDam MCP status service is unavailable");
                }
                service.setStatus(status);
                return null;
            }
        );
    }

    @Nullable
    private static <T> T await(@NotNull DBRProgressMonitor monitor, @NotNull Callable<T> call)
    throws DBException, InterruptedException {
        if (monitor.isCanceled()) {
            throw new InterruptedException("DataDam MCP request was canceled");
        }
        FutureTask<T> task = new FutureTask<>(call);
        Thread.ofVirtual().name("DataDam MCP request").start(task);
        while (true) {
            if (monitor.isCanceled()) {
                throw new InterruptedException("DataDam MCP request was canceled");
            }
            try {
                return task.get(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // Check whether the user canceled the request before waiting again.
            } catch (ExecutionException e) {
                if (e.getCause() instanceof DBException failure) {
                    throw failure;
                }
                throw new DBException("DataDam MCP request failed", e.getCause());
            }
        }
    }

    public static boolean isEnabled(@NotNull DBPDataSourceContainer connection) {
        return Boolean.TRUE.equals(connection.getExtension(ENABLED_EXTENSION));
    }

    public static void setEnabled(@NotNull DBPDataSourceContainer connection, boolean enabled) {
        connection.setExtension(ENABLED_EXTENSION, enabled ? Boolean.TRUE : null);
    }
}
