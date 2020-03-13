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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TaskHandlerDelete extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        List<DBTTask> tasksToDelete = new ArrayList<>();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection)selection;
            for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                if (element instanceof DBTTask) {
                    tasksToDelete.add((DBTTask) element);
                }
            }
        }

        if (!tasksToDelete.isEmpty()) {
            if (tasksToDelete.size() == 1) {
                if (!UIUtils.confirmAction(HandlerUtil.getActiveShell(event), "Delete task", "Are you sure you want to delete task '" + tasksToDelete.get(0).getName() + "'?")) {
                    return null;
                }
            } else {
                if (!UIUtils.confirmAction(HandlerUtil.getActiveShell(event), "Delete tasks", "Are you sure you want to delete " + tasksToDelete.size() + " tasks?")) {
                    return null;
                }
            }
            for (DBTTask task : tasksToDelete) {
                task.getProject().getTaskManager().deleteTaskConfiguration(task);
            }
        }

        return null;
    }

}