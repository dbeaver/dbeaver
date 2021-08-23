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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskFolder;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIViewMessages;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class TaskHandlerDelete extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        List<DBPNamedObject> objectsToDelete = new ArrayList<>();

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection)selection;
            for (Object element : structSelection) {
                if (element instanceof DBTTask) {
                    objectsToDelete.add((DBTTask) element);
                } else if (element instanceof DBTTaskFolder) {
                    objectsToDelete.add((DBTTaskFolder) element);
                }
            }
        }

        if (!objectsToDelete.isEmpty()) {
            if (objectsToDelete.size() == 1) {
                DBPNamedObject namedObject = objectsToDelete.get(0);
                if (namedObject instanceof DBTTaskFolder) {
                    if (confirmDeleteObjectAction(event, TaskUIViewMessages.task_handler_delete_folder_error_title, TaskUIViewMessages.task_handler_delete_confirm_question_delete_task_folder, namedObject)) {
                        return null;
                    }
                } else {
                    if (confirmDeleteObjectAction(event, TaskUIViewMessages.task_handler_delete_confirm_title_delete_task, TaskUIViewMessages.task_handler_delete_confirm_question_delete_task, namedObject)) {
                        return null;
                    }
                }
            } else {
                if (!UIUtils.confirmAction(HandlerUtil.getActiveShell(event), TaskUIViewMessages.task_handler_delete_confirm_title_delete_tasks, NLS.bind(TaskUIViewMessages.task_handler_delete_confirm_question_delete_tasks, objectsToDelete.size()))) {
                    return null;
                }
            }
            for (DBPNamedObject object : objectsToDelete) {
                try {
                    if (object instanceof DBTTask) {
                        DBTTask task = (DBTTask) object;
                        task.getProject().getTaskManager().deleteTaskConfiguration(task);
                    } else {
                        DBTTaskFolder taskFolder = (DBTTaskFolder) object;
                        taskFolder.getProject().getTaskManager().removeTaskFolder(taskFolder);
                    }
                } catch (DBException e) {
                    if (object instanceof DBTTask) {
                        DBWorkbench.getPlatformUI().showError(
                            TaskUIViewMessages.task_handler_delete_error_deleting_task_from_scheduler_title,
                            NLS.bind(TaskUIViewMessages.task_handler_delete_error_deleting_task_from_scheduler_message, ((DBTTask)object).getId()),
                            e
                        );
                    } else {
                        DBWorkbench.getPlatformUI().showError(
                            TaskUIViewMessages.task_handler_delete_folder_error_title,
                            NLS.bind(TaskUIViewMessages.task_handler_delete_folder_error_message, object.getName()),
                            e
                        );
                    }
                }
            }
        }

        return null;
    }

    private boolean confirmDeleteObjectAction(ExecutionEvent event, String title, String message, DBPNamedObject namedObject) {
        return !UIUtils.confirmAction(HandlerUtil.getActiveShell(event), title, NLS.bind(message, namedObject.getName()));
    }

}