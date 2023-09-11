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
package org.jkiss.dbeaver.tasks.ui;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.task.TaskTypeDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.tools.registry.ToolDescriptor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.List;

public class ExecuteToolHandler implements IActionDelegate {

    private static final Log log = Log.getLog(ExecuteToolHandler.class);
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
                    IStructuredSelection selectedObjects = new StructuredSelection(objects.toArray());
                    TaskConfigurationWizardDialog.openNewTaskDialog(
                        window,
                        selectedProject,
                        taskForObjs.getId(),
                        selectedObjects
                    );
                } else {
                    log.error("Couldn't apply requested task for the selected objects");
                }
            } else {
                log.error("Can't determine project for the selected objects");
            }
        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showError("Tool error", "Error executing tool '" + tool.getLabel() + "'", e);
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

}