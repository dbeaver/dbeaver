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

import com.dbeaver.datadam.dam.remote.api.rpc.DDRemoteDamGsonRpcCodec;
import com.dbeaver.datadam.mcp.api.model.DDMcpStatus;
import com.dbeaver.datadam.mcp.api.service.desktop.DDMcpFunctionService;
import com.dbeaver.datadam.mcp.api.service.web.DDMcpWebStatusService;
import com.dbeaver.messaging.requestreply.MessagingSubscription;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import com.dbeaver.messaging.rpc.MessagingRpcException;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.rpc.DDDesktopRpcRegistration;

public class DDMcpDesktopRpcRegistration implements DDDesktopRpcRegistration {
    private static final Log log = Log.getLog(DDMcpDesktopRpcRegistration.class);

    @Nullable
    private MessagingRpcEndpoint endpoint;
    @Nullable
    private MessagingSubscription subscription;

    @NotNull
    @Override
    public String getId() {
        return DDMcpFunctionService.SERVICE_ID;
    }

    @Override
    public synchronized void register(@NotNull MessagingRpcEndpoint endpoint) throws DBException {
        if (this.endpoint != endpoint) {
            unregister();
            this.endpoint = endpoint;
        }
        DDMcpStatus status = readStatus(endpoint);
        updateRegistration(endpoint, status);
    }

    @NotNull
    private DDMcpStatus readStatus(@NotNull MessagingRpcEndpoint endpoint) throws DBException {
        try {
            return endpoint.proxy(DDMcpWebStatusService.SERVICE_ID, DDMcpWebStatusService.class).getStatus();
        } catch (MessagingRpcException e) {
            throw new DBException("Cannot read DataDam MCP status", e);
        }
    }

    public synchronized void updateRegistration(@NotNull MessagingRpcEndpoint endpoint, @NotNull DDMcpStatus status) {
        if (endpoint.isClosed()) {
            throw new MessagingRpcException(MessagingRpcException.Reason.CLOSED);
        }
        if (status == DDMcpStatus.ENABLED) {
            if (subscription == null) {
                registerService(endpoint);
            }
        } else {
            unregister();
        }
    }

    private void registerService(@NotNull MessagingRpcEndpoint endpoint) {
        var codec = new DDRemoteDamGsonRpcCodec(new GsonBuilder()
            .serializeNulls()
            .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)
            .create());
        subscription = endpoint.register(
            DDMcpFunctionService.SERVICE_ID, DDMcpFunctionService.class,
            new DDMcpDesktopFunctionService(), codec);
    }

    private void unregister() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
            log.debug("Unregistered DataDam desktop MCP RPC service");
        }
    }
}
