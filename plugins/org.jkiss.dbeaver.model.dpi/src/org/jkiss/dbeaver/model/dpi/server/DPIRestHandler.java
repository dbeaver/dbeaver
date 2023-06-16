/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.dpi.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DPIRestHandler implements HttpHandler {

    private static final Log log = Log.getLog(DPIRestHandler.class);

    private static final Gson gson = new Gson();

    private final DPIRestServer server;
    private final DPIControllerImpl controller;

    public DPIRestHandler(DPIRestServer server) {
        this.server = server;
        this.controller = new DPIControllerImpl(server);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        log.debug("Income DPI request " + exchange.getRequestURI());
        try (InputStream requestBody = exchange.getRequestBody()) {
            if (requestBody == null) {
                throw new IOException("Empty request body");
            }
            Map<String, Object> request = JSONUtils.parseMap(
                gson, new InputStreamReader(requestBody, StandardCharsets.UTF_8));
            log.debug("Processes JSON: " + request);
            exchange.getResponseHeaders().add("content-type", "text/json");
            byte[] responseBody = "{ 'response': 2 }".getBytes();
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
        }
        catch (Throwable e) {
            log.error("Rest handle error", e);
        }
        finally {
            exchange.close();
        }
    }

}
