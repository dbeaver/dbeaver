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
package org.jkiss.dbeaver.model.datadam.sync.project.sync;

import com.dbeaver.datadam.share.api.exception.DDShareException;
import com.dbeaver.datadam.share.api.model.DDSharedProject;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import com.dbeaver.datadam.share.api.utils.DDFingerprintUtils;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;
import org.jkiss.dbeaver.model.datadam.sync.core.DDSharedProjectPullResult;
import org.jkiss.dbeaver.model.datadam.sync.core.PreparedFiles;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectSyncUtils;
import org.jkiss.utils.Pair;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Performs local and remote project transfers. Callers coordinate operations and persist their resulting bindings.
 */
public class DDProjectSyncService {

    private static final Log log = Log.getLog(DDProjectSyncService.class);

    private final DDShareClient client;
    private final UUID accountId;

    public DDProjectSyncService(
        @NotNull DDShareClient client,
        @NotNull UUID accountId
    ) {
        this.client = client;
        this.accountId = accountId;
    }

    @NotNull
    UUID getAccountId() {
        return accountId;
    }

    @NotNull
    public DDProjectSyncSnapshot getProjectSyncSnapshot(
        @NotNull DBPProject project,
        @NotNull DDProjectSyncLocalBinding binding
    ) throws DBException {
        log.debug("Creating sync snapshot for project '" + project.getName() + "' with local binding " + binding);
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
        DDProjectSyncSnapshot snapshot = new DDProjectSyncSnapshot(
            binding,
            serverRevision,
            new PreparedFiles(files, configurationFingerprint),
            DDProjectSyncUtils.classify(binding.lastSyncedRevision(), serverRevision, configurationFingerprint)
        );
        log.debug("Created sync snapshot for project '" + project.getName() + "' status result: " + snapshot.change());
        return snapshot;
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
            log.debug("Pushing project '" + project.getName() + "' to " + binding.remoteProjectId() +
                " with expected server fingerprint " + lastKnownConfigurationFingerprint);
            return client.pushFiles(
                binding.remoteProjectId(), preparedFiles, lastKnownConfigurationFingerprint);
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
        log.debug("Pulling project '" + project.getName() + "' from " + binding.remoteProjectId() +
            " with expected server revision " + expectedServerRevision);
        DDSharedProjectPullResult result = getPullResult(binding);
        if (!expectedServerRevision.configurationFingerprint().equals(
            result.currentRevision().configurationFingerprint())) {
            throw new DBException("Server project configuration has changed: " + binding.remoteProjectId());
        }
        DDProjectSyncContentAdapter content = getLocalContentAdapter(binding);
        content.write(project, result.files());
        return result.currentRevision();
    }

    @NotNull
    private DDSharedProjectPullResult getPullResult(@NotNull DDProjectSyncLocalBinding binding) throws DBException {
        try {
            DDSharedProjectPullResult result = client.pullFiles(binding.remoteProjectId());
            log.debug("Pulled " + result.files().size() + " files for project " + binding.remoteProjectId() +
                " at server revision " + result.currentRevision());
            return result;
        } catch (DDShareException e) {
            throw new DBException("Error pulling DataDam project files", e);
        }
    }

    @NotNull
    private DDSharedProjectRevision getServerRevision(@NotNull DDProjectSyncLocalBinding binding) throws DBException {
        try {
            DDSharedProjectRevision revision = client.getCurrentProjectRevision(binding.remoteProjectId());
            log.debug("Received server revision for project " + binding.remoteProjectId() + ": " + revision);
            return revision;
        } catch (DDShareException e) {
            throw new DBException("Error reading current DataDam project revision", e);
        }
    }

    @NotNull
    public ShareResult createRemoteProject(
        @NotNull DBPProject project, @NotNull String name, @Nullable String description
    ) throws DBException {
        log.debug("Sharing project '" + project.getName() + "' with DataDam");
        DDProjectSyncContentAdapter content = DDProjectSyncContentAdapter.forEnabledUnits();
        if (content.getUnitIds().isEmpty()) {
            throw new DBException("No project synchronization units are available");
        }

        UUID remoteProjectId = UUID.randomUUID();
        try {
            DDSharedProject remote = client.createProject(remoteProjectId, name, description);

            DDSharedProjectRevision revision = client.getCurrentProjectRevision(remoteProjectId);
            DDProjectSyncLocalBinding binding = new DDProjectSyncLocalBinding(
                remoteProjectId, accountId, revision, content.getUnitIds());
            log.debug("Shared project '" + project.getName() + "' as DataDam project " + remoteProjectId);
            return new ShareResult(remote, binding);
        } catch (Exception e) {
            throw new DBException("Error sharing project '" + project.getName() + "' with DataDam", e);
        }
    }


    @NotNull
    public DDProjectSyncLocalBinding importProject(
        @NotNull DBPProject project,
        @NotNull DDSharedProject remote
    ) throws DBException {
        log.debug("Importing DataDam project '" + remote.name() + "' (" + remote.id() + ")");
        if (!accountId.equals(remote.projectOwner())) {
            throw new DBException("DataDam project belongs to another account: " + remote.id());
        }
        try {
            DDSharedProjectPullResult pullResult = client.pullFiles(remote.id());
            DDProjectSyncContentAdapter content = getServerContentAdapter(pullResult.files());
            if (content.getUnitIds().isEmpty()) {
                throw new DBException("No project synchronization units are available");
            }

            content.write(project, pullResult.files());
            DDProjectSyncLocalBinding binding = new DDProjectSyncLocalBinding(
                remote.id(), accountId, pullResult.currentRevision(), content.getUnitIds());
            log.debug("Imported DataDam project " + remote.id() + " as local project '" + project.getName() + "'");
            return binding;
        } catch (DBException e) {
            throw e;
        } catch (Exception e) {
            throw new DBException("Error importing DataDam project '" + remote.name() + "'", e);
        }
    }

    @NotNull
    private DDProjectSyncContentAdapter getLocalContentAdapter(
        @NotNull DDProjectSyncLocalBinding binding
    ) throws DBException {
        DDProjectSyncContentAdapter content = DDProjectSyncContentAdapter.forUnitIds(binding.unitIds());
        log.debug("Resolved local content adapters for project " + binding.remoteProjectId() + ": " + content.getUnitIds());
        return content;
    }

    @NotNull
    private DDProjectSyncContentAdapter getServerContentAdapter(
        @NotNull Map<String, byte[]> files
    ) throws DBException {
        DDProjectSyncContentAdapter content = DDProjectSyncContentAdapter.forFiles(files.keySet());
        log.debug("Resolved server content adapters for " + files.size() + " files: " + content.getUnitIds());
        return content;
    }

    public record ShareResult(@NotNull DDSharedProject remoteProject, @NotNull DDProjectSyncLocalBinding binding) {
    }

}
