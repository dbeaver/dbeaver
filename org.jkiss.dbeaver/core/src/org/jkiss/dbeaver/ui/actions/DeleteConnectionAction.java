/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ViewUtils;

public class DeleteConnectionAction extends DataSourceAction
{
    private IWorkbenchWindow window;
    private ISelection selection;

    public void run()
    {
        if (window != null && selection instanceof IStructuredSelection) {
            DBSObject editElement = ViewUtils.getSelectedObject((IStructuredSelection)selection);
            if (editElement instanceof DataSourceDescriptor) {
                DataSourceDescriptor dataSource = (DataSourceDescriptor)editElement;
                if (UIUtils.confirmAction(
                    window.getShell(),
                    "Delete connection",
                    "Are you sure you want to delete connection '" + dataSource.getName() + "'?"))
                {
                    // Then delete it
                    if (dataSource.isConnected()) {
                        DisconnectAction.execute(dataSource);
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