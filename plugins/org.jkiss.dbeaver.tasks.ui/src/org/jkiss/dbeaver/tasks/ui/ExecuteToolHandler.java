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
package org.jkiss.dbeaver.tasks.ui;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.task.TaskTypeDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.tools.registry.ToolDescriptor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.Collections;
import java.util.List;

public class ExecuteToolHandler implements IActionDelegate {

    private final IWorkbenchWindow window;
    private final ToolDescriptor tool;
    private ISelection selection;

    public ExecuteToolHandler(IWorkbenchWindow window, ToolDescriptor tool) {
        this.window = window;
        this.tool = tool;
    }

    @Override
    public void run(IAction action) {
        if (!selection.isEmpty()) {
            List<DBSObject> selectedObjects = NavigatorUtils.getSelectedObjects(selection);
            executeTool(selectedObjects);
        }
    }

    private void executeTool(List<DBSObject> objects) {
        try {
            DBPDataSource dataSource = objects.get(0).getDataSource();
            if (dataSource != null) {
                DBPProject selectedProject = dataSource.getContainer().getProject();
                TaskTypeDescriptor taskForObjs = tool.getTaskForObjects(objects);
                if (taskForObjs != null) {
                    if (taskForObjs.requiresMutableDatabase() && DBUtils.isReadOnly(dataSource)) {
                        DBWorkbench.getPlatformUI().showWarningMessageBox(
                            TaskUIMessages.task_execute_handler_tool_warn_readonly_title,
                            NLS.bind(TaskUIMessages.task_execute_handler_tool_warn_readonly_message, dataSource.getName())
                        );
                    } else {
                        IStructuredSelection selectedObjects = new StructuredSelection(objects.toArray());
                        TaskConfigurationWizardDialog.openNewToolTaskDialog(
                            window,
                            selectedProject,
                            taskForObjs.getId(),
                            selectedObjects
                        );
                    }
                } else {
                    Command cmd = tool.getCommandForObjects(objects);
                    if (cmd != null) {
                        ISelection selection = new StructuredSelection(objects);
                        EvaluationContext context = new EvaluationContext(null, selection);
                        context.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, selection);

                        ExecutionEvent event = new ExecutionEvent(cmd, Collections.EMPTY_MAP, null, context);
                        cmd.executeWithChecks(event);
                    } else {
                        DBWorkbench.getPlatformUI().showWarningMessageBox(
                            TaskUIMessages.task_execute_handler_tool_error_title, 
                            TaskUIMessages.task_execute_handler_tool_error_apply_message
                        );
                    }
                }
            } else {
                DBWorkbench.getPlatformUI().showWarningMessageBox(
                    TaskUIMessages.task_execute_handler_tool_error_title,
                    TaskUIMessages.task_execute_handler_tool_error_project_message
                );
            }
        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showError(
                TaskUIMessages.task_execute_handler_tool_error_title,
                NLS.bind(TaskUIMessages.task_execute_handler_tool_error_message, tool.getLabel()),
                e
            );
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

}