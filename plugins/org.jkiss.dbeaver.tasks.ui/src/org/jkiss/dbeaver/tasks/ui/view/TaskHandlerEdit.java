/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.registry.TaskUIRegistry;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;

public class TaskHandlerEdit extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection)selection).getFirstElement();
            if (element instanceof DBTTask) {
                DBTTask task = (DBTTask) element;
                DBTTaskType taskTypeDescriptor = task.getType();
                if (!TaskUIRegistry.getInstance().supportsConfigurator(taskTypeDescriptor)) {
                    return null;
                }
                try {
                    TaskConfigurationWizard wizard = TaskUIRegistry.getInstance().createConfigurator(taskTypeDescriptor).createTaskConfigWizard(task);
                    if (wizard != null) {
                        TaskConfigurationWizardDialog dialog = new TaskConfigurationWizardDialog(HandlerUtil.getActiveWorkbenchWindow(event), wizard);
                        dialog.open();
                    }
                } catch (Throwable e) {
                    DBWorkbench.getPlatformUI().showError("Task configuration", "Error opening task '" + task.getName() + "' configuration editor", e);
                }
            }
        }

        return null;
    }

}