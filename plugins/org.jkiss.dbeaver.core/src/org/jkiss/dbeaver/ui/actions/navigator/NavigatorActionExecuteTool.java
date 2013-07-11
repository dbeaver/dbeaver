/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.DataSourceToolDescriptor;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.dbeaver.ui.UIUtils;

public class NavigatorActionExecuteTool implements IActionDelegate
{
    private IWorkbenchWindow window;
    private DataSourceToolDescriptor tool;
    private ISelection selection;

    public NavigatorActionExecuteTool(IWorkbenchWindow window, DataSourceToolDescriptor tool)
    {
        this.window = window;
        this.tool = tool;
    }

    @Override
    public void run(IAction action)
    {
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof DBSWrapper) {
                executeTool(((DBSWrapper) element).getObject());
            } else if (element instanceof DBPObject) {
                executeTool((DBPObject) element);
            }
        }
    }

    private void executeTool(DBPObject object)
    {
        try {
            IExternalTool toolInstance = tool.createTool();
            toolInstance.execute(window, DBUtils.getPublicObject(object));
        } catch (DBException e) {
            UIUtils.showErrorDialog(window.getShell(), "Tool error", "Error executing tool '" + tool.getLabel() + "'", e);
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection)
    {
        this.selection = selection;
    }

}