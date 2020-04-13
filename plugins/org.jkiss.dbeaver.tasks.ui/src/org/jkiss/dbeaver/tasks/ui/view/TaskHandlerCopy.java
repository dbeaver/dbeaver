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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;

import java.util.LinkedHashMap;

public class TaskHandlerCopy extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object firstElement = structSelection.getFirstElement();
            if (firstElement instanceof DBTTask) {
                DBTTask oldTask = (DBTTask) firstElement;
                for (;;) {
                    EnterNameDialog taskNameDialog = new EnterNameDialog(HandlerUtil.getActiveShell(event), TaskUIMessages.task_handler_copy_name_dialog_enter_task, oldTask.getName());
                    String newTaskName = taskNameDialog.chooseName();
                    if (newTaskName == null) {
                        return null;
                    }
                    DBTTaskManager taskManager = oldTask.getProject().getTaskManager();
                    if (taskManager.getTaskByName(newTaskName) != null) {
                        UIUtils.showMessageBox(HandlerUtil.getActiveShell(event), "Duplicate task name", "Task '" + newTaskName + "' already exists", SWT.ICON_ERROR);
                        continue;
                    }
                    try {
                        DBTTask newTask = taskManager.createTask(
                            oldTask.getType(),
                            newTaskName,
                            oldTask.getDescription(),
                            new LinkedHashMap<>(oldTask.getProperties())
                        );
                        taskManager.updateTaskConfiguration(newTask);

                        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
                        if (activePart instanceof DatabaseTasksView) {
                            UIUtils.asyncExec(() -> {
                                ((DatabaseTasksView) activePart).getTasksTree().getViewer().setSelection(new StructuredSelection(newTask), true);
                                ActionUtils.runCommand(DatabaseTasksView.EDIT_TASK_CMD_ID, activePart.getSite());
                            });
                        }
                    } catch (DBException e) {
                        DBWorkbench.getPlatformUI().showError("Task copy error", "Error copying task '" + oldTask.getName() + "'", e);
                    }
                    break;
                }
            }
        }

        return null;
    }

}