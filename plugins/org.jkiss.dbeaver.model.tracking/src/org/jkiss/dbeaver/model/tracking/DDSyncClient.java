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
package org.jkiss.dbeaver.model.tracking;

import com.dbeaver.rest.client.AbstractRestClient;
import com.dbeaver.rest.client.MediaType;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.HttpConstants;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class DDSyncClient extends AbstractRestClient implements DDWorkspaceService {

    private static final int TIMEOUT_MS = 30000;

    private final DDAccessKey accessKey;

    public DDSyncClient(@NotNull String url, @NotNull DDAccessKey accessKey) {
        super(url, DEFAULT_CONNECT_TIMEOUT, TIMEOUT_MS, List.of());
        this.accessKey = accessKey;
    }

    @NotNull
    @Override
    public byte[] loadDataKey() throws DBException {
        DDDataKey dataKey = execute(request(DATA_KEY_ENDPOINT, Map.of()).GET(), DDDataKey.class);
        return Base64.getDecoder().decode(dataKey.encryptedKey());
    }

    @NotNull
    @Override
    public DDWorkspace createWorkspace(@NotNull String label) throws DBException {
        return execute(
            request(WORKSPACE_ENDPOINT, Map.of(PARAM_LABEL, label)).POST(HttpRequest.BodyPublishers.noBody()),
            DDWorkspace.class);
    }

    @NotNull
    @Override
    public List<DDWorkspace> listWorkspaces() throws DBException {
        DDWorkspace[] workspaces = execute(request(WORKSPACE_ENDPOINT, Map.of()).GET(), DDWorkspace[].class);
        return List.of(workspaces);
    }

    @NotNull
    @Override
    public List<DDWorkspaceData> loadData(@NotNull String workspaceId) throws DBException {
        DDWorkspaceData[] data = execute(
            request(WORKSPACE_DATA_ENDPOINT.replace(VAR_WORKSPACE_ID, workspaceId), Map.of()).GET(),
            DDWorkspaceData[].class);
        return List.of(data);
    }

    @Override
    public void saveData(
        @NotNull String workspaceId,
        @NotNull String dataType,
        @NotNull String dataValue
    ) throws DBException {
        String endpoint = WORKSPACE_DATA_TYPE_ENDPOINT
            .replace(VAR_WORKSPACE_ID, workspaceId)
            .replace(VAR_DATA_TYPE, encodePathSegment(dataType));
        execute(
            request(endpoint, Map.of())
                .header(HttpConstants.HEADER_CONTENT_TYPE, MediaType.TEXT.toString())
                .PUT(HttpRequest.BodyPublishers.ofString(dataValue)),
            Void.class);
    }

    @NotNull
    private HttpRequest.Builder request(
        @NotNull String endpoint,
        @NotNull Map<String, String> parameters
    ) throws DBException {
        URI uri = buildUri(CommonUtils.removeLeadingSlash(endpoint), parameters);
        return HttpRequest.newBuilder(uri)
            .header(HttpConstants.HEADER_AUTHORIZATION, HttpConstants.BEARER_PREFIX + accessKey.buildToken());
    }

    @NotNull
    private static String encodePathSegment(@NotNull String value) {
        return value.replace("/", "%2F");
    }
}
