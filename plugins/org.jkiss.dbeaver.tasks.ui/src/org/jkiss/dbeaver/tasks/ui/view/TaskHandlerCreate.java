/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

public class TaskHandlerCreate extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DBPProject project = NavigatorUtils.getSelectedProject();
/*
        CreateTaskConfigurationDialog dialog = new CreateTaskConfigurationDialog(
            HandlerUtil.getActiveShell(event),
            project
        );
*/
        TaskConfigurationWizardDialog dialog = new TaskConfigurationWizardDialog(
            HandlerUtil.getActiveWorkbenchWindow(event));
        if (dialog.open() == IDialogConstants.OK_ID) {
            DBTTaskManager taskManager = project.getTaskManager();

            try {
/*
                DBTTaskConfigurator configurator = dialog.getSelectedCategory().createConfigurator();
                DBTTask task = taskManager.createTask(
                    dialog.getSelectedTaskType(),
                    dialog.getTaskName(),
                    dialog.getTaskDescription(),
                    dialog.getInitialProperties());
                if (!configurator.createTaskConfigWizard(task)) {
                    taskManager.deleteTaskConfiguration(task);
                }
*/
            } catch (Exception e) {
                DBWorkbench.getPlatformUI().showError("Create task failed", "Error while creating new task", e);
            }

        }
        return null;
    }

}