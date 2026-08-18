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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

class DDRestTransport extends AbstractRestClient implements DDSyncTransport {

    private static final int TIMEOUT_MS = 30000;

    private final DDSyncCredentials credentials;

    DDRestTransport(@NotNull String url, @NotNull DDSyncCredentials credentials) {
        super(url, DEFAULT_CONNECT_TIMEOUT, TIMEOUT_MS, List.of());
        this.credentials = credentials;
    }

    @NotNull
    @Override
    public DDContainer createContainer(@NotNull String label) throws DBException {
        DDContainerData created = execute(
            request(DDSyncApi.WORKSPACE_ENDPOINT, Map.of(DDSyncApi.PARAM_LABEL, label))
                .POST(HttpRequest.BodyPublishers.noBody()),
            DDContainerData.class);
        return toContainer(created);
    }

    @NotNull
    @Override
    public List<DDContainer> listContainers() throws DBException {
        DDContainerData[] data = execute(
            request(DDSyncApi.WORKSPACE_ENDPOINT, Map.of()).GET(), DDContainerData[].class);
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
            request(DDSyncApi.WORKSPACE_DATA_ENDPOINT.replace(DDSyncApi.VAR_WORKSPACE_ID, containerId), Map.of())
                .GET(),
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
        execute(
            request(endpoint, Map.of())
                .header(HttpConstants.HEADER_CONTENT_TYPE, MediaType.TEXT.toString())
                .PUT(HttpRequest.BodyPublishers.ofString(Base64.getEncoder().encodeToString(value))),
            Void.class);
    }

    @NotNull
    private static DDContainer toContainer(@NotNull DDContainerData data) {
        return new DDContainer(data.workspaceId(), data.label());
    }

    @NotNull
    private HttpRequest.Builder request(
        @NotNull String endpoint,
        @NotNull Map<String, String> parameters
    ) throws DBException {
        URI uri = buildUri(CommonUtils.removeLeadingSlash(endpoint), parameters);
        return HttpRequest.newBuilder(uri)
            .header(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.BEARER_PREFIX + credentials.buildToken());
    }
}
