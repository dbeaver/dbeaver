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
package org.jkiss.dbeaver.ui.gis.panel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.gis.internal.GISViewerActivator;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * A simple server that serves the GIS viewer's HTML and related resources.
 */
final class GISLeafletHttpServer implements Closeable {
    private static final Log log = Log.getLog(GISLeafletHttpServer.class);

    private final HttpServer server;
    private String index;

    public GISLeafletHttpServer() throws IOException {
        server = HttpServer.create();
        server.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("DBeaver Leaflet HTTP server");
            return thread;
        }));
        server.createContext("/", this::handle);
        server.bind(new InetSocketAddress(0), 0);
        server.start();
    }

    /**
     * Returns the local URL where the server is accessible that can be used in a browser.
     *
     * @return the local URL of the server
     */
    @NotNull
    public String getUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /**
     * Sets the HTML content to be served at the root URL ("/").
     *
     * @param index the HTML content to serve
     */
    public void setIndex(@NotNull String index) {
        this.index = index;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(@NotNull HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        var content = switch (path) {
            case "/" -> new ByteArrayInputStream(index.getBytes(StandardCharsets.UTF_8));
            case "/inc/leaflet.css",
                 "/inc/leaflet.js",
                 "/inc/layers.png",
                 "/inc/wkx.min.js",
                 "/inc/leaflet-lasso.min.js" -> GISViewerActivator.getDefault().getResourceStream("web" + path);
            default -> null;
        };
        try (content; exchange) {
            if (content != null) {
                log.debug("Serving content for path: " + path);
                exchange.sendResponseHeaders(200, 0);
                content.transferTo(exchange.getResponseBody());
            } else {
                log.warn("No content found for path: " + path);
                exchange.sendResponseHeaders(404, 0);
            }
        }
    }
}
