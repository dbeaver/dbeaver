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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.sync.project.ConflictResolution;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.LocalProjectInfo;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.ProjectRevisions;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.SyncResult;
import org.jkiss.dbeaver.model.datadam.sync.project.ProjectStatus;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DDDesktopTokenUIManager;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectConflictDialog.Choice;
import org.jkiss.dbeaver.ui.datadam.project.job.*;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.*;

public class DDProjectUIManager {
    private static final int VIEW_LOAD_THREADS = 4;
    @Nullable
    private static DDProjectUIManager instance;

    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
    @Nullable
    private volatile DDProjectManager projectManager;

    private DDProjectUIManager() {
        accessService.addKeyBundleListener(this::updateProjectManager);
        updateProjectManager();
    }

    @NotNull
    public static synchronized DDProjectUIManager getInstance() {
        if (instance == null) {
            instance = new DDProjectUIManager();
        }
        return instance;
    }

    public boolean isDDReady() {
        return projectManager != null;
    }

    @NotNull
    public DDProjectManager getProjectManager() throws DBException {
        DDProjectManager manager = projectManager;
        if (manager == null) {
            throw new DBException("Import encryption keys for the current DataDam account in Preferences > DataDam > Projects");
        }
        return manager;
    }

    private void updateProjectManager() {
        DDKeyBundle bundle = accessService.getKeyBundle();
        projectManager = bundle == null ? null : DDProjectManager.getInstance(bundle);
        DDProjectSyncPropertyTester.firePropertyChange();
    }

    @NotNull
    public List<DDProjectView> loadProjectViews(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (monitor.isCanceled()) {
            return List.of();
        }
        DDProjectManager manager = getProjectManager();
        Map<UUID, DDSharedProject> remoteProjects = new LinkedHashMap<>();
        for (DDSharedProject remote : manager.getRemoteProjects()) {
            remoteProjects.put(remote.id(), remote);
        }
        List<ProjectViewJob> jobs = new ArrayList<>();
        for (LocalProjectInfo local : manager.getLocalProjects()) {
            if (monitor.isCanceled()) {
                return List.of();
            }
            if (local.remoteProjectId() == null) {
                continue;
            }
            DDSharedProject remote = remoteProjects.remove(local.remoteProjectId());
            jobs.add(new ProjectViewJob(manager, local, remote));
        }
        if (monitor.isCanceled()) {
            return List.of();
        }
        if (!jobs.isEmpty()) {
            JobGroup group = createJobGroup(DDTrackingUIMessages.project_sync_import_list_job,
                Math.min(VIEW_LOAD_THREADS, jobs.size()), jobs.size());
            for (ProjectViewJob job : jobs) {
                job.setJobGroup(group);
                job.setSystem(true);
                job.schedule();
            }
            if (joinJobs(group, monitor)) {
                return List.of();
            }
        }
        List<DDProjectView> views = new ArrayList<>();
        for (ProjectViewJob job : jobs) {
            if (monitor.isCanceled()) {
                return List.of();
            }
            IStatus status = job.getResult();
            if (status == null || status.matches(IStatus.CANCEL)) {
                return List.of();
            }
            if (status.getSeverity() == IStatus.ERROR) {
                throw new DBException(status.getMessage(), status.getException());
            }
            views.add(job.view);
        }
        for (DDSharedProject remote : remoteProjects.values()) {
            if (monitor.isCanceled()) {
                return List.of();
            }
            views.add(new DDProjectView(
                remote.name(), remote.description(), null, remote, null, ProjectStatus.NOT_ON_DEVICE
            ));
        }
        return monitor.isCanceled() ? List.of() : views;
    }

    private class ProjectViewJob extends AbstractJob {
        @NotNull
        private final DDProjectManager manager;
        @NotNull
        private final LocalProjectInfo local;
        @Nullable
        private final DDSharedProject remote;
        private DDProjectView view;

