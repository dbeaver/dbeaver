/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTaskUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.wizard.EditTaskVariablesDialog;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskProcessorUI;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class TaskHandlerRun extends AbstractHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        String taskId = event.getParameter("task");
        DBTTask task = null;
        if (!CommonUtils.isEmpty(taskId)) {
            task = NavigatorUtils.getSelectedProject().getTaskManager().getTaskById(taskId);
        } else {
            final ISelection selection = HandlerUtil.getCurrentSelection(event);
            if (selection instanceof IStructuredSelection) {
                Object element = ((IStructuredSelection) selection).getFirstElement();
                if (element instanceof DBTTask) {
                    task = (DBTTask) element;
                }
            }
        }

        if (task != null) {
            runTask(task);
        }

        return null;
    }

    public static void runTask(DBTTask task) {
        if (!confirmTaskVariables(task)) {
            return;
        }

        try {
            TaskProcessorUI listener = new TaskProcessorUI(UIUtils.getDialogRunnableContext(), task);
            task.getProject().getTaskManager().scheduleTask(task, listener);
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Task run", "Error running task '" + task.getName() + "'", e);
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        String taskId = CommonUtils.toString(parameters.get("task"));
        if (!CommonUtils.isEmpty(taskId)) {
            DBTTask task = NavigatorUtils.getSelectedProject().getTaskManager().getTaskById(taskId);
            if (task != null) {
                DBPImage taskIcon = task.getType().getIcon();
                if (taskIcon == null) taskIcon = DBIcon.TREE_TASK;
                element.setIcon(DBeaverIcons.getImageDescriptor(taskIcon));
                element.setText(task.getName());
            }
        }

    }

    private static boolean confirmTaskVariables(@NotNull DBTTask task) {
        final Map<DBTTask, Map<String, Object>> variables = new LinkedHashMap<>();

        try {
            DBTaskUtils.collectTaskVariables(
                task,
                task1 -> CommonUtils.toBoolean(task1.getProperties().get(DBTaskUtils.TASK_PROMPT_VARIABLES)),
                variables
            );
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Task variables", "Error collecting task variables", e);
            return true;
        }

        if (variables.values().stream().anyMatch(vars -> !vars.isEmpty())) {
            final EditTaskVariablesDialog dialog = new EditTaskVariablesDialog(UIUtils.getActiveWorkbenchShell(), variables);

            if (dialog.open() != IDialogConstants.OK_ID) {
                return false;
            }

            for (DBTTask other : variables.keySet()) {
                DBTaskUtils.setVariables(other, dialog.getVariables(other));
            }
        }

        return true;
    }
}