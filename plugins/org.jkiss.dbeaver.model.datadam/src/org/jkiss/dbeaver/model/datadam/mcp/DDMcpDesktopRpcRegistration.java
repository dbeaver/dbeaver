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
import com.dbeaver.datadam.mcp.api.service.DDMcpFunctionService;
import com.dbeaver.messaging.rpc.MessagingRpcEndpoint;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.rpc.DDDesktopRpcRegistration;

public class DDMcpDesktopRpcRegistration implements DDDesktopRpcRegistration {
    private static final Log log = Log.getLog(DDMcpDesktopRpcRegistration.class);

    @Override
    public void register(@NotNull MessagingRpcEndpoint endpoint) {
        var codec = new DDRemoteDamGsonRpcCodec(new GsonBuilder()
            .serializeNulls()
            .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)
            .create());
        endpoint.register(DDMcpFunctionService.SERVICE_ID, DDMcpFunctionService.class,
            new DDMcpDesktopFunctionService(), codec);
        log.debug("Registered DataDam desktop MCP RPC service");
    }
}
