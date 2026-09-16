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
import com.dbeaver.datadam.share.api.utils.DDFingerprintUtils;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.datadam.sync.DDSyncChange;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSharedProjectPullResult;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSyncCredentials;
import org.jkiss.dbeaver.model.datadam.sync.core.PreparedFiles;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.util.*;

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
    public DDProjectSyncLocalBinding getBinding(@NotNull DBPProject project) throws DBException {
        DDProjectSyncLocalBinding binding = bindingStore.load(project);
        return binding != null && accountId.equals(binding.accountId()) ? binding : null;
    }

    public boolean isShared(@NotNull DBPProject project) throws DBException {
        return getBinding(project) != null;
    }

    @NotNull
    public DDProjectSyncSnapshot getProjectSyncSnapshot(@NotNull DBPProject project) throws DBException {
        DDProjectSyncLocalBinding binding = getBinding(project);
        if (binding == null) {
            throw new DBException("Project is not bound to DataDam: " + project.getName());
        }
        DDSharedProjectRevision serverRevision = getServerRevision(binding);
        Map<String, byte[]> content = getLocalContentAdapter(binding).read(project);
        Map<String, Pair<String, byte[]>> files = new LinkedHashMap<>(content.size());
        Map<String, String> fileFingerprints = new LinkedHashMap<>(content.size());
        for (Map.Entry<String, byte[]> file : content.entrySet()) {
            String fingerprint = DDFingerprintUtils.calculateFileFingerprint(
                binding.remoteProjectId(), file.getKey(), file.getValue());
            files.put(file.getKey(), new Pair<>(fingerprint, file.getValue()));
            fileFingerprints.put(file.getKey(), fingerprint);
        }
        String configurationFingerprint = DDFingerprintUtils.calculateConfigurationFingerprint(
            binding.remoteProjectId(), fileFingerprints);
        return new DDProjectSyncSnapshot(
            binding,
            serverRevision,
            new PreparedFiles(files, configurationFingerprint),
            classify(binding.lastSyncedRevision(), serverRevision, configurationFingerprint)
        );
    }

    @NotNull
    public DDSharedProjectRevision pushFiles(
        @NotNull DBPProject project,
        @NotNull DDProjectSyncLocalBinding binding,
        @NotNull PreparedFiles preparedFiles
    ) throws DBException {
        return pushFiles(
            project, binding, preparedFiles, binding.lastSyncedRevision().configurationFingerprint());
    }

    @NotNull
    public DDSharedProjectRevision forcePushFiles(
        @NotNull DBPProject project,
        @NotNull DDProjectSyncLocalBinding binding,
        @NotNull PreparedFiles preparedFiles,
        @NotNull String serverConfigurationFingerprint
    ) throws DBException {
        return pushFiles(project, binding, preparedFiles, serverConfigurationFingerprint);
    }

    @NotNull
    private DDSharedProjectRevision pushFiles(
        @NotNull DBPProject project,
        @NotNull DDProjectSyncLocalBinding binding,
        @NotNull PreparedFiles preparedFiles,
        @NotNull String lastKnownConfigurationFingerprint
    ) throws DBException {
        try {
            DDSharedProjectRevision revision = client.pushFiles(
                binding.remoteProjectId(), preparedFiles, lastKnownConfigurationFingerprint);
            saveBinding(project, binding.remoteProjectId(), revision, binding.unitIds());
            return revision;
        } catch (DDShareException e) {
            throw new DBException("Error pushing DataDam project files", e);
        }
    }

    @NotNull
    public DDSharedProjectRevision pullFiles(
        @NotNull DBPProject project,
        @NotNull DDProjectSyncLocalBinding binding,
        @NotNull DDSharedProjectRevision expectedServerRevision
    ) throws DBException {
        DDSharedProjectPullResult result = getPullResult(binding);
        if (!expectedServerRevision.configurationFingerprint().equals(
            result.currentRevision().configurationFingerprint())) {
            throw new DBException("Server project configuration has changed: " + binding.remoteProjectId());
        }
        DDProjectSyncContentAdapter content = getServerContentAdapter(result.files());
        content.write(project, result.files());
        saveBinding(project, binding.remoteProjectId(), result.currentRevision(), content.getUnitIds());
        return result.currentRevision();
    }

    @NotNull
    private DDSharedProjectPullResult getPullResult(@NotNull DDProjectSyncLocalBinding binding) throws DBException {
        try {
            return client.pullFiles(binding.remoteProjectId());
        } catch (DDShareException e) {
            throw new DBException("Error pulling DataDam project files", e);
        }
    }

    @NotNull
    private DDSharedProjectRevision getServerRevision(@NotNull DDProjectSyncLocalBinding binding) throws DBException {
        try {
            return client.getCurrentProjectRevision(binding.remoteProjectId());
        } catch (DDShareException e) {
            throw new DBException("Error reading current DataDam project revision", e);
        }
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
        try {
            DDSharedProject remote = client.createProject(remoteProjectId, project.getName(), description);
            DDSharedProjectRevision revision = client.getCurrentProjectRevision(remoteProjectId);
            saveBinding(project, remoteProjectId, revision, content.getUnitIds());
            return remote;
        } catch (Exception e) {
            throw new DBException("Error sharing project '" + project.getName() + "' with DataDam", e);
        }
    }

    @NotNull
    public DBPProject importProject(@NotNull DDSharedProject remote) throws DBException {
        requireRemoteProjectAvailable(remote);
        DBPProject project = null;
        try {
            DDSharedProjectPullResult pullResult = client.pullFiles(remote.id());
            DDProjectSyncContentAdapter content = getServerContentAdapter(pullResult.files());
            if (content.getUnitIds().isEmpty()) {
                throw new DBException("No project synchronization units are available");
            }

            project = workspace.createProject(uniqueProjectName(remote.name()), remote.description());
            content.write(project, pullResult.files());
            saveBinding(project, remote.id(), pullResult.currentRevision(), content.getUnitIds());
            return project;
        } catch (Exception e) {
            if (project != null) {
                try {
                    workspace.deleteProject(project);
                } catch (DBException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            if (e instanceof DBException dbException) {
                throw dbException;
            }
            throw new DBException("Error importing DataDam project '" + remote.name() + "'", e);
        }
    }

    private void requireUnbound(@NotNull DBPProject project) throws DBException {
        if (bindingStore.load(project) != null) {
            throw new DBException("Project '" + project.getName() + "' is already bound to DataDam");
        }
    }

    private void saveBinding(
        @NotNull DBPProject project,
        @NotNull UUID remoteProjectId,
        @NotNull DDSharedProjectRevision revision,
        @NotNull Set<String> syncUnitIds
    ) throws DBException {
        bindingStore.save(
            project,
            new DDProjectSyncLocalBinding(remoteProjectId, accountId, revision, syncUnitIds));
    }

    @NotNull
    private DDProjectSyncContentAdapter getLocalContentAdapter(
        @NotNull DDProjectSyncLocalBinding binding
    ) throws DBException {
        return DDProjectSyncContentAdapter.forUnitIds(binding.unitIds());
    }

    @NotNull
    private DDProjectSyncContentAdapter getServerContentAdapter(
        @NotNull Map<String, byte[]> files
    ) throws DBException {
        return DDProjectSyncContentAdapter.forFiles(files.keySet());
    }

    private void requireRemoteProjectAvailable(@NotNull DDSharedProject remote) throws DBException {
        if (!accountId.equals(remote.projectOwner())) {
            throw new DBException("DataDam project belongs to another account: " + remote.id());
        }
        for (DBPProject project : workspace.getProjects()) {
            DDProjectSyncLocalBinding binding = bindingStore.load(project);
            if (binding != null && accountId.equals(binding.accountId()) && remote.id().equals(binding.remoteProjectId())) {
                throw new DBException("DataDam project '" + remote.name() + "' is already in the workspace");
            }
        }
    }

    @NotNull
    private DDSyncChange classify(
        @NotNull DDSharedProjectRevision lastSyncedRevision,
        @NotNull DDSharedProjectRevision serverRevision,
        @NotNull String configurationFingerprint
    ) {
        boolean localChanged = !lastSyncedRevision.configurationFingerprint().equals(configurationFingerprint);
        boolean serverChanged = !lastSyncedRevision.configurationFingerprint().equals(serverRevision.configurationFingerprint());
        if (localChanged && serverChanged) {
            return DDSyncChange.CONFLICT;
        }
        if (localChanged) {
            return DDSyncChange.LOCAL;
        }
        return serverChanged ? DDSyncChange.SERVER : DDSyncChange.UNCHANGED;
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
