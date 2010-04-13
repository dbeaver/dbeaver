/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionDialog;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;

public class EditConnectionAction extends Action implements IObjectActionDelegate
{

    private IWorkbenchWindow window;
    private ISelection selection;

    public EditConnectionAction()
    {
        // The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_EDIT_CONNECTION);
        // Associate the action with a pre-defined command, to allow key bindings.
        //setActionDefinitionId(ICommandIds.CMD_EDIT_CONNECTION);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/tree/edit_connection.png"));
        setText("Edit Connection");
    }

    public void run()
    {
        if (window != null && selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object editElement = structSelection.getFirstElement();
            if (editElement instanceof DataSourceDescriptor) {
                EditConnectionDialog dialog = new EditConnectionDialog(window, (DataSourceDescriptor)editElement);
                dialog.open();
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
