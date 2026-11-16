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
package org.jkiss.dbeaver.ui.datadam.project.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.SyncResult;
import org.jkiss.dbeaver.model.datadam.sync.project.ProjectStatus;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DDUIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectConflictResolver;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager.ProjectJobBatchResult;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager.ProjectListing;

import java.util.List;

public class DDProjectSyncAllHandler extends AbstractHandler {
    private final DDProjectUIManager uiManager = DDProjectUIManager.getInstance();
    private final DDProjectConflictResolver conflictResolver = new DDProjectConflictResolver(uiManager);

    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        Shell shell = HandlerUtil.getActiveShell(event);
        AbstractJob job = new AbstractJob(DDTrackingUIMessages.projects_sync_all) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    List<DBPProject> projects = loadBoundProjects(monitor);
                    if (projects == null || projects.isEmpty()) {
                        return Status.CANCEL_STATUS;
                    }
                    ProjectJobBatchResult<SyncResult> result = uiManager.synchronizeProjects(projects, monitor);
                    if (result.canceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    int updated = (int) result.results().stream()
                        .filter(sync -> sync.status() == ProjectStatus.UP_TO_DATE).count();
                    List<String> errors = result.failures().stream()
                        .map(failure -> failure.projectName() + ": " + failure.error().getMessage()).toList();
                    List<DBPProject> conflicts = result.results().stream()
                        .filter(sync -> sync.status() == ProjectStatus.CONFLICT)
                        .map(SyncResult::project).toList();
                    UIUtils.syncExec(() -> showResults(updated, errors));
                    resolveConflicts(conflicts, monitor, shell);
                    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
                } catch (DBException e) {
                    UIUtils.asyncExec(() -> DDUIUtils.showNotification(
                        DDTrackingUIMessages.project_sync_title, DDTrackingUIMessages.projects_sync_all_load_failed,
                        DBPMessageType.ERROR, null));
                    return Status.error(DDTrackingUIMessages.projects_sync_all_load_failed, e);
                }
            }
        };
        job.setUser(true);
        job.schedule();
        return null;
    }

    @Nullable
    private List<DBPProject> loadBoundProjects(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<ProjectListing> listings = uiManager.loadProjectListings(monitor);
        if (listings == null || monitor.isCanceled()) {
            return null;
        }
        return listings.stream()
            .filter(listing -> listing.localProject() != null && listing.remoteProject() != null)
            .map(listing -> listing.localProject().project())
            .toList();
    }

    private void resolveConflicts(
        @NotNull List<DBPProject> conflicts,
        @NotNull DBRProgressMonitor monitor,
        @NotNull Shell shell
    ) {
        for (DBPProject project : conflicts) {
            if (monitor.isCanceled()) {
                return;
            }
            try {
                ProjectStatus result = conflictResolver.resolve(project, monitor, shell);
                if (result != null) {
                    UIUtils.syncExec(() -> showConflictResult(project, result));
                }
            } catch (DBException e) {
                UIUtils.syncExec(() -> DDUIUtils.showNotification(project.getName(),
                    DDTrackingUIMessages.project_sync_resolve_failed, DBPMessageType.ERROR,
                    () -> DDUIUtils.showError(
                        DDTrackingUIMessages.project_sync_title, DDTrackingUIMessages.project_sync_resolve_failed, e)));
            }
        }
    }

    private void showConflictResult(@NotNull DBPProject project, @NotNull ProjectStatus result) {
        switch (result) {
            case UP_TO_DATE -> DDUIUtils.showNotification(project.getName(),
                DDTrackingUIMessages.project_sync_update_success, DBPMessageType.INFORMATION, null);
            case NOT_SYNC -> DDUIUtils.showNotification(project.getName(),
                DDTrackingUIMessages.project_sync_unbind_success, DBPMessageType.INFORMATION, null);
            case CONFLICT -> DDUIUtils.showNotification(project.getName(),
                NLS.bind(DDTrackingUIMessages.projects_conflicts, project.getName()), DBPMessageType.WARNING, null);
            default -> DDUIUtils.showNotification(project.getName(),
                DDTrackingUIMessages.project_sync_update_failed, DBPMessageType.ERROR, null);
        }
    }

    private void showResults(int updated, @NotNull List<String> errors) {
        if (updated > 0) {
            DDUIUtils.showNotification(DDTrackingUIMessages.project_sync_title,
                NLS.bind(DDTrackingUIMessages.project_sync_update_count, updated), DBPMessageType.INFORMATION, null);
        }
        if (!errors.isEmpty()) {
            DDUIUtils.showNotification(DDTrackingUIMessages.project_sync_title,
                NLS.bind(DDTrackingUIMessages.projects_errors, String.join("\n", errors)), DBPMessageType.ERROR, null);
        }
    }
}
