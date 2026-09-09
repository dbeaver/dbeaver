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
package org.jkiss.dbeaver.model.tracking.sync;

import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.tracking.sync.core.DDProjectInfo;
import org.jkiss.dbeaver.model.tracking.sync.core.DDProjectPullResult;
import org.jkiss.dbeaver.model.tracking.sync.core.DDProjectSyncStore;
import org.jkiss.dbeaver.model.tracking.sync.core.DDSyncCredentials;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Standalone project sharing: separate identity and lifecycle from Configuration sync
 * (DDSyncService). A project keeps working locally regardless of sync state - these guards
 * only gate pull/push, not general use of the project. Name, description and file contents
 * are encrypted (DDProjectSyncStore) - nothing is ever sent to the server in the clear except
 * the code-controlled file names.
 */
public class DDProjectSyncService {

    public static final String PROP_SHARED_PROJECT_OWNER = "datadam.shared-project-owner-account-id";

    private final DDProjectSyncStore store;
    private final String accountId;

    public DDProjectSyncService(@NotNull String url, @NotNull DDSyncCredentials credentials, @NotNull String accountId) {
        this.store = new DDProjectSyncStore(url, credentials);
        this.accountId = accountId;
    }

    @NotNull
    public List<DDProjectInfo> listProjects() throws DBException {
        return store.listProjects();
    }

    @NotNull
    public DDProjectInfo share(@NotNull DBPProject project, @Nullable String description) throws DBException {
        String projectId = DDSyncService.getProjectId(project);
        DDProjectInfo created = store.createProject(projectId, project.getName(), description);
        bind(project, created);
        return created;
    }

    public void bind(@NotNull DBPProject project, @NotNull DDProjectInfo remote) {
        project.setProjectProperty(PROP_SHARED_PROJECT_OWNER, remote.ownerAccountId());
    }

    @Nullable
    public String getSharedProjectId(@NotNull DBPProject project) {
        if (project.getProjectProperty(PROP_SHARED_PROJECT_OWNER) == null) {
            return null;
        }
        return DDSyncService.getProjectId(project);
    }

    /**
     * True if this project was shared by the currently logged-in account. A project shared by
     * a different account (e.g. the local folder survived a user switch) is not touched.
     */
    public boolean isSyncAllowed(@NotNull DBPProject project) {
        String owner = CommonUtils.toString(project.getProjectProperty(PROP_SHARED_PROJECT_OWNER), null);
        return owner == null || owner.equals(accountId);
    }

    @Nullable
    public DDProjectPullResult pull(@NotNull DBPProject project) throws DBException {
        String projectId = requireSyncable(project);
        return store.pullFiles(projectId);
    }

    /**
     * lastKnownConfigurationFingerprint is the whole-project fingerprint last observed by the
     * caller (from a prior pull/push) - the push is all-or-nothing: it either replaces every
     * file atomically, or is rejected outright if that fingerprint is stale.
     */
    @Nullable
    public DDSharedProjectRevision push(
        @NotNull DBPProject project,
        @NotNull Map<String, byte[]> files,
        @NotNull String lastKnownConfigurationFingerprint
    ) throws DBException {
        String projectId = requireSyncable(project);
        return store.pushFiles(projectId, files, lastKnownConfigurationFingerprint);
    }

    @Nullable
    public DDProjectInfo update(
        @NotNull DBPProject project,
        @NotNull String name,
        @Nullable String description
    ) throws DBException {
        String projectId = requireSyncable(project);
        return store.updateProject(projectId, name, description);
    }

    public boolean unshare(@NotNull DBPProject project) throws DBException {
        String projectId = requireSyncable(project);
        boolean deleted = store.deleteProject(projectId);
        if (deleted) {
            project.setProjectProperty(PROP_SHARED_PROJECT_OWNER, null);
        }
        return deleted;
    }

    @NotNull
    private String requireSyncable(@NotNull DBPProject project) throws DBException {
        String projectId = getSharedProjectId(project);
        if (projectId == null) {
            throw new DBException("Project '" + project.getName() + "' is not shared");
        }
        if (!isSyncAllowed(project)) {
            throw new DBException(
                "Project '" + project.getName() + "' was shared by a different account, sync is disabled for it");
        }
        return projectId;
    }
}
