/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.task.DBTTaskFolder;
import org.jkiss.dbeaver.model.task.DBTTaskFolderEvent;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIViewMessages;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;

/**
 * You can use this handler for task folder renaming.
 */
public class TaskFolderRenameHandler extends AbstractHandler {

    private static final Log log = Log.getLog(TaskFolderRenameHandler.class);

    @Override
    public Object execute(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
        if (!(selectedObject instanceof DBTTaskFolder)) {
            return null;
        }
        DBTTaskFolder taskFolder = (DBTTaskFolder) selectedObject;

        String newFolderName = EnterNameDialog.chooseName(
            HandlerUtil.getActiveShell(event),
            TaskUIViewMessages.task_handler_folder_rename_property_label,
            taskFolder.getName());
        if (CommonUtils.isEmpty(newFolderName)) {
            return null;
        }

        DBTTaskManager taskManager = taskFolder.getProject().getTaskManager();
        DBTTaskFolder[] tasksFolders = taskManager.getTasksFolders();
        if (!ArrayUtils.isEmpty(tasksFolders)
            && Arrays.stream(tasksFolders).anyMatch(e -> e.getName().equalsIgnoreCase(newFolderName))) {
            DBWorkbench.getPlatformUI().showError(
                TaskUIViewMessages.task_handler_folder_rename_error_title,
                NLS.bind(TaskUIViewMessages.task_handler_folder_rename_error_message, taskFolder.getName(), newFolderName)
            );
            log.error("Can't rename task folder " + taskFolder.getName());
            return null;
        }

        taskFolder.setName(newFolderName);
        taskManager.updateConfiguration();
        TaskRegistry.getInstance().notifyTaskFoldersListeners(
            new DBTTaskFolderEvent(taskFolder, DBTTaskFolderEvent.Action.TASK_FOLDER_UPDATE));

        return null;
    }

}
