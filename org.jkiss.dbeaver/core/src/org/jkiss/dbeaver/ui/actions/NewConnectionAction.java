/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionWizard;

public class NewConnectionAction implements IWorkbenchWindowActionDelegate
{

    private IWorkbenchWindow window;

    public void run(IAction action)
    {
        if (window != null) {
            ConnectionDialog dialog = new ConnectionDialog(window, new NewConnectionWizard(window));
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
