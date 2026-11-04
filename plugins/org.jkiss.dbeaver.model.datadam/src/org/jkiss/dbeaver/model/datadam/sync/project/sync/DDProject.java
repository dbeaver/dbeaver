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

import com.dbeaver.datadam.share.api.model.DDSharedProject;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.DDSyncChange;
import org.jkiss.dbeaver.model.datadam.sync.project.ConflictResolution;
import org.jkiss.dbeaver.model.datadam.sync.project.ProjectStatus;

import java.nio.file.Path;

/**
 * DataDam operations for one local binding-file location, bound or unbound.
 * Operations on this instance are serialized, including snapshot preparation and binding persistence.
 * The manager must maintain one instance per binding-file location; external writers are not coordinated here.
 */
public class DDProject {
    @NotNull
    private final Path bindingFile;
    @NotNull
    private final DDProjectSyncBindingStore bindingStore;
    @NotNull
    private final DDProjectSyncService syncService;

    public DDProject(
        @NotNull Path bindingFile,
        @NotNull DDProjectSyncService syncService
    ) {
        this.bindingFile = bindingFile.toAbsolutePath().normalize();
        this.bindingStore = new DDProjectSyncBindingStore(this.bindingFile);
        this.syncService = syncService;
    }

    @Nullable
    public synchronized DDProjectSyncLocalBinding getBinding(@NotNull DBPProject project) throws DBException {
        validateProject(project);
        DDProjectSyncLocalBinding binding = bindingStore.load();
        return belongsToAnotherAccount(binding) ? null : binding;
    }

    @NotNull
    public synchronized ProjectStatus calculateSyncStatus(@NotNull DBPProject project) throws DBException {
        DDProjectSyncSnapshot snapshot = prepareSnapshot(project);
        return snapshot == null ? ProjectStatus.NOT_SYNC : status(snapshot.change());
    }

    @NotNull
    public synchronized ProjectStatus synchronize(@NotNull DBPProject project) throws DBException {
        return synchronize(project, prepareSnapshot(project));
    }

    @NotNull
    private ProjectStatus synchronize(
        @NotNull DBPProject project,
        @Nullable DDProjectSyncSnapshot snapshot
    ) throws DBException {
        if (snapshot == null) {
            return ProjectStatus.NOT_SYNC;
        }
        return switch (snapshot.change()) {
            case CONFLICT -> ProjectStatus.CONFLICT;
            case UNCHANGED -> ProjectStatus.UP_TO_DATE;
            case LOCAL -> transfer(
                snapshot,
                state -> syncService.pushFiles(project, state.binding(), state.preparedFiles())
            );
            case SERVER -> transfer(
                snapshot,
                state -> syncService.pullFiles(project, state.binding(), state.serverRevision())
            );
        };
    }

    @NotNull
    public synchronized DDSharedProject share(
        @NotNull DBPProject project, @NotNull String name, @Nullable String description
    ) throws DBException {
        requireUnbound(project);
        DDProjectSyncService.ShareResult result = syncService.createRemoteProject(project, name, description);
        bindingStore.save(result.binding());
        // Persist the new association before uploading, so a failed upload can be retried without sharing again.
        synchronize(project);
        return result.remoteProject();
    }

    public synchronized void importProject(@NotNull DBPProject project, @NotNull DDSharedProject remote) throws DBException {
        requireUnbound(project);
        DDProjectSyncLocalBinding newBinding = syncService.importProject(project, remote);
        bindingStore.save(newBinding);
    }

    public synchronized void unbind(@NotNull DBPProject project) throws DBException {
        validateProject(project);
        DDProjectSyncLocalBinding binding = bindingStore.load();
        if (belongsToAnotherAccount(binding)) {
            throw new DBException("Project is bound to another DataDam account: " + project.getName());
        }
        bindingStore.delete();
    }

    @NotNull
    public synchronized ProjectStatus resolveConflict(
        @NotNull DBPProject project,
        @NotNull ConflictResolution resolution
    ) throws DBException {
        DDProjectSyncSnapshot snapshot = prepareSnapshot(project);
        if (snapshot == null || snapshot.change() != DDSyncChange.CONFLICT) {
            return synchronize(project, snapshot);
        }
        return switch (resolution) {
            case USE_LOCAL_VERSION -> transfer(snapshot, state -> syncService.forcePushFiles(
                project, state.binding(), state.preparedFiles(), state.serverRevision().configurationFingerprint()));
            case USE_SERVER_VERSION -> transfer(snapshot,
                state -> syncService.pullFiles(project, state.binding(), state.serverRevision()));
        };
    }

    @NotNull
    private ProjectStatus transfer(
        @NotNull DDProjectSyncSnapshot snapshot,
        @NotNull SnapshotTransfer operation
    ) throws DBException {
        DDProjectSyncLocalBinding current = snapshot.binding();
        DDSharedProjectRevision revision = operation.apply(snapshot);
        bindingStore.save(new DDProjectSyncLocalBinding(
            current.remoteProjectId(), current.accountId(), revision, current.unitIds()));
        return ProjectStatus.UP_TO_DATE;
    }

    @Nullable
    private DDProjectSyncSnapshot prepareSnapshot(@NotNull DBPProject project) throws DBException {
        DDProjectSyncLocalBinding current = getBinding(project);
        if (current == null) {
            return null;
        }
        return syncService.getProjectSyncSnapshot(project, current);
    }

    private void requireUnbound(@NotNull DBPProject project) throws DBException {
        validateProject(project);
        if (bindingStore.load() != null) {
            throw new DBException("Project is already bound to DataDam: " + project.getName());
        }
    }

    private void validateProject(@NotNull DBPProject project) throws DBException {
        if (!bindingFile.equals(DDProjectSyncBindingStore.getBindingFile(project))) {
            throw new DBException("Project binding location does not match this DataDam project: " + project.getName());
        }
    }

    private boolean belongsToAnotherAccount(@Nullable DDProjectSyncLocalBinding binding) {
        return binding != null && !binding.accountId().equals(syncService.getAccountId());
    }

    @NotNull
    private ProjectStatus status(@NotNull DDSyncChange change) {
        return switch (change) {
            case UNCHANGED -> ProjectStatus.UP_TO_DATE;
            case LOCAL -> ProjectStatus.LOCAL_CHANGES;
            case SERVER -> ProjectStatus.SERVER_CHANGES;
            case CONFLICT -> ProjectStatus.CONFLICT;
        };
    }

    @FunctionalInterface
    private interface SnapshotTransfer {
        @NotNull
        DDSharedProjectRevision apply(@NotNull DDProjectSyncSnapshot snapshot) throws DBException;
    }

}
