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
package org.jkiss.dbeaver.tasks.ui.view;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.registry.task.TaskImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Date;

public class TaskHandlerRename extends AbstractHandler {
    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        if (!(HandlerUtil.getCurrentSelection(event) instanceof IStructuredSelection selection)
            || selection.size() != 1
            || !(selection.getFirstElement() instanceof TaskImpl task)
            || !task.getProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT)) {
            return null;
        }
        DBTTaskManager taskManager = task.getProject().getTaskManager();
        String title = TaskUIMessages.edit_task_config_dialog_label_name;
        InputDialog dialog = new InputDialog(HandlerUtil.getActiveShell(event), title, title, task.getName(), name -> {
            if (CommonUtils.isEmptyTrimmed(name)) {
                return TaskUIMessages.task_configuration_wizard_page_task_error_message_enter_task_name;
            }
            DBTTask existingTask = taskManager.getTaskByName(name);
            return existingTask == null || existingTask == task ? null : NLS.bind(
                TaskUIMessages.task_configuration_wizard_page_task_already_exists, name, task.getProject().getName());
        });
        if (dialog.open() == IDialogConstants.OK_ID && !task.getName().equals(dialog.getValue())) {
            String oldName = task.getName();
            Date oldUpdateTime = task.getUpdateTime();
            task.setName(dialog.getValue());
            task.setUpdateTime(new Date());
            try {
                taskManager.updateTaskConfiguration(task);
            } catch (DBException e) {
                task.setName(oldName);
                task.setUpdateTime(oldUpdateTime);
                DBWorkbench.getPlatformUI().showError(title, null, e);
            }
        }
        return null;
    }
}
