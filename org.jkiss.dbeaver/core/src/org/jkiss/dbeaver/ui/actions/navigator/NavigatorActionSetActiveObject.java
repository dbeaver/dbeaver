/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntitySelector;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.lang.reflect.InvocationTargetException;

public class NavigatorActionSetActiveObject implements IActionDelegate
{
    private ISelection selection;

    public void run(IAction action)
    {
        if (selection instanceof IStructuredSelection) {
            final DBNNode selectedNode = ViewUtils.getSelectedNode((IStructuredSelection) selection);
            if (selectedNode != null) {
                final DBSEntitySelector activeContainer = DBUtils.getParentAdapter(
                    DBSEntitySelector.class, selectedNode.getObject());
                DBeaverCore.getInstance().runAndWait(new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        try {
                            activeContainer.setActiveChild(monitor, selectedNode.getObject());
                        }
                        catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            }
        }
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
        this.selection = selection;
    }

}