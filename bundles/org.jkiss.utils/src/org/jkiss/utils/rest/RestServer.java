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
package org.jkiss.utils.rest;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jkiss.code.NotNull;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class RestServer<T> {
    private static final Logger log = Logger.getLogger(RestServer.class.getName());
    private final HttpServer server;

    public RestServer(
        @NotNull Class<T> cls,
        @NotNull T object,
        @NotNull Gson gson,
        @NotNull Predicate<InetSocketAddress> filter,
        int port,
        int backlog
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), backlog);
        server.createContext("/", createHandler(cls, object, gson, filter));
        server.setExecutor(createExecutor());
        server.start();
    }

    @NotNull
    public static <T> Builder<T> builder(@NotNull Class<T> cls, @NotNull T object) {
        return new Builder<>(object, cls);
    }

    public void stop() {
        stop(1);
    }

    public void stop(int delay) {
        server.stop(delay);

        final Executor executor = server.getExecutor();
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }

    @NotNull
    public InetSocketAddress getAddress() {
        return server.getAddress();
    }

    @NotNull
    protected Executor createExecutor() {
        return new ThreadPoolExecutor(1, 10, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @NotNull
    protected RequestHandler<T> createHandler(
        @NotNull Class<T> cls,
        @NotNull T object,
        @NotNull Gson gson,
        @NotNull Predicate<InetSocketAddress> filter
    ) {
        return new RequestHandler<>(cls, object, gson, filter);
    }

    protected static class RequestHandler<T> implements HttpHandler {
        private static final Type REQUEST_TYPE = new TypeToken<Map<String, JsonElement>>() {}.getType();

        private final T object;
        private final Gson gson;
        private final Map<String, Method> mappings;
        private final Predicate<InetSocketAddress> filter;

        protected RequestHandler(
            @NotNull Class<T> cls,
            @NotNull T object,
            @NotNull Gson gson,
            @NotNull Predicate<InetSocketAddress> filter
        ) {
            this.object = object;
            this.gson = gson;
            this.mappings = createMappings(cls);
            this.filter = filter;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!filter.test(exchange.getRemoteAddress())) {
                log.info("Rejecting remote connection for " + exchange.getRemoteAddress());
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }

            final URI uri = exchange.getRequestURI();
            final String path = uri.getPath().replaceAll("^/+", "");
            final Method method = mappings.get(path);

            if (method == null) {
                log.severe("No mapping for path " + path);
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
                return;
            }

            final Map<String, JsonElement> request;

            try (Reader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                request = gson.fromJson(reader, REQUEST_TYPE);
            } catch (Exception e) {
                throw new IOException("Error reading request", e);
            }

            final Parameter[] parameters = method.getParameters();
            final Object[] values = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                final Parameter p = parameters[i];
                final RequestParameter param = p.getDeclaredAnnotation(RequestParameter.class);
                final JsonElement element = request.getOrDefault(param.value(), JsonNull.INSTANCE);
                values[i] = gson.fromJson(element, p.getParameterizedType());
            }

            final String response;

            try {
                final Object result = method.invoke(object, values);
                final Type type = method.getGenericReturnType();

                if (type != void.class) {
                    response = gson.toJson(result, type);
                } else {
                    response = "";
                }
            } catch (Throwable e) {
                throw new IOException("Error invoking target method " + method, e);
            }

            exchange.sendResponseHeaders(200, response.length());

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody()))) {
                writer.write(response);
            }

            exchange.close();
        }


        @NotNull
        protected Map<String, Method> createMappings(@NotNull Class<T> cls) {
            final Map<String, Method> mappings = new HashMap<>();

            for (Method method : cls.getDeclaredMethods()) {
                if (method.getDeclaringClass() == Object.class) {
                    continue;
                }

                final RequestMapping mapping = method.getDeclaredAnnotation(RequestMapping.class);

                if (mapping == null) {
                    continue;
                }

                if (CommonUtils.isEmptyTrimmed(mapping.value())) {
                    log.warning("Method " + method + " has empty mapping, skipping");
                    continue;
                }

                if (mappings.containsKey(mapping.value())) {
                    log.warning("Method " + method + " has duplicate mapping, skipping");
                    continue;
                }

                method.setAccessible(true);
                mappings.put(mapping.value(), method);
            }

            return Collections.unmodifiableMap(mappings);
        }
    }

    public static final class Builder<T> {
        private static final Predicate<InetSocketAddress> DEFAULT_PREDICATE = address -> true;

        private final T object;
        private final Class<T> cls;
        private Gson gson;
        private int port;
        private int backlog;
        private Predicate<InetSocketAddress> filter = DEFAULT_PREDICATE;

        private Builder(@NotNull T object, @NotNull Class<T> cls) {
            this.object = object;
            this.cls = cls;
            this.gson = RestClient.gson;
            this.port = 0;
            this.backlog = 0;
        }

        @NotNull
        public Builder<T> setGson(@NotNull Gson gson) {
            this.gson = gson;
            return this;
        }

        @NotNull
        public Builder<T> setPort(int port) {
            this.port = port;
            return this;
        }

        @NotNull
        public Builder<T> setBacklog(int backlog) {
            this.backlog = backlog;
            return this;
        }

        @NotNull
        public Builder<T> setFilter(@NotNull Predicate<InetSocketAddress> filter) {
            this.filter = filter;
            return this;
        }

        @NotNull
        public RestServer<T> create() {
            try {
                return new RestServer<>(cls, object, gson, filter, port, backlog);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
