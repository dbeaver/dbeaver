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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.task.DBTTaskFolder;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;

public class TaskHandlerCreate extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {

        TaskConfigurationWizardDialog dialog = new TaskConfigurationWizardDialog(HandlerUtil.getActiveWorkbenchWindow(event));
        TaskConfigurationWizard taskWizard = dialog.getTaskWizard();
        if (taskWizard != null) {
            final ISelection selection = HandlerUtil.getCurrentSelection(event);
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection structuredSelection = (IStructuredSelection) selection;
                if (structuredSelection.size() == 1 && structuredSelection.getFirstElement() instanceof DBTTaskFolder) {
                    // We need to throw this information about the choice of task folder in TaskConfigurationWizardPageTask
                    taskWizard.setCurrentSelectedTaskFolder((DBTTaskFolder) structuredSelection.getFirstElement());
                }
            } else {
                taskWizard.setCurrentSelectedTaskFolder(null);
            }
        }
        if (dialog.open() == IDialogConstants.OK_ID) {

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