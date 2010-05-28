/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionDialog;

public class EditConnectionAction extends DataSourceAction
{
    public EditConnectionAction() {
    }

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
