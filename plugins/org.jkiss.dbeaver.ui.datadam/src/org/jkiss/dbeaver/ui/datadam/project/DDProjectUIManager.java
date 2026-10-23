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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.LocalProjectInfo;
import org.jkiss.dbeaver.model.datadam.sync.project.ProjectStatus;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.job.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DDProjectUIManager {
    private static final DDProjectUIManager INSTANCE = new DDProjectUIManager();

    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();

    private DDProjectUIManager() {
    }

    @NotNull
    public static DDProjectUIManager getInstance() {
        return INSTANCE;
    }

    public boolean isDDReady() {
        return accessService.getKeyBundle() != null;
    }

    @NotNull
    public DDProjectManager getProjectManager() throws DBException {
        DDKeyBundle bundle = accessService.getKeyBundle();
        if (bundle == null) {
            throw new DBException("Import encryption keys for the current DataDam account in Preferences > DataDam");
        }
        return DDProjectManager.getInstance(bundle);
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
        List<DDProjectView> views = new ArrayList<>();
        for (LocalProjectInfo local : manager.getLocalProjects()) {
            if (monitor.isCanceled()) {
                return List.of();
            }
            if (local.remoteProjectId() == null) {
                continue;
            }
            DBPProject project = local.project();
            DDSharedProject remote = remoteProjects.remove(local.remoteProjectId());
            ProjectStatus status = remote == null ? ProjectStatus.MISSING_ON_SERVER : manager.getStatus(project);
            views.add(new DDProjectView(
                project.getName(), remote == null ? project.getDescription() : remote.description(), project, remote,
                local.lastSyncedRevision(), status
            ));
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

    @NotNull
    public List<DBPProject> getNotDatadamLocalProjects() throws DBException {
        return getProjectManager().getLocalProjects().stream()
            .filter(info -> info.remoteProjectId() == null)
            .map(LocalProjectInfo::project)
            .toList();
    }

    @NotNull
    public ProjectStatus getStatus(@NotNull DBPProject project) throws DBException {
        return getProjectManager().getStatus(project);
    }

    @NotNull
    public ProjectJobBatchResult<ProjectStatus> synchronizeProjects(@NotNull List<DBPProject> projects) {
        List<DDProjectSynchronizationJob> jobs = projects.stream().map(DDProjectSynchronizationJob::new).toList();
        return runJobs(
            DDTrackingUIMessages.project_sync_update_job, jobs,
            projects.stream().map(DBPProject::getName).toList(), 0
        );
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
    public ProjectJobBatchResult<DDSharedProject> shareProject(@NotNull DBPProject project, @Nullable String description) {
        DDProjectSharingJob job = new DDProjectSharingJob(project, description);
        return runJobs(
            DDTrackingUIMessages.project_sync_share_job, List.of(job),
            List.of(project.getName()), 0
        );
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
        JobGroup group = new JobGroup(title, maxThreads, jobs.size()) {
            @Override
            protected boolean shouldCancel(@NotNull IStatus result, int failedJobs, int canceledJobs) {
                return canceledJobs > 0;
            }
        };
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
                    try {
                        group.join(0, monitor.getNestedMonitor());
                    } catch (OperationCanceledException | InterruptedException e) {
                        result = true;
                        group.cancel();
                        // Backend operations finish their current transfer before the caller refreshes the table.
                        group.join(0, null);
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                    }
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
