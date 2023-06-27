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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.jkiss.code.NotNull;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscribers;
import java.util.LinkedHashMap;
import java.util.Map;

public class RestClient {
    static final Gson gson = new GsonBuilder()
        .setLenient()
        .disableHtmlEscaping()
        .serializeNulls()
        .create();

    private RestClient() {
        // prevents instantiation
    }

    @NotNull
    public static <T> T create(@NotNull URI uri, @NotNull Class<T> cls, @NotNull Gson gson) {
        final Object proxy = Proxy.newProxyInstance(
            cls.getClassLoader(),
            new Class[]{cls},
            new ClientInvocationHandler(uri, gson)
        );

        return cls.cast(proxy);
    }

    @NotNull
    public static <T> Builder<T> builder(@NotNull URI uri, @NotNull Class<T> cls) {
        return new Builder<>(uri, cls);
    }

    public static final class Builder<T> {
        private final URI uri;
        private final Class<T> cls;
        private Gson gson;

        private Builder(@NotNull URI uri, @NotNull Class<T> cls) {
            this.uri = uri;
            this.cls = cls;
            this.gson = RestClient.gson;
        }

        @NotNull
        public Builder<T> setGson(@NotNull Gson gson) {
            this.gson = gson;
            return this;
        }

        @NotNull
        public T create() {
            return RestClient.create(uri, cls, gson);
        }
    }

    private static class ClientInvocationHandler implements InvocationHandler {
        private final URI uri;
        private final Gson gson;
        private final HttpClient client;

        private ClientInvocationHandler(@NotNull URI uri, @NotNull Gson gson) {
            this.uri = uri;
            this.gson = gson;
            this.client = HttpClient.newHttpClient();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws RestException {
            if (method.getDeclaringClass() == Object.class) {
                return BeanUtils.handleObjectMethod(proxy, method, args);
            }

            final RequestMapping mapping = method.getDeclaredAnnotation(RequestMapping.class);

            if (mapping == null) {
                throw createException(method, "it's not annotated with @RequestMapping");
            }

            final Parameter[] parameters = method.getParameters();
            final Map<String, JsonElement> values = new LinkedHashMap<>(parameters.length);

            for (int i = 0; i < parameters.length; i++) {
                final Parameter p = parameters[i];
                final RequestParameter param = p.getDeclaredAnnotation(RequestParameter.class);

                if (param == null) {
                    throw createException(method, "one or more of its parameters are not annotated with @RequestParameter");
                }

                if (CommonUtils.isEmptyTrimmed(param.value())) {
                    throw createException(method, "one or more of its parameters has empty name specified in @RequestParameter");
                }

                if (values.put(param.value(), gson.toJsonTree(args[i])) != null) {
                    throw createException(method, "one or more of its parameters share the same name specified in @RequestParameter");
                }
            }

            try {
                final HttpResponse<Reader> response = client.send(
                    HttpRequest.newBuilder()
                        .uri(URI.create(uri.toString() + '/' + mapping.value()))
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString(gson.toJson(values)))
                        .build(),
                    info -> BodySubscribers.mapping(
                        BodySubscribers.ofInputStream(),
                        InputStreamReader::new
                    )
                );

                try (Reader reader = response.body()) {
                    if (response.statusCode() != 200) {
                        final StringWriter writer = new StringWriter();
                        reader.transferTo(writer);
                        throw new RestException(writer.toString());
                    }

                    if (method.getReturnType() == void.class) {
                        return null;
                    }

                    return gson.fromJson(response.body(), method.getReturnType());
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RestException(e);
            }
        }

        @NotNull
        private static RestException createException(@NotNull Method method, @NotNull String reason) {
            return new RestException("Unable to invoke the method " + method + " because " + reason);
        }
    }
}
