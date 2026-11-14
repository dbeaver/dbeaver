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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.sync.project.ConflictResolution;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.LocalProjectInfo;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.SyncResult;
import org.jkiss.dbeaver.model.datadam.sync.project.ProjectStatus;
import org.jkiss.dbeaver.model.navigator.DBNEmptyNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DDDesktopTokenUIManager;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.job.*;
import org.jkiss.dbeaver.ui.datadam.project.navigator.DDProjectSyncPropertyTester;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorContent;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.ui.navigator.project.ProjectExplorerView;
import org.jkiss.dbeaver.ui.navigator.project.ProjectNavigatorView;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DDProjectUIManager {
    private static final int VIEW_LOAD_THREADS = 4;
    @Nullable
    private static DDProjectUIManager instance;

    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
    @NotNull
    private final DDProjectManager projectManager = DDProjectManager.getInstance();

    private DDProjectUIManager() {
        accessService.addKeyBundleListener(this::refreshProjectVisibility);
        accessService.addAccessListener(new DDDesktopAccessListener() {
            @Override
            public void accessChanging() {
            }

            @Override
            public void accessChanged() {
                refreshProjectVisibility();
            }
        });
        refreshProjectVisibility();
    }

    @NotNull
    public static synchronized DDProjectUIManager getInstance() {
        if (instance == null) {
            instance = new DDProjectUIManager();
        }
        return instance;
    }

    public boolean isProjectSyncReady() {
        return accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN
            && accessService.getKeyBundle() != null;
    }

    @NotNull
    public DDProjectManager getProjectManager() {
        return projectManager;
    }

    public void refreshProjectVisibility() {
        DDProjectSyncPropertyTester.firePropertyChange();
        UIUtils.asyncExec(() -> {
            for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
                for (IWorkbenchPage page : window.getPages()) {
                    refreshNavigator(page, ProjectNavigatorView.VIEW_ID);
                    refreshNavigator(page, ProjectExplorerView.VIEW_ID);
                    refreshNavigator(page, DatabaseNavigatorView.VIEW_ID);
                }
            }
        });
    }

    private void refreshNavigator(@NotNull IWorkbenchPage page, @NotNull String viewId) {
        if (page.findView(viewId) instanceof NavigatorViewBase view && view.getNavigatorTree() != null
            && !view.getNavigatorTree().isDisposed()) {
            Object input = view.getNavigatorViewer().getInput();
            DBNNode currentRoot = input instanceof DatabaseNavigatorContent content ? content.getRootNode() : null;
            DBNNode newRoot = view.getRootNode();
            if (currentRoot == newRoot || currentRoot instanceof DBNEmptyNode && newRoot instanceof DBNEmptyNode) {
                view.getNavigatorViewer().refresh();
            } else if (view instanceof ProjectExplorerView explorer) {
                explorer.refreshRepresentation();
            } else {
                view.getNavigatorTree().setInput(newRoot);
            }
        }
    }

    @Nullable
    public List<DDProjectView> loadProjectViews(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (monitor.isCanceled()) {
            return List.of();
        }
        List<ProjectListing> listings = loadProjectListings(monitor);
        if (listings == null) {
            return null;
        }
        DDProjectManager manager = getProjectManager();
        List<ProjectViewJob> jobs = new ArrayList<>();
        for (ProjectListing listing : listings) {
            if (monitor.isCanceled()) {
                return List.of();
            }
            LocalProjectInfo local = listing.localProject();
            if (local != null && local.remoteProjectId() != null) {
                jobs.add(new ProjectViewJob(manager, local, listing.remoteProject()));
            }
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
        for (ProjectListing listing : listings) {
            if (monitor.isCanceled()) {
                return List.of();
            }
            if (listing.localProject() == null && listing.remoteProject() != null) {
                DDSharedProject remote = listing.remoteProject();
                views.add(new DDProjectView(
                    remote.name(), remote.description(), null, remote, null, ProjectStatus.NOT_ON_DEVICE
                ));
            }
        }
        return monitor.isCanceled() ? List.of() : views;
    }

    @Nullable
    public List<ProjectListing> loadProjectListings(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (monitor.isCanceled() || !DDDesktopTokenUIManager.getInstance().ensureTokenReady(monitor)) {
            return null;
        }
        DDProjectManager manager = getProjectManager();
        Map<UUID, DDSharedProject> remoteProjects = new LinkedHashMap<>();
        for (DDSharedProject remote : manager.getRemoteProjects()) {
            remoteProjects.put(remote.id(), remote);
        }
        if (monitor.isCanceled()) {
            return null;
        }
        List<ProjectListing> listings = new ArrayList<>();
        for (LocalProjectInfo local : manager.getLocalProjects()) {
            if (monitor.isCanceled()) {
                return null;
            }
            listings.add(new ProjectListing(local, remoteProjects.remove(local.remoteProjectId())));
        }
        for (DDSharedProject remote : remoteProjects.values()) {
            if (monitor.isCanceled()) {
                return null;
            }
            listings.add(new ProjectListing(null, remote));
        }
        return listings;
    }

    public record ProjectListing(
        @Nullable LocalProjectInfo localProject,
        @Nullable DDSharedProject remoteProject
    ) {
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
    public List<LocalProjectInfo> getLocalProjects() throws DBException {
        return getProjectManager().getLocalProjects();
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
        refreshProjectVisibility();
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
