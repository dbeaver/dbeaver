/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui.view;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTaskFolder;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.registry.task.TaskManagerImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIViewMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.List;

public class TaskHandlerFolderCreate extends AbstractHandler {

    private static final Log log = Log.getLog(TaskHandlerFolderCreate.class);

    @Override
    public Object execute(ExecutionEvent event) {
        DBPProject project = NavigatorUtils.getSelectedProject();

        CreateFolderDialog createFolderDialog = new CreateFolderDialog(HandlerUtil.getActiveShell(event), project);
        if (createFolderDialog.open() == IDialogConstants.OK_ID) {
            DBPProject folderProject = createFolderDialog.getProject();
            DBTTaskManager taskManager = folderProject.getTaskManager();
            DBTTaskFolder taskFolder = null;
            try {
                taskFolder = taskManager.createTaskFolder(folderProject, createFolderDialog.getName(), null);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(
                        TaskUIViewMessages.task_handler_folder_create_error_title,
                        NLS.bind(TaskUIViewMessages.task_handler_folder_create_error_message, createFolderDialog.getName()),
                        e
                );
                log.error("Can't create new task folder", e);
            }
            // Write new task folder name in json file
            if (taskFolder != null && taskManager instanceof TaskManagerImpl) {
                ((TaskManagerImpl) taskManager).saveConfiguration();
            }
        }
        return null;
    }

    private class CreateFolderDialog extends BaseDialog {

        private DBPProject project;
        private String name;

        CreateFolderDialog(Shell parentShell, DBPProject project) {
            super(parentShell, TaskUIViewMessages.task_handler_folder_create_dialog_title, null);
            this.project = project;
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite composite = super.createDialogArea(parent);

            final Text nameText = UIUtils.createLabelText(composite, TaskUIViewMessages.task_handler_folder_create_dialog_text_label_name, ""); //$NON-NLS-2$
            nameText.addModifyListener(e -> {
                name = nameText.getText().trim();
                getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
            });

            List<DBPProject> projects = DBWorkbench.getPlatform().getWorkspace().getProjects();
            UIUtils.createControlLabel(composite, TaskUIViewMessages.task_handler_folder_create_dialog_text_label_folder_project);

            final Combo projectCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
            projectCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            for (DBPProject project : projects) {
                projectCombo.add(project.getName());
            }

            projectCombo.setText(project.getName());
            projectCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    project = projects.get(projectCombo.getSelectionIndex());
                }
            });

            return composite;
        }

        public String getName() {
            return name;
        }

        public DBPProject getProject() {
            return project;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            super.createButtonsForButtonBar(parent);
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
    }

}
