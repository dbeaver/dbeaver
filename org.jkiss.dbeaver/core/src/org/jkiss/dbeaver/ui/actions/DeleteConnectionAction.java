/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.utils.DBeaverUtils;

public class DeleteConnectionAction implements IObjectActionDelegate
{

    private IWorkbenchWindow window;
    private ISelection selection;

    public void run()
    {
        if (window != null && selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object editElement = structSelection.getFirstElement();
            if (editElement instanceof DataSourceDescriptor) {
                DataSourceDescriptor dataSource = (DataSourceDescriptor)editElement;
                if (UIUtils.confirmAction(
                    window.getShell(),
                    "Delete connection",
                    "Are you sure you want to delete connection '" + dataSource.getName() + "'?"))
                {
                    // Then delete it
                    if (dataSource.isConnected()) {
                        try {
                            dataSource.disconnect(this);
                        }
                        catch (DBException ex) {
                            DBeaverUtils.showErrorDialog(window.getShell(), "Disconnect", "Can't disconnect from '" + dataSource.getName() + "'", ex);
                        }
                    }
                    DataSourceRegistry.getDefault().removeDataSource(dataSource);
                }
            }
        }
    }

    public void run(IAction action)
    {
        this.run();
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
        this.selection = selection;
    }

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        window = targetPart.getSite().getWorkbenchWindow();
	}
}