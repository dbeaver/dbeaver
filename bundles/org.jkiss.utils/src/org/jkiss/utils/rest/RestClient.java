/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import java.lang.reflect.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestClient {

    private static final Pattern ST_LINE_PATTERN = Pattern.compile("\\s*at\\s+([\\w/.$]+)\\((.+)\\)");

    private RestClient() {
        // prevents instantiation
    }

    @NotNull
    public static <T> T create(@NotNull URI uri, @NotNull Class<T> cls, @NotNull Gson gson) {
        final Object proxy = Proxy.newProxyInstance(
            cls.getClassLoader(),
            new Class[]{cls, RestProxy.class},
            new ClientInvocationHandler(cls, uri, gson)
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
        @NotNull
        private final Class<?> clientClass;
        private final URI uri;
        private final Gson gson;
        private final ExecutorService httpExecutor;
        private final HttpClient client;
        private final ThreadLocal<Type> resultType = new ThreadLocal<>();

        private ClientInvocationHandler(@NotNull Class<?> clientClass, @NotNull URI uri, @NotNull Gson gson) {
            this.clientClass = clientClass;
            this.uri = uri;
            this.gson = gson;
            this.httpExecutor = Executors.newSingleThreadExecutor();
            this.client = HttpClient.newBuilder()
                .executor(httpExecutor)
                .build();
        }

        @Override
        public synchronized Object invoke(Object proxy, Method method, Object[] args) throws RestException {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class) {
                return BeanUtils.handleObjectMethod(proxy, method, args);
            } else if (declaringClass == RestProxy.class) {
                setNextCallResultType((Type) args[0]);
                return null;
            } else if (method.getName().equals("close") && (declaringClass == AutoCloseable.class || declaringClass == clientClass)) {
                closeClient();
                return null;
            }
            if (httpExecutor.isShutdown() || httpExecutor.isTerminated()) {
                throw new RestException("Rest client has been terminated");
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
                String requestString = gson.toJson(values);

                final HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(requestString));

                if (mapping.timeout() > 0) {
                    builder.timeout(Duration.ofSeconds(mapping.timeout()));
                }

                final HttpResponse<String> response = client.send(
                    builder.build(),
                    readerBodyHandler
                );

                String contents = response.body();
                if (response.statusCode() != RestConstants.SC_OK) {
                    handleError(contents);
                }

                Type returnType = resultType.get();
                if (returnType == null) {
                    returnType = method.getReturnType();
                } else {
                    resultType.remove();
                }
                if (returnType == void.class) {
                    return null;
                }
                if (returnType instanceof TypeVariable) {
                    Type[] bounds = ((TypeVariable<?>) returnType).getBounds();
                    if (bounds.length > 0) {
                        returnType = bounds[0];
                    }
                }
                if (returnType instanceof ParameterizedType && ((ParameterizedType) returnType).getRawType() == Class.class) {
                    // Convert to raw class type to force our serializer to work
                    returnType = Class.class;
                }

                return gson.fromJson(contents, returnType);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RestException(e);
            }
        }

        private void closeClient() {
            if (!httpExecutor.isShutdown()) {
                httpExecutor.shutdown();
            }
        }

        @NotNull
        private static RestException createException(@NotNull Method method, @NotNull String reason) {
            return new RestException("Unable to invoke the method " + method + " because " + reason);
        }

        @Override
        public void setNextCallResultType(Type type) {
            this.resultType.set(type);
        }
    }

    private static void handleError(String contents) throws RestException {
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
        Collections.addAll(stackTraceElements, runtimeException.getStackTrace());
        runtimeException.setStackTrace(stackTraceElements.toArray(new StackTraceElement[0]));

        throw runtimeException;
    }
}
