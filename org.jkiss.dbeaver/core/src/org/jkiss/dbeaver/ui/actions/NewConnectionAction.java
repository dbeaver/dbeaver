package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionDialog;


public class NewConnectionAction implements IWorkbenchWindowActionDelegate
{

    private IWorkbenchWindow window;

    public void run(IAction action)
    {
        if (window != null) {
            NewConnectionDialog dialog = new NewConnectionDialog(window);
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
