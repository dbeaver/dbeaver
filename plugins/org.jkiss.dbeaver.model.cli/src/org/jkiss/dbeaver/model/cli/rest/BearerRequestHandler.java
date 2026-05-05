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
package org.jkiss.dbeaver.model.cli.rest;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;
import org.jkiss.utils.rest.RestServer;
import org.jkiss.utils.rest.RpcConstants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class BearerRequestHandler<T> extends RestServer.RequestHandler<T> {
    private static final Logger log = Logger.getLogger(BearerRequestHandler.class.getName());
    @NotNull
    private final String token;

    public BearerRequestHandler(
        @NotNull Class<T> cls,
        @NotNull T object,
        @NotNull Gson gson,
        @NotNull Predicate<InetSocketAddress> filter,
        @Nullable String landingPage,
        @NotNull String token
    ) {
        super(cls, object, gson, filter, landingPage);
        this.token = token;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst(HttpConstants.HEADER_AUTHORIZATION);
        if (CommonUtils.isEmpty(authHeader)
            || !authHeader.startsWith(HttpConstants.BEARER_PREFIX)
            || !token.equals(authHeader.substring(HttpConstants.BEARER_PREFIX.length()))
        ) {
            sendError(exchange, RpcConstants.SC_FORBIDDEN, "Not authorized");
            return;
        }

        super.handle(exchange);
    }
}
