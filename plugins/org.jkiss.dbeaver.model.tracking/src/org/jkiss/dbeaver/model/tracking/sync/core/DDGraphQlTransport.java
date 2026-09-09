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
package org.jkiss.dbeaver.model.tracking.sync.core;

import com.dbeaver.datadam.share.api.model.DDConfiguration;
import com.dbeaver.datadam.share.api.model.DDConfigurationSummary;
import com.dbeaver.datadam.share.api.model.DDCreateConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationResult;
import com.dbeaver.rest.client.AbstractRestClient;
import com.dbeaver.rest.client.MediaType;
import com.dbeaver.rest.client.interceptor.HttpRequestWrapper;
import com.dbeaver.rest.client.interceptor.HttpResponseWrapper;
import com.dbeaver.rest.client.interceptor.InterceptorChain;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

class DDGraphQlTransport extends AbstractRestClient implements DDSyncTransport {

    private static final int TIMEOUT_MS = 30000;
    private static final String SERVER_TIME_HEADER = "X-DD-Server-Time";

    private final DDSyncCredentials credentials;
    private final ThreadLocal<byte[]> requestBody = new ThreadLocal<>();

    DDGraphQlTransport(@NotNull String url, @NotNull DDSyncCredentials credentials) {
        super(url, DEFAULT_CONNECT_TIMEOUT, TIMEOUT_MS, List.of());
        this.credentials = credentials;
    }

    @NotNull
    @Override
    public List<DDConfigurationSummary> listConfigurations() throws DBException {
        JsonObject data = call(DDSyncApi.QUERY_LIST_CONFIGURATIONS, Map.of());
        return List.of(gson.fromJson(data.get("configurations"), DDConfigurationSummary[].class));
    }

    @NotNull
    @Override
    public DDConfiguration getConfiguration(@NotNull String configurationId) throws DBException {
        JsonObject data = call(DDSyncApi.QUERY_GET_CONFIGURATION, Map.of("configurationId", configurationId));
        JsonElement configuration = data.get("configuration");
        if (configuration == null || configuration.isJsonNull()) {
            throw new DDConfigurationNotFoundException("Configuration '" + configurationId + "' not found");
        }
        return gson.fromJson(configuration, DDConfiguration.class);
    }

    @NotNull
    @Override
    public DDConfiguration createConfiguration(@NotNull DDCreateConfigurationRequest request) throws DBException {
        JsonObject data = call(DDSyncApi.MUTATION_CREATE_CONFIGURATION, Map.of("input", request));
        return gson.fromJson(data.get("createConfiguration"), DDConfiguration.class);
    }

    @NotNull
    @Override
    public DDUpdateConfigurationResult updateConfiguration(
        @NotNull String configurationId,
        @NotNull DDUpdateConfigurationRequest request
    ) throws DBException {
        JsonObject data = call(
            DDSyncApi.MUTATION_UPDATE_CONFIGURATION,
            Map.of("configurationId", configurationId, "input", request));
        JsonElement result = data.get("updateConfiguration");
        if (result == null || result.isJsonNull()) {
            throw new DDConfigurationNotFoundException("Configuration '" + configurationId + "' not found");
        }
        return gson.fromJson(result, DDUpdateConfigurationResult.class);
    }

    @NotNull
    private JsonObject call(@NotNull String query, @NotNull Map<String, Object> variables) throws DBException {
        byte[] body = gson.toJson(Map.of("query", query, "variables", variables)).getBytes(StandardCharsets.UTF_8);
        JsonObject response = execute(request(body), JsonObject.class);

        if (response.get("errors") instanceof JsonArray errors && !errors.isEmpty()) {
            throw mapGraphQlError(errors);
        }
        return response.getAsJsonObject("data");
    }

    @NotNull
    private DBException mapGraphQlError(@NotNull JsonArray errors) {
        if (!(errors.get(0) instanceof JsonObject first)) {
            return new DBException("GraphQL request failed");
        }
        String message = first.get("message") instanceof JsonPrimitive m ? m.getAsString() : "GraphQL request failed";
        if ("INTERNAL_ERROR".equals(extractClassification(first))) {
            return new DDTransportException(message);
        }
        return new DBException(message);
    }

    @Nullable
    private static String extractClassification(@NotNull JsonObject error) {
        if (!(error.get("extensions") instanceof JsonObject extensions)) {
            return null;
        }
        return extensions.get("classification") instanceof JsonPrimitive c ? c.getAsString() : null;
    }

    @NotNull
    private HttpRequest.Builder request(@NotNull byte[] body) throws DBException {
        URI uri = buildUri(CommonUtils.removeLeadingSlash(DDSyncApi.GRAPHQL_ENDPOINT), Map.of());
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .header(HttpConstants.HEADER_CONTENT_TYPE, MediaType.JSON.toString())
            .header(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.BEARER_PREFIX + buildToken("POST", uri, body))
            .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        requestBody.set(body);
        return builder;
    }

    @NotNull
    @Override
    protected <T> T execute(@NotNull HttpRequest.Builder builder, @NotNull Type type) throws DBException {
        try {
            return super.execute(builder, type);
        } finally {
            requestBody.remove();
        }
    }

    @NotNull
    @Override
    protected HttpResponseWrapper executeChain(
        @NotNull InterceptorChain chain,
        @NotNull HttpRequestWrapper request,
        @NotNull URI uri
    ) throws Exception {
        HttpResponseWrapper response = chain.proceed(request);
        String serverTime = header(response, SERVER_TIME_HEADER);
        if (serverTime == null) {
            return response;
        }
        credentials.updateServerTime(Long.parseLong(serverTime));
        request.withHeader(
            HttpConstants.HEADER_AUTHORIZATION,
            HttpConstants.BEARER_PREFIX + buildToken("POST", uri, requestBody.get()));
        return chain.proceed(request);
    }

    @NotNull
    private String buildToken(@NotNull String method, @NotNull URI uri, @NotNull byte[] body) throws DBException {
        String pathAndQuery = uri.getRawQuery() == null ? uri.getRawPath() : uri.getRawPath() + "?" + uri.getRawQuery();
        return credentials.buildToken(method, pathAndQuery, body);
    }

    @NotNull
    @Override
    protected DBException mapErrorResponse(int code, @NotNull String message, @NotNull URI uri) {
        if (code >= 500) {
            return new DDTransportException(message);
        }
        return super.mapErrorResponse(code, message, uri);
    }

    @Override
    protected void handleRequestException(@NotNull String message, @NotNull Throwable e) throws DBException {
        if (e instanceof DBException exception) {
            throw exception;
        }
        if (e instanceof IOException) {
            throw new DDTransportException(message, e);
        }
        super.handleRequestException(message, e);
    }

    @Nullable
    private static String header(@NotNull HttpResponseWrapper response, @NotNull String name) {
        return response.headers().entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase(name))
            .flatMap(entry -> entry.getValue().stream())
            .findFirst()
            .orElse(null);
    }
}
