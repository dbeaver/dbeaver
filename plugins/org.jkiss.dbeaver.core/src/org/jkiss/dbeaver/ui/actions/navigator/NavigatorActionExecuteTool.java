/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tools.ToolDescriptor;
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
            executeTool(DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActivePart(), selectedObjects);
        }
    }

    private void executeTool(IWorkbenchPart part, Collection<DBSObject> objects)
    {
        try {
            IExternalTool toolInstance = tool.createTool();
            toolInstance.execute(window, part, objects);
        } catch (Throwable e) {
            UIUtils.showErrorDialog(window.getShell(), "Tool error", "Error executing tool '" + tool.getLabel() + "'", e);
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection)
    {
        this.selection = selection;
    }

}