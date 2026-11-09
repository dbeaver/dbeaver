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
package org.jkiss.dbeaver.model.datadam.sync.project;

import com.dbeaver.datadam.share.api.exception.DDShareException;
import com.dbeaver.datadam.share.api.model.DDSharedProject;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;
import org.jkiss.dbeaver.model.datadam.sync.project.sync.DDProject;
import org.jkiss.dbeaver.model.datadam.sync.project.sync.DDProjectSyncBindingStore;
import org.jkiss.dbeaver.model.datadam.sync.project.sync.DDProjectSyncLocalBinding;
import org.jkiss.dbeaver.model.datadam.sync.project.sync.DDProjectSyncService;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DDProjectManager {
    public enum BindingStatus { UNBOUND, CURRENT_ACCOUNT, OTHER_ACCOUNT, ACCOUNT_UNKNOWN, PROJECT_REMOVED }

    @Nullable
    private static DDProjectManager instance;

    @NotNull
    private final DBPWorkspace workspace;
    @NotNull
    private final DDShareClient client;
    @NotNull
    private final DDProjectSyncService syncService;
    @NotNull
    private final DDDesktopAccessService accessService;
    private final ConcurrentMap<Path, DDProject> projects = new ConcurrentHashMap<>();

    DDProjectManager(
        @NotNull DBPWorkspace workspace,
        @NotNull DDShareClient client,
        @NotNull DDProjectSyncService syncService,
        @NotNull DDDesktopAccessService accessService
    ) {
        this.workspace = workspace;
        this.client = client;
        this.syncService = syncService;
        this.accessService = accessService;
    }

    @NotNull
    public static synchronized DDProjectManager getInstance() {
        if (instance == null) {
            DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
            DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
            DDShareClient client = new DDShareClient(DDEndpoints.getStorageBaseUrl(), accessService);
            DDProjectSyncService syncService = new DDProjectSyncService(client);
            instance = new DDProjectManager(workspace, client, syncService, accessService);
            instance.loadProjects();
        }
        return instance;
    }

    @NotNull
    public DDSharedProject shareProject(
        @NotNull DBPProject project, @NotNull String name, @Nullable String description
    ) throws DBException {
        requireAccountId();
        return getOrCreate(project).share(project, name, description);
    }

    @NotNull
    public DDSharedProject updateProject(
        @NotNull DDSharedProject project, @NotNull String name, @Nullable String description
    ) throws DBException {
        try {
            return client.updateProject(project.id(), name, description);
        } catch (DDShareException e) {
            throw new DBException("Error updating DataDam project '" + project.name() + "'", e);
        }
    }

    private void loadProjects() {
        for (DBPProject project : workspace.getProjects()) {
            getOrCreate(project);
        }
    }

    @NotNull
    public List<LocalProjectInfo> getLocalProjects() throws DBException {
        List<LocalProjectInfo> result = new ArrayList<>();
        for (DBPProject project : workspace.getProjects()) {
            result.add(getLocalProject(project));
        }
        return result;
    }

    @NotNull
    public LocalProjectInfo getLocalProject(@NotNull DBPProject project) throws DBException {
        DDProjectSyncLocalBinding binding = getAccountBinding(project);
        return new LocalProjectInfo(
            project,
            binding == null ? null : binding.remoteProjectId(),
            binding == null ? null : binding.lastSyncedRevision().updateTime()
        );
    }

    @NotNull
    public BindingStatus getBindingStatus(@NotNull DBPProject project) throws DBException {
        if (project.getWorkspace().getProject(project.getName()) != project) {
            return BindingStatus.PROJECT_REMOVED;
        }
        DDProjectSyncLocalBinding binding;
        try {
            binding = getOrCreate(project).getBinding(project);
        } catch (IllegalStateException e) {
            if (project.getWorkspace().getProject(project.getName()) != project) {
                return BindingStatus.PROJECT_REMOVED;
            }
            throw e;
        }
        if (binding == null) {
            return BindingStatus.UNBOUND;
        }
        if (accessService.getStatus() != DDDesktopAccessService.Status.SIGNED_IN) {
            return BindingStatus.ACCOUNT_UNKNOWN;
        }
        DDKeyBundle bundle = accessService.getKeyBundle();
        if (bundle == null) {
            return BindingStatus.ACCOUNT_UNKNOWN;
        }
        return binding.accountId().equals(bundle.accountId())
            ? BindingStatus.CURRENT_ACCOUNT : BindingStatus.OTHER_ACCOUNT;
    }

    /**
     * Calculates the current sync status by fetching the server revision and reading
     * and fingerprinting local project files. This operation may be expensive.
     *
     * @param project the local project to inspect
     * @return the current sync status, or {@link ProjectStatus#NOT_SYNC} if the project is not bound
     * @throws DBException if the project cannot be inspected
     */
    @NotNull
    public ProjectStatus calculateSyncStatus(@NotNull DBPProject project) throws DBException {
        DDProjectSyncLocalBinding binding = getAccountBinding(project);
        return binding == null ? ProjectStatus.NOT_SYNC : getOrCreate(project).calculateSyncStatus(project);
    }

    @NotNull
    public SyncResult synchronizeProject(@NotNull DBPProject project) throws DBException {
        DDProjectSyncLocalBinding binding = getAccountBinding(project);
        return new SyncResult(project, binding == null ? ProjectStatus.NOT_SYNC : getOrCreate(project).synchronize(project));
    }

    public void unbindProject(@NotNull DBPProject project) throws DBException {
        UUID accountId = requireAccountId();
        DDProjectSyncLocalBinding binding = getOrCreate(project).getBinding(project);
        if (binding != null && !binding.accountId().equals(accountId)) {
            throw new DBException("Project is bound to another DataDam account: " + project.getName());
        }
        getOrCreate(project).unbind(project);
    }

    @NotNull
    public ProjectStatus resolveConflict(
        @NotNull DBPProject project,
        @NotNull ConflictResolution resolution
    ) throws DBException {
        DDProjectSyncLocalBinding binding = getAccountBinding(project);
        return binding == null ? ProjectStatus.NOT_SYNC : getOrCreate(project).resolveConflict(project, resolution);
    }

    @NotNull
    public List<DDSharedProject> getRemoteProjects() throws DBException {
        try {
            return client.listProjects();
        } catch (DDShareException e) {
            throw new DBException("Error listing DataDam projects", e);
        }
    }

    @NotNull
    public ProjectRevisions getProjectRevisions(
        @NotNull DBPProject project, @Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime
    ) throws DBException {
        DDProjectSyncLocalBinding binding = getAccountBinding(project);
        if (binding == null) {
            throw new DBException("Project is not bound to DataDam: " + project.getName());
        }
        try {
            List<DDSharedProjectRevision> revisions = client.getProjectRevisions(
                binding.remoteProjectId(), startTime, endTime);
            return new ProjectRevisions(binding.lastSyncedRevision(), revisions);
        } catch (DDShareException e) {
            throw new DBException("Error listing DataDam project revisions for '" + project.getName() + "'", e);
        }
    }

    @NotNull
    public synchronized DBPProject importProject(@NotNull DDSharedProject remote) throws DBException {
        if (!requireAccountId().equals(remote.projectOwner())) {
            throw new DBException("DataDam project belongs to another account: " + remote.id());
        }
        for (DBPProject project : workspace.getProjects()) {
            DDProjectSyncLocalBinding binding = getAccountBinding(project);
            if (binding != null && binding.remoteProjectId().equals(remote.id())) {
                throw new DBException("DataDam project is already in the workspace: " + remote.name());
            }
        }

        DBPProject project = workspace.createProject(uniqueProjectName(remote.name()), remote.description());

        try {
            getOrCreate(project).importProject(project, remote);
            return project;
        } catch (DBException e) {
            rollbackImport(project, e);
            throw e;
        } catch (Exception e) {
            rollbackImport(project, e);
            throw new DBException("Error importing DataDam project '" + remote.name() + "'", e);
        }
    }

    @NotNull
    private DDProject getOrCreate(@NotNull DBPProject project) {
        Path bindingFile = DDProjectSyncBindingStore.getBindingFile(project);
        return projects.computeIfAbsent(bindingFile, path -> new DDProject(path, syncService));
    }

    @Nullable
    private DDProjectSyncLocalBinding getAccountBinding(@NotNull DBPProject project) throws DBException {
        UUID accountId = requireAccountId();
        DDProjectSyncLocalBinding binding = getOrCreate(project).getBinding(project);
        return binding != null && binding.accountId().equals(accountId) ? binding : null;
    }

    @NotNull
    private UUID requireAccountId() throws DBException {
        DDKeyBundle bundle = accessService.getKeyBundle();
        if (bundle == null) {
            throw new DBException("Import encryption keys for the current DataDam account in Preferences > DataDam > Projects");
        }
        return bundle.accountId();
    }

    private void rollbackImport(@NotNull DBPProject project, @NotNull Exception cause) {
        try {
            workspace.deleteProject(project);
        } catch (DBException e) {
            cause.addSuppressed(e);
        }
    }

    @NotNull
    private String uniqueProjectName(@Nullable String remoteName) {
        String base = CommonUtils.isEmpty(remoteName) ? "DataDam Project" : remoteName;
        String name = base;
        for (int suffix = 2; workspace.getProject(name) != null; suffix++) {
            name = base + " (" + suffix + ")";
        }
        return name;
    }

    public record SyncResult(@NotNull DBPProject project, @NotNull ProjectStatus status) {
    }

    public record ProjectRevisions(
        @NotNull DDSharedProjectRevision lastSyncedRevision,
        @NotNull List<DDSharedProjectRevision> revisions
    ) {
        public ProjectRevisions {
            revisions = List.copyOf(revisions);
        }
    }

    public record LocalProjectInfo(
        @NotNull DBPProject project,
        @Nullable UUID remoteProjectId,
        @Nullable LocalDateTime lastSyncedRevision
    ) {
    }
}
