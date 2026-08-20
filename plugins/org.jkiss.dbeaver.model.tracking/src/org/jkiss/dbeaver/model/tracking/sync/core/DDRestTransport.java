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
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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
    public DDContainer createContainer(@NotNull String label) throws DBException {
        DDContainerData created = execute(
            request(DDSyncApi.WORKSPACE_ENDPOINT, Map.of(DDSyncApi.PARAM_LABEL, label), "POST", EMPTY_BODY),
            DDContainerData.class);
        return toContainer(created);
    }

    @NotNull
    @Override
    public List<DDContainer> listContainers() throws DBException {
        DDContainerData[] data = execute(
            request(DDSyncApi.WORKSPACE_ENDPOINT, Map.of(), "GET", EMPTY_BODY), DDContainerData[].class);
        List<DDContainer> containers = new ArrayList<>(data.length);
        for (DDContainerData container : data) {
            containers.add(toContainer(container));
        }
        return containers;
    }

    @NotNull
    @Override
    public List<DDRawEntry> load(@NotNull String containerId) throws DBException {
        DDResourceData[] data = execute(
            request(
                DDSyncApi.WORKSPACE_DATA_ENDPOINT.replace(DDSyncApi.VAR_WORKSPACE_ID, containerId),
                Map.of(),
                "GET",
                EMPTY_BODY),
            DDResourceData[].class);
        List<DDRawEntry> entries = new ArrayList<>(data.length);
        for (DDResourceData entry : data) {
            entries.add(new DDRawEntry(entry.dataType(), Base64.getDecoder().decode(entry.dataValue())));
        }
        return entries;
    }

    @Override
    public void save(
        @NotNull String containerId,
        @NotNull String key,
        @NotNull byte[] value
    ) throws DBException {
        String endpoint = DDSyncApi.WORKSPACE_DATA_TYPE_ENDPOINT
            .replace(DDSyncApi.VAR_WORKSPACE_ID, containerId)
            .replace(DDSyncApi.VAR_DATA_TYPE, key);
        byte[] body = Base64.getEncoder().encodeToString(value).getBytes(StandardCharsets.UTF_8);
        execute(
            request(endpoint, Map.of(), "PUT", body)
                .header(HttpConstants.HEADER_CONTENT_TYPE, MediaType.TEXT.toString()),
            Void.class);
    }

    @NotNull
    private static DDContainer toContainer(@NotNull DDContainerData data) {
        return new DDContainer(data.workspaceId(), data.label());
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
            return new DDWorkspaceNotFoundException(message);
        }
        return super.mapErrorResponse(code, message, uri);
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
