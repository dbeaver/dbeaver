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

import com.dbeaver.datadam.share.api.model.DDSharedProject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectSyncService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectSyncPropertyTester;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectSyncUIManager;

import java.util.List;

public class DDProjectImportHandler extends AbstractHandler {
    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        DDProjectSyncUIManager ddManager = DDProjectSyncUIManager.getInstance();
        if (!ddManager.isDDEnabled()) {
            return null;
        }
        DDProjectSyncService service = ddManager.getService();
        new AbstractJob(DDTrackingUIMessages.project_sync_import_list_job) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    List<DDSharedProject> projects = service.listNotAlreadyBindRemoteProjects();
                    UIUtils.asyncExec(() -> selectProject(ddManager, service, projects));
                } catch (DBException e) {
                    ddManager.showError(DDTrackingUIMessages.project_sync_import_list_failed, e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
        return null;
    }

    private static void selectProject(
        @NotNull DDProjectSyncUIManager ddManager,
        @NotNull DDProjectSyncService service,
        @NotNull List<DDSharedProject> projects
    ) {
        if (projects.isEmpty()) {
            ddManager.showMessage(DDTrackingUIMessages.project_sync_import_no_projects, false);
            return;
        }
        Shell shell = UIUtils.getActiveWorkbenchShell();
        if (shell == null) {
            return;
        }
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(
            shell, new LabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return ((DDSharedProject) element).name();
            }
        }
        ) {
            @Override
            protected void createButtonsForButtonBar(@NotNull Composite parent) {
                createButton(parent, IDialogConstants.OK_ID, DDTrackingUIMessages.project_sync_import_button, true);
                createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
            }
        };
        dialog.setTitle(DDTrackingUIMessages.project_sync_import_dialog_title);
        dialog.setMessage(DDTrackingUIMessages.project_sync_import_dialog_message);
        dialog.setMultipleSelection(false);
        dialog.setElements(projects.toArray());
        if (dialog.open() == Window.OK) {
            importProject(ddManager, service, (DDSharedProject) dialog.getFirstResult());
        }
    }

    private static void importProject(
        @NotNull DDProjectSyncUIManager ddManager,
        @NotNull DDProjectSyncService service,
        @NotNull DDSharedProject project
    ) {
        new AbstractJob(DDTrackingUIMessages.project_sync_import_job) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    DBPProject importedProject = service.importProject(project);
                    DDProjectSyncPropertyTester.firePropertyChange();
                    ddManager.showMessage(
                        NLS.bind(DDTrackingUIMessages.project_sync_import_success, importedProject.getName()), false);
                } catch (DBException e) {
                    ddManager.showError(DDTrackingUIMessages.project_sync_import_failed, e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }
}
