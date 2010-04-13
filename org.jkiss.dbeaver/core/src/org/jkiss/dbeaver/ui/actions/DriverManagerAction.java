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
import org.jkiss.dbeaver.ui.dialogs.driver.DriverManagerDialog;


public class DriverManagerAction implements IWorkbenchWindowActionDelegate
{
    private IWorkbenchWindow window;

/*
    public DriverManagerAction()
    {
        // The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_EDIT_DRIVERS);
        // Associate the action with a pre-defined command, to allow key bindings.
        setActionDefinitionId(ICommandIds.CMD_EDIT_DRIVERS);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/driver_manager.png"));
    }
*/

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