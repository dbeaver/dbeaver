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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.SyncResult;
import org.jkiss.dbeaver.model.datadam.sync.project.ProjectStatus;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectConflictDialog.Choice;
import org.jkiss.dbeaver.ui.datadam.project.job.DDProjectSynchronizationJob;

public class DDProjectUpdateHandler extends DDProjectHandler<DDProjectSynchronizationJob> {
    @NotNull
    @Override
    protected DDProjectSynchronizationJob createJob(@NotNull ExecutionEvent event, @NotNull DBPProject project) {
        return new DDProjectSynchronizationJob(project);
    }

    @Override
    protected void onSuccess(@NotNull DDProjectSynchronizationJob job) {
        var status = job.getOperationResult();
        if (status == null) {
            return;
        }
        switch (status.status()) {
            case UP_TO_DATE -> showMessage(DDTrackingUIMessages.project_sync_update_success);
            case CONFLICT -> resolveConflict(status.project());
            default ->
                showError(DDTrackingUIMessages.project_sync_update_failed,
                    new DBException("Project synchronization ended with status: " + status.status()));
        }
    }

    private void resolveConflict(@NotNull DBPProject project) {
        try {
            Choice choice = uiManager.openConflictDialog(UIUtils.getActiveWorkbenchShell(), project);
            if (choice == null) {
                return;
            }
            var result = uiManager.applyConflictChoice(project, choice);
            if (!result.failures().isEmpty()) {
                var error = result.failures().get(0).error();
                showError(error.getMessage(), new DBException(error.getMessage(), error.getException()));
            } else if (choice == Choice.UNBIND && !result.canceled()) {
                showMessage(DDTrackingUIMessages.project_sync_unbind_success);
            } else if (result.results().stream().anyMatch(value ->
                value instanceof SyncResult sync
                    && sync.status() == ProjectStatus.UP_TO_DATE)) {
                showMessage(DDTrackingUIMessages.project_sync_update_success);
            }
        } catch (DBException e) {
            showError(DDTrackingUIMessages.project_sync_revisions_failed, e);
        }
    }
}
