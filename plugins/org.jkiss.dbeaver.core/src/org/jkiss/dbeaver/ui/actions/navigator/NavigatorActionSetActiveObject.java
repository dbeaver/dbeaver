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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.lang.reflect.InvocationTargetException;

public class NavigatorActionSetActiveObject implements IActionDelegate
{
    private ISelection selection;

    @Override
    public void run(IAction action)
    {
        if (selection instanceof IStructuredSelection) {
            DBNNode selectedNode = NavigatorUtils.getSelectedNode(selection);
            if (selectedNode instanceof DBNDatabaseNode) {
                final DBNDatabaseNode databaseNode = (DBNDatabaseNode)selectedNode;
                final DBSObjectSelector activeContainer = DBUtils.getParentAdapter(
                    DBSObjectSelector.class, databaseNode.getObject());
                if (activeContainer != null) {
                    TasksJob.runTask("Select active object", new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException {
                            try {
                                activeContainer.setDefaultObject(monitor, databaseNode.getObject());
                            } catch (DBException e) {
                                throw new InvocationTargetException(e);
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection)
    {
        this.selection = selection;
    }

}
