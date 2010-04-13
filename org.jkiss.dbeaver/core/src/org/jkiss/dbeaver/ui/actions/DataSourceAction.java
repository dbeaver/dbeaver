package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.ext.ui.IDataSourceUser;
import org.jkiss.dbeaver.ui.dialogs.connection.SelectDataSourceDialog;
import org.jkiss.dbeaver.DBException;

/**
 * DataSource action
 */
public abstract class DataSourceAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
    private ISelection selection;
    private IWorkbenchPart activePart;

	/**
	 * The constructor.
	 */
	public DataSourceAction() {
	}

    public IWorkbenchWindow getWindow() {
        return window;
    }

    public ISelection getSelection() {
        return selection;
    }

    public IWorkbenchPart getActivePart() {
        return activePart;
    }

    public abstract void run(IAction action);

    protected void updateAction(IAction action)
    {

    }

	/**
	 * Selection in the workbench has been changed. We
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after
	 * the delegate has been created.
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
        if (window != null && this.window.getActivePage() != null) {
            this.activePart = this.window.getActivePage().getActivePart();
        }
        this.updateAction(action);
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
        this.window = null;
        this.selection = null;
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

    protected DBSDataSourceContainer getDataSourceContainer()
    {
        if (activePart instanceof IDataSourceUser) {
            return ((IDataSourceUser)activePart).getDataSource().getContainer();
        }
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object editElement = structSelection.getFirstElement();
            if (editElement instanceof DBSDataSourceContainer) {
                return (DBSDataSourceContainer)editElement;
            } else if (editElement instanceof DBSObject) {
                return ((DBSObject)editElement).getDataSource().getContainer();
            }
        }
        if (window != null) {
            return SelectDataSourceDialog.selectDataSource(window.getShell());
        }
        return null;
    }

    protected DBPDataSource getDataSource()
    {
        if (activePart instanceof IDataSourceUser) {
            return ((IDataSourceUser)activePart).getDataSource();
        }
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object editElement = structSelection.getFirstElement();
            if (editElement instanceof DBSObject) {
                return ((DBSObject)editElement).getDataSource();
            }
        }
        return null;
    }

    protected DBCSession getSession() throws DBException
    {
        DBCSession session = null;
        if (activePart instanceof IDataSourceUser) {
            session = ((IDataSourceUser)activePart).getSession();
        }
        if (session == null) {
            DBPDataSource dataSource = getDataSource();
            if (dataSource != null) {
                session = dataSource.getSession(false);
            }
        }
        return session;
    }

}