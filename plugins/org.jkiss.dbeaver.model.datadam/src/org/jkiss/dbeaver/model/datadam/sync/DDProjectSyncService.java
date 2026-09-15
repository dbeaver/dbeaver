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
package org.jkiss.dbeaver.model.datadam.sync;

import com.dbeaver.datadam.share.api.exception.DDShareException;
import com.dbeaver.datadam.share.api.model.DDSharedProject;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSharedProjectPullResult;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSyncCredentials;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages associations between local DBeaver projects and individual DataDam projects.
 */
public class DDProjectSyncService {

    private final DDShareClient client;
    private final DBPWorkspace workspace;
    private final UUID accountId;
    private final DDProjectSyncBindingStore bindingStore = new DDProjectSyncBindingStore();

    public DDProjectSyncService(
        @NotNull String url,
        @NotNull DDSyncCredentials credentials,
        @NotNull DBPWorkspace workspace,
        @NotNull UUID accountId
    ) {
        this.client = new DDShareClient(url, credentials);
        this.workspace = workspace;
        this.accountId = accountId;
    }

    @NotNull
    public List<DDSharedProject> listRemoteProjects() throws DBException {
        try {
            return client.listProjects();
        } catch (DDShareException e) {
            throw new DBException("Error listing DataDam projects", e);
        }
    }

    @Nullable
    public DDProjectSyncBinding getBinding(@NotNull DBPProject project) throws DBException {
        DDProjectSyncBinding binding = bindingStore.load(project);
        return binding != null && accountId.equals(binding.accountId()) ? binding : null;
    }

    public boolean isShared(@NotNull DBPProject project) throws DBException {
        return getBinding(project) != null;
    }

    @NotNull
    public List<DBPProject> getSharedProjects() throws DBException {
        List<DBPProject> projects = new ArrayList<>();
        for (DBPProject project : workspace.getProjects()) {
            if (isShared(project)) {
                projects.add(project);
            }
        }
        return projects;
    }

    @NotNull
    public DDSharedProject shareProject(@NotNull DBPProject project, @Nullable String description) throws DBException {
        requireUnbound(project);
        DDProjectSyncContentAdapter content = DDProjectSyncContentAdapter.forEnabledUnits();
        if (content.getUnitIds().isEmpty()) {
            throw new DBException("No project synchronization units are available");
        }

        UUID remoteProjectId = UUID.randomUUID();
        boolean remoteCreated = false;
        try {
            Map<String, byte[]> files = content.read(project);
            DDSharedProject remote = client.createProject(remoteProjectId, project.getName(), description);
            remoteCreated = true;
            DDSharedProjectPullResult initial = client.pullFiles(remoteProjectId);
            String fingerprint = initial.configurationFingerprint();
            if (!files.isEmpty()) {
                DDSharedProjectRevision revision = client.pushFiles(remoteProjectId, files, fingerprint);
                fingerprint = revision.configurationFingerprint();
            }
            bindingStore.save(
                project, new DDProjectSyncBinding(
                    remoteProjectId, accountId, fingerprint, content.getUnitIds())
            );
            return remote;
        } catch (Exception e) {
            if (remoteCreated) {
                try {
                    client.deleteProject(remoteProjectId);
                } catch (DDShareException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            if (e instanceof DBException exception) {
                throw exception;
            }
            throw new DBException("Error sharing project '" + project.getName() + "' with DataDam", e);
        }
    }

    @NotNull
    public DBPProject importProject(@NotNull DDSharedProject remote) throws DBException {
        requireRemoteProjectAvailable(remote);

        DBPProject project = null;
        try {
            DDSharedProjectPullResult pullResult = client.pullFiles(remote.id());
            DDProjectSyncContentAdapter content = DDProjectSyncContentAdapter.forFiles(pullResult.files().keySet());
            if (content.getUnitIds().isEmpty()) {
                throw new DBException("No project synchronization units are available");
            }

            project = workspace.createProject(uniqueProjectName(remote.name()), remote.description());
            content.write(project, pullResult.files());
            bindingStore.save(
                project, new DDProjectSyncBinding(
                    remote.id(), accountId, pullResult.configurationFingerprint(), content.getUnitIds())
            );
            return project;
        } catch (Exception e) {
            if (project != null) {
                try {
                    workspace.deleteProject(project);
                } catch (DBException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            if (e instanceof DBException exception) {
                throw exception;
            }
            throw new DBException("Error importing DataDam project '" + remote.name() + "'", e);
        }
    }

    private void requireUnbound(@NotNull DBPProject project) throws DBException {
        if (bindingStore.load(project) != null) {
            throw new DBException("Project '" + project.getName() + "' is already bound to DataDam");
        }
    }

    private void requireRemoteProjectAvailable(@NotNull DDSharedProject remote) throws DBException {
        if (!accountId.equals(remote.projectOwner())) {
            throw new DBException("DataDam project belongs to another account: " + remote.id());
        }
        for (DBPProject project : workspace.getProjects()) {
            DDProjectSyncBinding binding = bindingStore.load(project);
            if (binding != null && accountId.equals(binding.accountId()) && remote.id().equals(binding.remoteProjectId())) {
                throw new DBException("DataDam project '" + remote.name() + "' is already in the workspace");
            }
        }
    }

    @NotNull
    private String uniqueProjectName(@Nullable String remoteName) {
        String baseName = CommonUtils.isEmpty(remoteName) ? "DataDam Project" : remoteName;
        String name = baseName;
        for (int suffix = 2; workspace.getProject(name) != null; suffix++) {
            name = baseName + " (" + suffix + ")";
        }
        return name;
    }
}
