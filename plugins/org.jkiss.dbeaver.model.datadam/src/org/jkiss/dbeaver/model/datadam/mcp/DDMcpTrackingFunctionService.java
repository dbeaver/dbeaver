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
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.datadam.rpc.DBDamSessionService;
import org.jkiss.dbeaver.model.datadam.transport.DDConnectionMonitor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Records MCP requests and responses for the desktop monitoring view. */
public final class DDMcpTrackingFunctionService implements DDMcpFunctionService {
    private static final Log log = Log.getLog(DDMcpTrackingFunctionService.class);

    private final DDMcpFunctionService delegate;
    @Nullable
    private final DDConnectionMonitor monitor;

    public DDMcpTrackingFunctionService(@NotNull DDMcpFunctionService delegate) {
        this(delegate, null);
    }

    public DDMcpTrackingFunctionService(@NotNull DDMcpFunctionService delegate, @Nullable DDConnectionMonitor monitor) {
        this.delegate = delegate;
        this.monitor = monitor;
    }

    @Override
    @NotNull
    public List<DDMcpFunctionDescriptor> getFunctions() throws DDMcpFunctionException {
        return track("getFunctions", null, delegate::getFunctions);
    }

    @Override
    @NotNull
    public DDMcpFunctionResult callFunction(@NotNull DDMcpFunctionCall call) throws DDMcpFunctionException {
        return track("callFunction: " + call.functionId(), call.arguments(), () -> delegate.callFunction(call));
    }

    @NotNull
    private <T> T track(@NotNull String operation, @Nullable Map<String, Object> arguments, @NotNull McpCall<T> call)
        throws DDMcpFunctionException {
        DDConnectionMonitor target = monitor;
        if (target == null) {
            try {
                target = DBDamSessionService.getInstance().getMonitor();
            } catch (DBException e) {
                log.debug("Cannot record DataDam MCP request", e);
            }
        }
        if (target == null) {
            return call.run();
        }
        var request = target.beginMcpRequest(operation, arguments);
        T result;
        try {
            result = call.run();
        } catch (DDMcpFunctionException e) {
            var response = new LinkedHashMap<String, Object>();
            response.put("code", e.getCode());
            response.put("message", e.getMessage());
            target.recordMcpResponse(request, e.getCode().name(), e.getMessage(), JSONUtils.PRETTY_GSON.toJson(response));
            throw e;
        } catch (RuntimeException e) {
            target.recordMcpResponse(request, "INTERNAL_ERROR", e.getMessage(), JSONUtils.PRETTY_GSON.toJson(e.getMessage()));
            throw e;
        }
        boolean failed = result instanceof DDMcpFunctionResult functionResult && functionResult.error();
        String body = JSONUtils.PRETTY_GSON.toJson(result);
        target.recordMcpResponse(request, failed ? "ERROR" : DDConnectionMonitor.MCP_RESULT_OK,
            failed ? JSONUtils.PRETTY_GSON.toJson(((DDMcpFunctionResult) result).value()) : null, body);
        return result;
    }

    @FunctionalInterface
    private interface McpCall<T> {
        @NotNull
        T run() throws DDMcpFunctionException;
    }
}
