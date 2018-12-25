/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tools.ToolDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.Collection;
import java.util.List;

public class NavigatorActionExecuteTool implements IActionDelegate
{
    private IWorkbenchWindow window;
    private ToolDescriptor tool;
    private ISelection selection;

    public NavigatorActionExecuteTool(IWorkbenchWindow window, ToolDescriptor tool)
    {
        this.window = window;
        this.tool = tool;
    }

    @Override
    public void run(IAction action)
    {
        if (!selection.isEmpty()) {
            List<DBSObject> selectedObjects = NavigatorUtils.getSelectedObjects(selection);
            executeTool(UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart(), selectedObjects);
        }
    }

    private void executeTool(IWorkbenchPart part, Collection<DBSObject> objects)
    {
        try {
            IExternalTool toolInstance = tool.createTool();
            toolInstance.execute(window, part, objects);
        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showError("Tool error", "Error executing tool '" + tool.getLabel() + "'", e);
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection)
    {
        this.selection = selection;
    }

}