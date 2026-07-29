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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.utils.CommonUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DDSyncService {

    private static final Log log = Log.getLog(DDSyncService.class);

    public static final String BINDING_FILE = ".synchronize";
    public static final String PROP_PROJECT_ID = "datadam.project-id";

    private static final String TYPE_SEPARATOR = "/";

    private final DDSyncClient client;
    private final DDAccessKey accessKey;
    private final DBPWorkspace workspace;

    public DDSyncService(
        @NotNull String url,
        @NotNull DDAccessKey accessKey,
        @NotNull DBPWorkspace workspace
    ) {
        this.client = new DDSyncClient(url, accessKey);
        this.accessKey = accessKey;
        this.workspace = workspace;
    }

    @NotNull
    public List<DDWorkspace> listWorkspaces() throws DBException {
        return client.listWorkspaces();
    }

    @Nullable
    public String getBoundWorkspaceId() {
        return readBinding(workspace.getAbsolutePath());
    }

    @Nullable
    public static String readBinding(@NotNull Path workspacePath) {
        Path bindingFile = workspacePath.resolve(BINDING_FILE);
        try {
            return Files.exists(bindingFile) ? Files.readString(bindingFile).trim() : null;
        } catch (IOException e) {
            log.debug("Error reading synchronization binding", e);
            return null;
        }
    }

    public void bindWorkspace(@NotNull String workspaceId) throws DBException {
        try {
            Files.writeString(workspace.getAbsolutePath().resolve(BINDING_FILE), workspaceId);
        } catch (IOException e) {
            throw new DBException("Error writing synchronization binding", e);
        }
    }

    @NotNull
    public String createWorkspace(@NotNull String label) throws DBException {
        DDWorkspace created = client.createWorkspace(label);
        bindWorkspace(created.workspaceId());
        return created.workspaceId();
    }

    @NotNull
    public List<String> upload(@NotNull String workspaceId) throws DBException {
        SecretKey dataKey = loadDataKey();
        List<String> uploaded = new ArrayList<>();
        for (DDSyncConfigType type : DDSyncConfigType.values()) {
            if (type.getScope() == DDSyncScope.WORKSPACE) {
                if (uploadFile(workspaceId, dataKey, type.getId(), workspace.getAbsolutePath().resolve(type.getPath()), null)) {
                    uploaded.add(type.getId());
                }
            } else {
                for (DBPProject project : workspace.getProjects()) {
                    String dataType = getProjectId(project) + TYPE_SEPARATOR + type.getId();
                    Path file = project.getAbsolutePath().resolve(type.getPath());
                    if (uploadFile(workspaceId, dataKey, dataType, file, project.getName())) {
                        uploaded.add(project.getName() + TYPE_SEPARATOR + type.getId());
                    }
                }
            }
        }
        return uploaded;
    }

    @NotNull
    public List<String> download(@NotNull String workspaceId) throws DBException {
        SecretKey dataKey = loadDataKey();
        List<String> restored = new ArrayList<>();
        for (DDWorkspaceData data : client.loadData(workspaceId)) {
            String dataType = data.dataType();
            int separator = dataType.indexOf(TYPE_SEPARATOR);
            DDSyncConfigType type = DDSyncConfigType.findById(
                separator < 0 ? dataType : dataType.substring(separator + 1));
            if (type == null) {
                log.debug("Skip unknown synchronization data type: " + dataType);
                continue;
            }
            DDSyncEnvelope envelope = JSONUtils.GSON.fromJson(
                new String(DDSyncCrypto.decrypt(dataKey, data.dataValue()), StandardCharsets.UTF_8),
                DDSyncEnvelope.class);
            Path target = type.getScope() == DDSyncScope.WORKSPACE
                ? workspace.getAbsolutePath().resolve(type.getPath())
                : resolveProject(dataType.substring(0, Math.max(separator, 0)), envelope.project())
                    .getAbsolutePath().resolve(type.getPath());
            writeFile(target, envelope.content());
            restored.add(dataType);
        }
        return restored;
    }

    private boolean uploadFile(
        @NotNull String workspaceId,
        @NotNull SecretKey dataKey,
        @NotNull String dataType,
        @NotNull Path file,
        @Nullable String projectName
    ) throws DBException {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            DDSyncEnvelope envelope = new DDSyncEnvelope(projectName, Files.readString(file));
            byte[] content = JSONUtils.GSON.toJson(envelope).getBytes(StandardCharsets.UTF_8);
            client.saveData(workspaceId, dataType, DDSyncCrypto.encrypt(dataKey, content));
            return true;
        } catch (IOException e) {
            throw new DBException("Error reading " + file, e);
        }
    }

    private static void writeFile(@NotNull Path file, @NotNull String content) throws DBException {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new DBException("Error writing " + file, e);
        }
    }

    @NotNull
    private DBPProject resolveProject(@NotNull String projectId, @Nullable String projectName) throws DBException {
        for (DBPProject project : workspace.getProjects()) {
            if (projectId.equals(CommonUtils.toString(project.getProjectProperty(PROP_PROJECT_ID), null))) {
                return project;
            }
        }
        String name = CommonUtils.isEmpty(projectName) ? projectId : projectName;
        if (workspace.getProject(name) != null) {
            name = name + " (" + projectId + ")";
        }
        DBPProject created = workspace.createProject(name, null);
        created.setProjectProperty(PROP_PROJECT_ID, projectId);
        return created;
    }

    @NotNull
    private static String getProjectId(@NotNull DBPProject project) {
        String projectId = CommonUtils.toString(project.getProjectProperty(PROP_PROJECT_ID), null);
        if (CommonUtils.isEmpty(projectId)) {
            projectId = UUID.randomUUID().toString();
            project.setProjectProperty(PROP_PROJECT_ID, projectId);
        }
        return projectId;
    }

    @NotNull
    private SecretKey loadDataKey() throws DBException {
        return accessKey.decryptDataKey(client.loadDataKey());
    }
}
