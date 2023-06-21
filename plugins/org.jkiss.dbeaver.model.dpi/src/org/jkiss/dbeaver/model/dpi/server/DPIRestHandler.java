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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.dpi.api.ApiEndpoint;
import org.jkiss.dbeaver.model.dpi.api.DPIController;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
        String uri = exchange.getRequestURI().toString();
        uri = CommonUtils.removeTrailingSlash(CommonUtils.removeLeadingSlash(uri));

        log.debug("Income DPI request " + uri);
        try (InputStream requestBody = exchange.getRequestBody()) {
            Map<String, Object> parameters;
            if (requestBody != null) {
                parameters = JSONUtils.parseMap(
                    gson, new InputStreamReader(requestBody, StandardCharsets.UTF_8));
            } else {
                parameters = Collections.emptyMap();
            }

            Method apiMethod = null;
            for (Method method : DPIController.class.getMethods()) {{
                ApiEndpoint apiEndpoint = method.getAnnotation(ApiEndpoint.class);
                if (apiEndpoint != null) {
                    String methodId = apiEndpoint.id();
                    if (CommonUtils.isEmpty(methodId)) {
                        methodId = method.getName();
                    }
                    if (CommonUtils.equalObjects(methodId, uri)) {
                        apiMethod = method;
                        break;
                    }
                }
            }}
            if (apiMethod == null) {
                log.debug("API method " + uri + " not found");
                sendError(exchange, new DBException("Invalid request endpoint: " + uri));
                return;
            }

            Object result = apiMethod.invoke(controller);

            exchange.getResponseHeaders().add("content-type", "text/json");
            String responseJson = gson.toJson(result);

            byte[] responseBody = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
        }
        catch (Throwable e) {
            log.error("Rest handle error", e);
            sendError(exchange, e);
        }
        finally {
            exchange.close();
        }
    }

    private void sendError(HttpExchange exchange, Throwable error) throws IOException {
        exchange.getResponseHeaders().add("content-type", "text/plain");
        StringWriter buffer = new StringWriter();
        error.printStackTrace(new PrintWriter(buffer, true));
        byte[] result = buffer.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(400, result.length);
        exchange.getResponseBody().write(result);

    }

}
