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
package org.jkiss.dbeaver.ui.datadam.project;

import com.dbeaver.datadam.share.api.model.DDSharedProject;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.sync.DDSyncChange;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectSyncLocalBinding;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectSyncService;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.*;

/**
 * Project-level operations and the latest explicitly refreshed list for the current DataDam account.
 */
public class DDProjectCatalog {
    private static final DDProjectCatalog INSTANCE = new DDProjectCatalog();

    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
    @Nullable
    private DDProjectSyncService service;
    @NotNull
    private List<DDProjectView> projects = List.of();

    private DDProjectCatalog() {
        accessService.addKeyBundleListener(this::clear);
    }

    @NotNull
    public static DDProjectCatalog getInstance() {
        return INSTANCE;
    }

    public boolean isDDEnabled() {
        return accessService.getKeyBundle() != null;
    }

    public boolean isShared(@NotNull DBPProject project) throws DBException {
        return getService().isShared(project);
    }

    @NotNull
    public List<DDProjectView> getProjects() {
        return projects;
    }

    public void refreshProjects(@NotNull DBRProgressMonitor monitor) throws DBException {
        DDProjectSyncService sync = getService();
        Map<UUID, DBPProject> local = new HashMap<>();
        Map<UUID, DDProjectSyncLocalBinding> bindings = new HashMap<>();
        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            DDProjectSyncLocalBinding binding = sync.getBinding(project);
            if (binding != null) {
                local.put(binding.remoteProjectId(), project);
                bindings.put(binding.remoteProjectId(), binding);
            }
        }
        List<DDProjectView> refreshed = new ArrayList<>();
        for (DDSharedProject remote : sync.listAllRemoteProjects()) {
            if (monitor.isCanceled()) {
                return;
            }
            DBPProject project = local.remove(remote.id());
            DDProjectSyncLocalBinding binding = bindings.remove(remote.id());
            DDProjectView.Status status = project == null ? DDProjectView.Status.NOT_ON_DEVICE
                : status(sync.getProjectSyncSnapshot(project).change());
            String name = project != null && !project.getName().equals(remote.name())
                ? project.getName() + " (" + remote.name() + ")" : remote.name();
            refreshed.add(new DDProjectView(
                remote.id(), name, remote.description(), project, remote,
                binding == null ? null : binding.lastSyncedRevision().updateTime(), status
            ));
        }
        for (Map.Entry<UUID, DBPProject> missing : local.entrySet()) {
            DDProjectSyncLocalBinding binding = bindings.get(missing.getKey());
            DBPProject project = missing.getValue();
            refreshed.add(new DDProjectView(
                missing.getKey(), project.getName(), project.getDescription(), project, null,
                binding.lastSyncedRevision().updateTime(), DDProjectView.Status.MISSING_ON_SERVER
            ));
        }
        if (!monitor.isCanceled()) {
            projects = List.copyOf(refreshed);
        }
    }

    @NotNull
    public List<DBPProject> getNotDatadamLocalProjects() throws DBException {
        DDProjectSyncService sync = getService();
        List<DBPProject> local = new ArrayList<>();
        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            if (!sync.isShared(project)) {
                local.add(project);
            }
        }
        return local;
    }

    public void shareProject(@NotNull DBPProject project, @Nullable String description) throws DBException {
        DDProjectSyncService sync = getService();
        sync.shareProject(project, description);
        DDProjectSyncPropertyTester.firePropertyChange();
        var snapshot = sync.getProjectSyncSnapshot(project);
        sync.pushFiles(project, snapshot.binding(), snapshot.preparedFiles());
    }

    @NotNull
    public DBPProject importProject(@NotNull DDProjectView view) throws DBException {
        if (view.remoteProject() == null) {
            throw new DBException("Project is not available on DataDam: " + view.name());
        }
        DBPProject imported = getService().importProject(view.remoteProject());
        DDProjectSyncPropertyTester.firePropertyChange();
        return imported;
    }

    public void unbindProject(@NotNull DBPProject project) throws DBException {
        getService().unbindProject(project);
        DDProjectSyncPropertyTester.firePropertyChange();
    }

    @NotNull
    public SyncResult synchronizeProject(@NotNull DBPProject project) {
        DDSyncChange change = null;
        try {
            DDProjectSyncService sync = getService();
            var snapshot = sync.getProjectSyncSnapshot(project);
            change = snapshot.change();
            switch (change) {
                case UNCHANGED, CONFLICT -> {
                }
                case LOCAL -> sync.pushFiles(project, snapshot.binding(), snapshot.preparedFiles());
                case SERVER -> sync.pullFiles(project, snapshot.binding(), snapshot.serverRevision());
            }
            return new SyncResult(project, change, null);
        } catch (DBException e) {
            return new SyncResult(project, change, e);
        }
    }

    @NotNull
    private DDProjectSyncService getService() throws DBException {
        if (service == null) {
            DDKeyBundle bundle = accessService.getKeyBundle();
            if (bundle == null) {
                throw new DBException("Import encryption keys for the current DataDam account in Preferences > DataDam");
            }
            service = new DDProjectSyncService(
                DDEndpoints.getStorageBaseUrl(),
                new DDBundleCredentials(bundle),
                DBWorkbench.getPlatform().getWorkspace(),
                bundle.accountId()
            );
        }
        return service;
    }

    @NotNull
    private DDProjectView.Status status(@NotNull DDSyncChange change) {
        return switch (change) {
            case UNCHANGED -> DDProjectView.Status.UP_TO_DATE;
            case LOCAL -> DDProjectView.Status.LOCAL_CHANGES;
            case SERVER -> DDProjectView.Status.SERVER_CHANGES;
            case CONFLICT -> DDProjectView.Status.CONFLICT;
        };
    }

    private void clear() {
        service = null;
        projects = List.of();
        DDProjectSyncPropertyTester.firePropertyChange();
    }

    public record SyncResult(@NotNull DBPProject project, @Nullable DDSyncChange change, @Nullable DBException error) {
    }
}
