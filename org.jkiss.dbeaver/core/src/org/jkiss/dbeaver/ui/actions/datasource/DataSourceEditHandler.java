/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionWizard;

public class DataSourceEditHandler extends DataSourceHandler
{
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(event, false, false);
        if (dataSourceContainer != null) {
            execute(HandlerUtil.getActiveWorkbenchWindow(event), dataSourceContainer);
        }
        return null;
    }

    public static void execute(IWorkbenchWindow window, final DBSDataSourceContainer dataSourceContainer) {
        if (dataSourceContainer instanceof DataSourceDescriptor) {
            ConnectionDialog dialog = new ConnectionDialog(
                window,
                new EditConnectionWizard((DataSourceDescriptor)dataSourceContainer));
            dialog.open();
        }
    }

}