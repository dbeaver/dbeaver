package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

public abstract class ConnectionAction extends Action implements IObjectActionDelegate
{

    private IWorkbenchWindow window;
    private ISelection selection;

    public void selectionChanged(IAction action, ISelection selection)
    {
        this.selection = selection;
    }

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        window = targetPart.getSite().getWorkbenchWindow();
	}

    public void run(IAction action)
    {
        this.run();
    }

    protected DBSDataSourceContainer getDataSourceContainer()
    {
        if (window != null && selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object editElement = structSelection.getFirstElement();
            if (editElement instanceof DBSDataSourceContainer) {
                return (DBSDataSourceContainer)editElement;
            }
        }
        return null;
    }
}