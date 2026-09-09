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
package org.jkiss.dbeaver.ui.browser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.HttpConstants;
import org.jkiss.utils.IOUtils;
import org.osgi.framework.Bundle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

/**
 * A reference-counted loopback server for resources used by embedded browsers.
 */
public final class LocalResourceHttpServer {
    @FunctionalInterface
    public interface Resource {
        @NotNull
        static Resource of(@NotNull Resource resource) {
            return resource;
        }

        @Nullable
        InputStream openStream() throws IOException;

        @NotNull
        default TextResource map(@NotNull UnaryOperator<String> mapper) {
            return () -> {
                try (InputStream stream = openStream()) {
                    if (stream == null) {
                        return null;
                    }
                    return mapper.apply(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                }
            };
        }
    }

    @FunctionalInterface
    public interface TextResource {
        /**
         * Produces the resource content for each request.
         */
        @Nullable
        String getContent() throws IOException;

        @NotNull
        default TextResource map(@NotNull UnaryOperator<String> mapper) {
            return () -> {
                String content = getContent();
                return content == null ? null : mapper.apply(content);
            };
        }

        @NotNull
        default Resource toResource() {
            return () -> {
                String content = getContent();
                return content == null ? null : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            };
        }
    }

    public static final class Handle implements AutoCloseable {
        private final UUID id;
        private final LocalResourceHttpServer server;
        private boolean closed;

        private Handle(@NotNull UUID id, @NotNull LocalResourceHttpServer server) {
            this.id = id;
            this.server = server;
        }

        public void addResource(@NotNull String path, @NotNull Resource resource) {
            server.addResource(id, path, resource);
        }

        /**
         * Registers UTF-8 text that is produced for each request.
         */
        public void addTextResource(@NotNull String path, @NotNull TextResource resource) {
            addResource(path, resource.toResource());
        }

        /**
         * Registers files found below the bundle root using paths relative to that root.
         */
        public void addBundleResources(
            @NotNull Bundle bundle,
            @NotNull String root,
            @NotNull BiConsumer<String, URL> registrar
        ) {
            var normalizedRoot = normalizePath(root);
            var resources = bundle.findEntries(normalizedRoot, "*", true);
            while (resources != null && resources.hasMoreElements()) {
                var resource = resources.nextElement();
                var path = resource.getPath();
                int rootPathIndex = path.indexOf(normalizedRoot + '/');
                if (rootPathIndex < 0) {
                    log.error("Unexpected bundle resource path: " + path);
                    continue;
                }
                if (!path.endsWith("/")) {
                    registrar.accept(path.substring(rootPathIndex + normalizedRoot.length() + 1), resource);
                }
            }
        }

        @NotNull
        public String getBaseUrl() {
            return server.getUrl(id, "");
        }

        @NotNull
        public String getUrl(@NotNull String path) {
            return server.getUrl(id, normalizePath(path));
        }

        @Override
        public void close() {
            synchronized (this) {
                if (closed) {
                    return;
                }
                closed = true;
            }
            release(id);
        }
    }

    private static final Log log = Log.getLog(LocalResourceHttpServer.class);
    private static final Object lock = new Object();
    private static LocalResourceHttpServer instance;
    private static int references;

    private final HttpServer server;
    private final ExecutorService executor;
    private final Map<UUID, Map<String, Resource>> resources = new ConcurrentHashMap<>();

    private LocalResourceHttpServer() throws IOException {
        server = HttpServer.create();
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "DBeaver local resource HTTP server");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.start();
    }

    @NotNull
    public static Handle acquire() throws IOException {
        synchronized (lock) {
            if (instance == null) {
                instance = new LocalResourceHttpServer();
            }
            UUID id = UUID.randomUUID();
            instance.resources.put(id, new ConcurrentHashMap<>());
            references++;
            return new Handle(id, instance);
        }
    }

    private static void release(@NotNull UUID id) {
        synchronized (lock) {
            if (instance == null || instance.resources.remove(id) == null) {
                return;
            }
            if (--references == 0) {
                instance.stop();
                instance = null;
            }
        }
    }

    private void addResource(@NotNull UUID id, @NotNull String path, @NotNull Resource resource) {
        Map<String, Resource> handleResources = resources.get(id);
        if (handleResources == null) {
            throw new IllegalStateException("The local resource server handle is closed");
        }
        handleResources.put(normalizePath(path), resource);
    }

    @NotNull
    private String getUrl(@NotNull UUID id, @NotNull String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + '/' + id + '/' + path;
    }

    private void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void handle(@NotNull HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Resource resource = getResource(path);
        try (exchange) {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            if (resource == null) {
                log.trace("No local browser resource found for path: " + path);
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            try (InputStream content = resource.openStream()) {
                if (content == null) {
                    log.trace("No local browser resource content found for path: " + path);
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                log.trace("Serving local browser resource for path: " + path);
                exchange.getResponseHeaders().set(HttpConstants.HEADER_CONTENT_TYPE, getContentType(path));
                exchange.getResponseHeaders().set("X-UA-Compatible", "IE=edge");
                exchange.sendResponseHeaders(200, 0);
                content.transferTo(exchange.getResponseBody());
            } catch (IOException e) {
                log.debug("Error serving local browser resource for path: " + path, e);
                exchange.sendResponseHeaders(500, -1);
            }
        }
    }

    @Nullable
    private Resource getResource(@NotNull String path) {
        String[] segments = path.split("/", 3);
        if (segments.length != 3 || segments[1].isEmpty() || segments[2].isEmpty()) {
            return null;
        }
        UUID id;
        try {
            id = UUID.fromString(segments[1]);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Map<String, Resource> handleResources = resources.get(id);
        return handleResources == null ? null : handleResources.get(segments[2]);
    }

    @NotNull
    private static String normalizePath(@NotNull String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (normalized.isEmpty()
            || normalized.startsWith("../")
            || normalized.endsWith("/..")
            || normalized.contains("/../")
        ) {
            throw new IllegalArgumentException("Invalid resource path: " + path);
        }
        return normalized;
    }

    @NotNull
    private static String getContentType(@NotNull String path) {
        String extension = IOUtils.getFileExtension(path.toLowerCase(Locale.ROOT));
        return switch (extension) {
            case "html", "htm" -> "text/html; charset=UTF-8";
            case "css" -> "text/css; charset=UTF-8";
            case "js", "mjs" -> "text/javascript; charset=UTF-8";
            case "json" -> HttpConstants.CONTENT_TYPE_JSON + "; charset=UTF-8";
            case "png" -> "image/png";
            case "svg" -> "image/svg+xml";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "woff" -> "font/woff";
            case "woff2" -> "font/woff2";
            case null, default -> HttpConstants.CONTENT_TYPE_OCTET_STREAM;
        };
    }
}
