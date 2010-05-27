/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverManagerDialog;


public class DriverManagerAction implements IWorkbenchWindowActionDelegate
{
    private IWorkbenchWindow window;

    public void run(IAction action)
    {
        if (window != null) {
            DriverManagerDialog dialog = new DriverManagerDialog(window.getShell());
            dialog.open();
        }
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
    }

    public void dispose() {

    }

    public void init(IWorkbenchWindow window) {
        this.window = window;
    }
}