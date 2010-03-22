package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionDialog;


public class NewConnectionAction extends Action implements IViewActionDelegate
{

    private IWorkbenchWindow window;

    public NewConnectionAction()
    {
        // The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_NEW_CONNECTION);
        // Associate the action with a pre-defined command, to allow key bindings.
        setActionDefinitionId(ICommandIds.CMD_NEW_CONNECTION);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/database_connect.png"));
    }

    public NewConnectionAction(IWorkbenchWindow window)
    {
        this();
        this.window = window;
        setText("New Connection");
    }

    public void run()
    {
        if (window != null) {
            NewConnectionDialog dialog = new NewConnectionDialog(window);
            dialog.open();
        }
/*		if(window != null) {	
			try {
				window.getActivePage().showView(ConsoleView.ID, Integer.toString(instanceNum++), IWorkbenchPage.VIEW_ACTIVATE);
			} catch (PartInitException e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" + e.getMessage());
			}
		}
*/
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
