/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.ui.NavigatorUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;

public class NavigatorActionSetActiveObject implements IActionDelegate
{
    private ISelection selection;

    public void run(IAction action)
    {
        if (selection instanceof IStructuredSelection) {
            DBNNode selectedNode = NavigatorUtils.getSelectedNode(selection);
            if (selectedNode instanceof DBNDatabaseNode) {
                final DBNDatabaseNode databaseNode = (DBNDatabaseNode)selectedNode;
                if (databaseNode.getObject() instanceof DBSEntity) {
                    final DBSObjectSelector activeContainer = DBUtils.getParentAdapter(
                        DBSObjectSelector.class, databaseNode.getObject());
                    try {
                        DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                            public void run(DBRProgressMonitor monitor)
                                throws InvocationTargetException, InterruptedException
                            {
                                try {
                                    activeContainer.selectObject(monitor, databaseNode.getObject());
                                } catch (DBException e) {
                                    throw new InvocationTargetException(e);
                                }
                            }
                        });
                    } catch (InvocationTargetException e) {
                        UIUtils.showErrorDialog(null, "Select entity", "Can't change selected entity", e.getTargetException());
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }
            }
        }
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
        this.selection = selection;
    }

}