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

import com.dbeaver.rest.client.AbstractRestClient;
import com.dbeaver.rest.client.MediaType;
import com.dbeaver.rest.client.interceptor.HttpRequestWrapper;
import com.dbeaver.rest.client.interceptor.HttpResponseWrapper;
import com.dbeaver.rest.client.interceptor.InterceptorChain;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

class DDRestTransport extends AbstractRestClient implements DDSyncTransport {

    private static final int TIMEOUT_MS = 30000;
    private static final String SERVER_TIME_HEADER = "X-DD-Server-Time";
    private static final byte[] EMPTY_BODY = new byte[0];

    private final DDSyncCredentials credentials;
    private final ThreadLocal<byte[]> requestBody = new ThreadLocal<>();

    DDRestTransport(@NotNull String url, @NotNull DDSyncCredentials credentials) {
        super(url, DEFAULT_CONNECT_TIMEOUT, TIMEOUT_MS, List.of());
        this.credentials = credentials;
    }

    @NotNull
    @Override
    public List<DDConfigurationSummaryData> listConfigurations() throws DBException {
        return List.of(execute(
            request(DDSyncApi.CONFIGURATION_ENDPOINT, Map.of(), "GET", EMPTY_BODY),
            DDConfigurationSummaryData[].class));
    }

    @NotNull
    @Override
    public DDConfigurationData getConfiguration(@NotNull String configurationId) throws DBException {
        return execute(
            request(
                DDSyncApi.CONFIGURATION_ITEM_ENDPOINT.replace(DDSyncApi.VAR_CONFIGURATION_ID, configurationId),
                Map.of(),
                "GET",
                EMPTY_BODY),
            DDConfigurationData.class);
    }

    @NotNull
    @Override
    public DDConfigurationData createConfiguration(@NotNull DDCreateConfigurationRequest request) throws DBException {
        return execute(
            jsonRequest(DDSyncApi.CONFIGURATION_ENDPOINT, "POST", request),
            DDConfigurationData.class);
    }

    @NotNull
    @Override
    public DDUpdateConfigurationResultData updateConfiguration(
        @NotNull String configurationId,
        @NotNull DDUpdateConfigurationRequest request
    ) throws DBException {
        String endpoint = DDSyncApi.CONFIGURATION_ITEM_ENDPOINT.replace(DDSyncApi.VAR_CONFIGURATION_ID, configurationId);
        return execute(jsonRequest(endpoint, "PUT", request), DDUpdateConfigurationResultData.class);
    }

    @NotNull
    private HttpRequest.Builder jsonRequest(
        @NotNull String endpoint,
        @NotNull String method,
        @NotNull Object payload
    ) throws DBException {
        byte[] body = JSONUtils.GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        return request(endpoint, Map.of(), method, body)
            .header(HttpConstants.HEADER_CONTENT_TYPE, MediaType.JSON.toString());
    }

    @NotNull
    private HttpRequest.Builder request(
        @NotNull String endpoint,
        @NotNull Map<String, String> parameters,
        @NotNull String method,
        @NotNull byte[] body
    ) throws DBException {
        URI uri = buildUri(CommonUtils.removeLeadingSlash(endpoint), parameters);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .header(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.BEARER_PREFIX + buildToken(method, uri, body))
            .method(method, HttpRequest.BodyPublishers.ofByteArray(body));
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
            HttpConstants.BEARER_PREFIX + buildToken(request.method(), uri, requestBody.get()));
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
        if (code == 404) {
            return new DDConfigurationNotFoundException(message);
        }
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
