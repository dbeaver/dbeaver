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
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

public class EditConnectionAction extends DataSourceAction
{
    //private IWorkbenchWindow window;
    //private ISelection selection;

/*
    public EditConnectionAction()
    {
        // The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_EDIT_CONNECTION);
        // Associate the action with a pre-defined command, to allow key bindings.
        //setActionDefinitionId(ICommandIds.CMD_EDIT_CONNECTION);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/tree/edit_connection.png"));
        setText("Edit Connection");
    }
*/

    public void run(IAction action)
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(false);
        if (dataSourceContainer instanceof DataSourceDescriptor) {
            EditConnectionDialog dialog = new EditConnectionDialog(
                getWindow(),
                (DataSourceDescriptor)dataSourceContainer);
            dialog.open();
        }
    }

}
