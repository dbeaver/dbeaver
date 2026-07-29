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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.util.List;

/**
 * Workspace synchronization endpoints.
 */
public interface DDWorkspaceService {

    String DATA_KEY_ENDPOINT = "/data/key";
    String WORKSPACE_ENDPOINT = "/workspace";
    String WORKSPACE_DATA_ENDPOINT = "/workspace/{workspaceId}/data";
    String WORKSPACE_DATA_TYPE_ENDPOINT = "/workspace/{workspaceId}/data/{dataType}";

    String PARAM_LABEL = "label";
    String VAR_WORKSPACE_ID = "{workspaceId}";
    String VAR_DATA_TYPE = "{dataType}";

    @NotNull
    byte[] loadDataKey() throws DBException;

    @NotNull
    DDWorkspace createWorkspace(@NotNull String label) throws DBException;

    @NotNull
    List<DDWorkspace> listWorkspaces() throws DBException;

    @NotNull
    List<DDWorkspaceData> loadData(@NotNull String workspaceId) throws DBException;

    void saveData(
        @NotNull String workspaceId,
        @NotNull String dataType,
        @NotNull String dataValue
    ) throws DBException;
}
