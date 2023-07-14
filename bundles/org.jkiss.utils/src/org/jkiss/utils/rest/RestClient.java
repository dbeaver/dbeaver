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
import org.jkiss.code.NotNull;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestClient {

    private RestClient() {
        // prevents instantiation
    }

    @NotNull
    public static <T> T create(@NotNull URI uri, @NotNull Class<T> cls, @NotNull Gson gson) {
        final Object proxy = Proxy.newProxyInstance(
            cls.getClassLoader(),
            new Class[]{cls, RestProxy.class},
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
            this.gson = RestConstants.DEFAULT_GSON;
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

    private static class ClientInvocationHandler implements InvocationHandler, RestProxy {
        private final URI uri;
        private final Gson gson;
        private final HttpClient client;
        private final ThreadLocal<Class<?>> resultType = new ThreadLocal<>();

        private ClientInvocationHandler(@NotNull URI uri, @NotNull Gson gson) {
            this.uri = uri;
            this.gson = gson;
            this.client = HttpClient.newHttpClient();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws RestException {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class) {
                return BeanUtils.handleObjectMethod(proxy, method, args);
            } else if (declaringClass == RestProxy.class) {
                setNextCallResultType((Class<?>) args[0]);
                return null;
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
                String endpoint = mapping.value();
                if (CommonUtils.isEmpty(endpoint)) {
                    endpoint = method.getName();
                }
                StringBuilder url = new StringBuilder();
                url.append(uri);
                if (url.charAt(url.length() - 1) != '/') url.append('/');
                url.append(endpoint);
                HttpResponse.BodyHandler<String> readerBodyHandler =
                    info -> BodySubscribers.ofString(StandardCharsets.UTF_8);
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(gson.toJson(values)))
                    .build();
                final HttpResponse<String> response = client.send(
                    httpRequest,
                    readerBodyHandler
                );

                String contents = response.body();
                if (response.statusCode() != RestConstants.SC_OK) {
                    handleError(response.statusCode(), contents);
                }

                Class<?> returnType = resultType.get();
                if (returnType == null) {
                    returnType = method.getReturnType();
                } else {
                    resultType.remove();
                }
                if (returnType == void.class) {
                    return null;
                }

                return gson.fromJson(contents, returnType);
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

        @Override
        public void setNextCallResultType(Class<?> type) {
            this.resultType.set(type);
        }
    }

    private static final Pattern ST_LINE_PATTERN = Pattern.compile("\\s*at\\s+([\\w/.$]+)\\((.+)\\)");
    private static void handleError(int errorCode, String contents) throws RestException {
        String[] stackTraceRows = contents.split("\n");
        String errorLine = stackTraceRows[0];
        List<StackTraceElement> stackTraceElements = new ArrayList<>();
        for (int i = 1; i < stackTraceRows.length; i++) {
            Matcher matcher = ST_LINE_PATTERN.matcher(stackTraceRows[i]);
            if (matcher.find()) {
                String methodRef = matcher.group(1);
                int divPos = methodRef.lastIndexOf('.');
                String className = methodRef.substring(0, divPos);
                String methodName = methodRef.substring(divPos + 1);

                String classRef = matcher.group(2);
                divPos = classRef.indexOf(':');
                String fileName;
                int fileLine;
                if (divPos == -1) {
                    fileName = classRef;
                    fileLine = -1;
                } else {
                    fileName = classRef.substring(0, divPos).trim();
                    fileLine = CommonUtils.toInt(classRef.substring(divPos + 1).trim());
                }
                stackTraceElements.add(
                    new StackTraceElement(className, methodName, fileName, fileLine));
            }
        }
        RestException runtimeException = new RestException(errorLine);
        runtimeException.setStackTrace(stackTraceElements.toArray(new StackTraceElement[0]));

        throw runtimeException;
    }
}
