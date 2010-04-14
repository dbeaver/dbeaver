/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.ui.ICommandIds;

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
                MessageBox messageBox = new MessageBox(window.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
                messageBox.setMessage("Do you really want to delete connection '" + dataSource.getName() + "'?");
                messageBox.setText("Delete connection");
                int response = messageBox.open();
                if (response == SWT.YES) {
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