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
package org.jkiss.dbeaver.ui.datadam.project.event.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.LocalProjectInfo;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;

import java.util.UUID;

public class ProjectDeletedAction extends AbstractProjectEventJob {
    public ProjectDeletedAction(@NotNull DDProjectUIManager uiManager, @NotNull UUID remoteProjectId) {
        super(uiManager, remoteProjectId, DDTrackingUIMessages.project_sync_deleted_title,
            DDTrackingUIMessages.project_sync_deleted_failed);
    }

    @NotNull
    @Override
    protected IStatus perform(@NotNull DBRProgressMonitor monitor) throws DBException {
        LocalProjectInfo local = findLocalProject();
        if (local == null) {
            log.debug("Ignoring shared project deletion: local project not found: " + remoteProjectId);
            return Status.OK_STATUS;
        }
        DBPProject project = local.project();
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }
        if (project.getWorkspace().getActiveProject() == project) {
            uiManager.getProjectManager().unbindProject(project);
            UIUtils.syncExec(() -> MessageDialog.openInformation(UIUtils.getActiveWorkbenchShell(),
                DDTrackingUIMessages.project_sync_deleted_title,
                NLS.bind(DDTrackingUIMessages.project_sync_deleted_active_message, project.getName())));
        } else {
            Integer choice = UIUtils.syncExec(new RunnableWithResult<>() {
                @NotNull
                @Override
                public Integer runWithResult() {
                    return new MessageDialog(UIUtils.getActiveWorkbenchShell(),
                        DDTrackingUIMessages.project_sync_deleted_title, null,
                        NLS.bind(DDTrackingUIMessages.project_sync_deleted_message, project.getName()),
                        MessageDialog.WARNING, 2,
                        new String[] {
                            DDTrackingUIMessages.project_sync_deleted_delete_local,
                            DDTrackingUIMessages.project_sync_deleted_keep_local,
                            IDialogConstants.CANCEL_LABEL
                        }).open();
                }
            });
            if (choice == null || choice != 0 && choice != 1 || monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            if (choice == 0) {
                project.getWorkspace().deleteProject(project);
            } else {
                uiManager.getProjectManager().unbindProject(project);
            }
        }
        uiManager.refreshProjectVisibility();
        return Status.OK_STATUS;
    }

    @Override
    protected void showError(@NotNull DBException error) {
        UIUtils.asyncExec(() -> MessageDialog.openError(UIUtils.getActiveWorkbenchShell(),
            DDTrackingUIMessages.project_sync_deleted_title,
            DDTrackingUIMessages.project_sync_deleted_failed + "\n" + error.getMessage()));
    }
}
