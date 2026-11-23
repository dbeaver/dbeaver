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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.project.ProjectStatus;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DDUIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectConflictResolver;
import org.jkiss.dbeaver.ui.datadam.project.job.DDProjectSynchronizationJob;

import java.util.concurrent.atomic.AtomicReference;

public class DDProjectUpdateHandler extends DDProjectHandler<DDProjectSynchronizationJob> {
    private final DDProjectConflictResolver conflictResolver = new DDProjectConflictResolver(uiManager);

    @NotNull
    @Override
    protected DDProjectSynchronizationJob createJob(@NotNull ExecutionEvent event, @NotNull DBPProject project) {
        return new DDProjectSynchronizationJob(project);
    }

    public void synchronizeProject(@NotNull DBPProject project) {
        scheduleJob(project, new DDProjectSynchronizationJob(project));
    }

    @Override
    protected void onSuccess(@NotNull DBPProject project, @NotNull DDProjectSynchronizationJob job) {
        var status = job.getOperationResult();
        if (status == null) {
            return;
        }
        switch (status.status()) {
            case UP_TO_DATE -> showMessage(project, DDTrackingUIMessages.project_sync_update_success);
            case CONFLICT -> resolveConflict(status.project());
            default ->
                showError(project, DDTrackingUIMessages.project_sync_update_failed,
                    new DBException("Project synchronization ended with status: " + status.status()));
        }
    }

    private void resolveConflict(@NotNull DBPProject project) {
        Shell shell = UIUtils.getActiveWorkbenchShell();
        AtomicReference<ProjectStatus> resolutionResult = new AtomicReference<>();
        var resolution = new AbstractJob(DDTrackingUIMessages.project_sync_resolve_job) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    ProjectStatus result = conflictResolver.resolve(project, monitor, shell);
                    resolutionResult.set(result);
                    return result == null ? Status.CANCEL_STATUS : Status.OK_STATUS;
                } catch (DBException e) {
                    return Status.error(DDTrackingUIMessages.project_sync_resolve_failed, e);
                }
            }
        };
        scheduleBackgroundJob(resolution, status -> {
            ProjectStatus result = resolutionResult.get();
            if (status.getSeverity() == IStatus.ERROR) {
                showError(project, status.getMessage(), status.getException());
            } else if (status.isOK() && result != null) {
                switch (result) {
                    case NOT_SYNC -> showMessage(project, DDTrackingUIMessages.project_sync_unbind_success);
                    case UP_TO_DATE -> showMessage(project, DDTrackingUIMessages.project_sync_update_success);
                    case CONFLICT -> DDUIUtils.showNotification(
                        projectTitle(project),
                        NLS.bind(DDTrackingUIMessages.projects_conflicts, project.getName()), DBPMessageType.WARNING, null);
                    default -> showError(project, DDTrackingUIMessages.project_sync_update_failed,
                        new DBException("Project synchronization ended with status: " + result));
                }
            }
        });
    }
}
