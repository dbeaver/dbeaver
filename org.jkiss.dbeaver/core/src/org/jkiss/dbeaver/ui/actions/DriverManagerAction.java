package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverManagerDialog;


public class DriverManagerAction extends Action implements IViewActionDelegate
{

    private IWorkbenchWindow window;

    public DriverManagerAction()
    {
        // The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_EDIT_DRIVERS);
        // Associate the action with a pre-defined command, to allow key bindings.
        setActionDefinitionId(ICommandIds.CMD_EDIT_DRIVERS);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/driver_manager.png"));
    }

    public DriverManagerAction(IWorkbenchWindow window)
    {
        this();
        this.window = window;
        setText("Driver Manager");
    }

    public void run()
    {
        if (window != null) {
            DriverManagerDialog dialog = new DriverManagerDialog(window.getShell());
            dialog.open();
        }
    }

    public void init(IViewPart view)
    {
        window = view.getViewSite().getWorkbenchWindow();
    }

    public void run(IAction action)
    {
        this.run();
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
    }
}