        private ProjectViewJob(
            @NotNull DDProjectManager manager, @NotNull LocalProjectInfo local, @Nullable DDSharedProject remote
        ) {
            super(local.project().getName());
            this.manager = manager;
            this.local = local;
            this.remote = remote;
        }

        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            DBPProject project = local.project();
            try {
                ProjectStatus status = remote == null ? ProjectStatus.MISSING_ON_SERVER : manager.calculateSyncStatus(project);
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                view = new DDProjectView(
                    project.getName(), remote == null ? project.getDescription() : remote.description(), project, remote,
                    local.lastSyncedRevision(), status
                );
                return Status.OK_STATUS;
            } catch (DBException e) {
                return Status.error("Could not load DataDam project '" + project.getName() + "'", e);
            }
        }
    }

    @NotNull
    public List<DBPProject> getNotDatadamLocalProjects() throws DBException {
        return getProjectManager().getLocalProjects().stream()
            .filter(info -> info.remoteProjectId() == null)
            .map(LocalProjectInfo::project)
            .toList();
    }

    @NotNull
    public ProjectStatus calculateStatus(@NotNull DBPProject project) throws DBException {
        return getProjectManager().calculateSyncStatus(project);
    }

    @NotNull
    public ProjectJobBatchResult<SyncResult> synchronizeProjects(@NotNull List<DBPProject> projects) {
        List<DDProjectSynchronizationJob> jobs = projects.stream().map(DDProjectSynchronizationJob::new).toList();
        return runJobs(
            DDTrackingUIMessages.project_sync_update_job, jobs,
            projects.stream().map(DBPProject::getName).toList(), 0
        );
    }

    @NotNull
    public ProjectJobBatchResult<SyncResult> resolveConflict(
        @NotNull DBPProject project, @NotNull ConflictResolution resolution
    ) {
        DDProjectConflictResolutionJob job = new DDProjectConflictResolutionJob(project, resolution);
        return runJobs(DDTrackingUIMessages.project_sync_resolve_job, List.of(job), List.of(project.getName()), 0);
    }

    @Nullable
    public Choice openConflictDialog(@NotNull Shell shell, @NotNull DBPProject project) throws DBException {
        DBRRunnableWithResult<ConflictDetails> loader = new DBRRunnableWithResult<>() {
            @Override
            public void run(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException {
                monitor.beginTask(DDTrackingUIMessages.project_sync_revisions_job, 1);
                try {
                    if (!DDDesktopTokenUIManager.getInstance().ensureTokenReady(monitor)) {
                        return;
                    }
                    DDProjectManager manager = getProjectManager();
                    LocalDateTime startTime = manager.getLocalProject(project).lastSyncedRevision();
                    ProjectRevisions revisions = manager.getProjectRevisions(project, startTime, null);
                    if (!monitor.isCanceled()) {
                        result = new ConflictDetails(project, revisions);
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            UIUtils.runInProgressDialog(loader);
        } catch (InvocationTargetException e) {
            throw new DBException(DDTrackingUIMessages.project_sync_revisions_failed, e.getTargetException());
        }
        ConflictDetails details = loader.getResult();
        if (details == null) {
            return null;
        }
        int buttonId = new DDProjectConflictDialog(shell, details).open();
        Choice choice = switch (buttonId) {
            case DDProjectConflictDialog.USE_LOCAL_ID -> Choice.USE_LOCAL;
            case DDProjectConflictDialog.USE_SERVER_ID -> Choice.USE_SERVER;
            case DDProjectConflictDialog.STOP_SYNC_ID -> Choice.UNBIND;
            default -> null;
        };
        if (choice == Choice.UNBIND && !UIUtils.confirmAction(
            shell, DDTrackingUIMessages.project_sync_unbind_confirm_title,
            NLS.bind(DDTrackingUIMessages.project_sync_unbind_confirm_message, project.getName()))) {
            return null;
        }
        return choice;
    }

    @NotNull
    public ProjectJobBatchResult<?> applyConflictChoice(@NotNull DBPProject project, @NotNull Choice choice) {
        return switch (choice) {
            case USE_LOCAL -> resolveConflict(project, ConflictResolution.USE_LOCAL_VERSION);
            case USE_SERVER -> resolveConflict(project, ConflictResolution.USE_SERVER_VERSION);
            case UNBIND -> unbindProjects(List.of(project));
        };
    }

    public record ConflictDetails(@NotNull DBPProject project, @NotNull ProjectRevisions revisions) {
    }

    @NotNull
    public ProjectJobBatchResult<DBPProject> importProjects(@NotNull List<DDSharedProject> projects) {
        List<DDProjectImportJob> jobs = projects.stream().map(DDProjectImportJob::new).toList();
        return runJobs(
            DDTrackingUIMessages.project_sync_import_job, jobs,
            projects.stream().map(DDSharedProject::name).toList(), 1
        );
    }

    @NotNull
    public ProjectJobBatchResult<DBPProject> unbindProjects(@NotNull List<DBPProject> projects) {
        List<DDProjectUnbindJob> jobs = projects.stream().map(DDProjectUnbindJob::new).toList();
        return runJobs(
            DDTrackingUIMessages.project_sync_unbind_job, jobs,
            projects.stream().map(DBPProject::getName).toList(), 0
        );
    }

    @NotNull
    public ProjectJobBatchResult<DDSharedProject> shareProject(
        @NotNull DBPProject project, @NotNull String name, @Nullable String description
    ) {
        DDProjectSharingJob job = new DDProjectSharingJob(project, name, description);
        return runJobs(
            DDTrackingUIMessages.project_sync_share_job, List.of(job),
            List.of(project.getName()), 0
        );
    }

    @NotNull
    public ProjectJobBatchResult<DDSharedProject> updateProject(
        @NotNull DDSharedProject project, @NotNull String name, @Nullable String description
    ) {
        DDProjectUpdateJob job = new DDProjectUpdateJob(project, name, description);
        return runJobs(DDTrackingUIMessages.project_edit_job, List.of(job), List.of(project.name()), 0);
    }

    @NotNull
    private <T, J extends DDProjectOperationJob<T>> ProjectJobBatchResult<T> runJobs(
        @NotNull String title,
        @NotNull List<J> jobs,
        @NotNull List<String> projectNames,
        int maxThreads
    ) {
        if (jobs.isEmpty()) {
            return new ProjectJobBatchResult<>(List.of(), List.of(), false);
        }
        JobGroup group = createJobGroup(title, maxThreads, jobs.size());
        DBRRunnableWithResult<Boolean> runner = new DBRRunnableWithResult<>() {
            @Override
            public void run(@NotNull DBRProgressMonitor monitor) throws InterruptedException {
                result = false;
                monitor.beginTask(title, jobs.size());
                try {
                    for (Job job : jobs) {
                        job.setJobGroup(group);
                        job.setSystem(true);
                        job.schedule();
                    }
                    result = joinJobs(group, monitor);
                } finally {
                    monitor.done();
                }
            }
        };
        IStatus executionError = null;
        try {
            UIUtils.runInProgressDialog(runner);
        } catch (InvocationTargetException e) {
            group.cancel();
            executionError = Status.error(title, e.getTargetException());
        }
        List<T> results = new ArrayList<>();
        List<ProjectJobFailure> failures = new ArrayList<>();
        boolean canceled = Boolean.TRUE.equals(runner.getResult());
        for (int i = 0; i < jobs.size(); i++) {
            J job = jobs.get(i);
            IStatus status = job.getResult();
            if (status == null) {
                if (executionError != null) {
                    failures.add(new ProjectJobFailure(projectNames.get(i), executionError));
                } else {
                    canceled = true;
                }
            } else if (status.getSeverity() == IStatus.ERROR) {
                failures.add(new ProjectJobFailure(projectNames.get(i), status));
            } else if (status.matches(IStatus.CANCEL)) {
                canceled = true;
            } else if (status.isOK()) {
                T value = job.getOperationResult();
                if (value != null) {
                    results.add(value);
                }
            }
        }
        return new ProjectJobBatchResult<>(results, failures, canceled);
    }

    @NotNull
    private JobGroup createJobGroup(@NotNull String title, int maxThreads, int jobCount) {
        return new JobGroup(title, maxThreads, jobCount) {
            @Override
            protected boolean shouldCancel(@NotNull IStatus result, int failedJobs, int canceledJobs) {
                return canceledJobs > 0;
            }
        };
    }

    private boolean joinJobs(@NotNull JobGroup group, @NotNull DBRProgressMonitor monitor) {
        try {
            group.join(0, monitor.getNestedMonitor());
            return monitor.isCanceled();
        } catch (OperationCanceledException e) {
            cancelAndJoin(group);
            return true;
        } catch (InterruptedException e) {
            cancelAndJoin(group);
            Thread.currentThread().interrupt();
            return true;
        }
    }

    private void cancelAndJoin(@NotNull JobGroup group) {
        group.cancel();
        // Backend operations finish their current transfer before the caller refreshes the table.
        boolean interrupted = false;
        while (true) {
            try {
                group.join(0, null);
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public record ProjectJobFailure(@NotNull String projectName, @NotNull IStatus error) {
    }

    public record ProjectJobBatchResult<T>(
        @NotNull List<T> results,
        @NotNull List<ProjectJobFailure> failures,
        boolean canceled
    ) {
        public ProjectJobBatchResult {
            results = List.copyOf(results);
            failures = List.copyOf(failures);
        }
    }
}
