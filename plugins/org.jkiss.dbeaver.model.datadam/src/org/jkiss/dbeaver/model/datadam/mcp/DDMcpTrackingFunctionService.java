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
import com.dbeaver.datadam.mcp.api.model.DDMcpFunctionResult;
import com.dbeaver.datadam.mcp.api.service.desktop.DDMcpFunctionService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.rpc.DDRemoteDamDesktopSessionService;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Records live MCP requests and their parameters without retaining result data. */
public final class DDMcpTrackingFunctionService implements DDMcpFunctionService {
    private static final Log log = Log.getLog(DDMcpTrackingFunctionService.class);

    private final DDMcpFunctionService delegate;
    private final BiConsumer<String, Map<String, Object>> events;

    public DDMcpTrackingFunctionService(@NotNull DDMcpFunctionService delegate) {
        this(delegate, DDMcpTrackingFunctionService::recordEvent);
    }

    public DDMcpTrackingFunctionService(@NotNull DDMcpFunctionService delegate, @NotNull DDConnectionMonitor monitor) {
        this(delegate, monitor::recordMcpRequest);
    }

    private DDMcpTrackingFunctionService(@NotNull DDMcpFunctionService delegate, @NotNull BiConsumer<String, Map<String, Object>> events) {
        this.delegate = delegate;
        this.events = events;
    }

    @Override
    @NotNull
    public List<DDMcpFunctionDescriptor> getFunctions() throws DDMcpFunctionException {
        events.accept("getFunctions", null);
        return delegate.getFunctions();
    }

    @Override
    @NotNull
    public DDMcpFunctionResult callFunction(@NotNull DDMcpFunctionCall call) throws DDMcpFunctionException {
        events.accept("callFunction: " + call.functionId(), call.arguments());
        return delegate.callFunction(call);
    }

    private static void recordEvent(@NotNull String operation, @Nullable Map<String, Object> arguments) {
        try {
            DDRemoteDamDesktopSessionService.getInstance().getMonitor().recordMcpRequest(operation, arguments);
        } catch (DBException e) {
            log.debug("Cannot record DataDam MCP request", e);
        }
    }
}
