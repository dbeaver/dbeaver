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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * A simple server that serves the GIS viewer's HTML and related resources.
 */
final class GISLeafletHttpServer {
    static final class Handle implements AutoCloseable {
        private final UUID uuid;
        private final GISLeafletHttpServer server;

        private Handle(@NotNull UUID uuid, @NotNull GISLeafletHttpServer server) {
            this.uuid = uuid;
            this.server = server;
        }

        /**
         * Returns the local URL where the HTML content set for this handle can be accessed.
         *
         * @return the local URL for this handle
         */
        @NotNull
        String getUrl() {
            return server.getUrl() + '/' + uuid;
        }

        /**
         * Sets the HTML content to be served at the URL for this handle.
         *
         * @param index the HTML content to serve
         * @see #getUrl()
         */
        void setIndex(@NotNull String index) {
            server.index.put(uuid, index);
        }

        @Override
        public void close() {
            server.index.remove(uuid);
            release();
        }
    }

    private static final Log log = Log.getLog(GISLeafletHttpServer.class);

    private static final Object lock = new Object();
    private static GISLeafletHttpServer instance;
    private static int refs = 0;

    private final HttpServer server;
    private final Map<UUID, String> index = new ConcurrentHashMap<>();

    private GISLeafletHttpServer() throws IOException {
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

    @NotNull
    static Handle acquire() throws IOException {
        synchronized (lock) {
            if (instance == null) {
                instance = new GISLeafletHttpServer();
            }
            refs++;
            return new Handle(UUID.randomUUID(), instance);
        }
    }

    private static void release() {
        synchronized (lock) {
            refs--;
            if (refs == 0) {
                instance.stop();
                instance = null;
            }
        }
    }

    @NotNull
    private String getUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void stop() {
        server.stop(0);
    }

    private void handle(@NotNull HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        var content = switch (path) {
            case "/inc/leaflet.css",
                 "/inc/leaflet.js",
                 "/inc/layers.png",
                 "/inc/wkx.min.js",
                 "/inc/leaflet-lasso.min.js" -> GISViewerActivator.getDefault().getResourceStream("web" + path);
            default -> {
                UUID uuid;
                try {
                    uuid = UUID.fromString(path.substring(1));
                } catch (Exception e) {
                    yield null;
                }
                String data = index.get(uuid);
                if (data == null) {
                    yield null;
                }
                yield new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            }
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
