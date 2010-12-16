/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceConnectHandler;
import org.jkiss.dbeaver.ui.dialogs.search.FindObjectsDialog;

public class NavigatorHandlerObjectFind extends DataSourceHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(event, false, false);
        if (dataSourceContainer == null) {
            return null;
        }
        if (!dataSourceContainer.isConnected()) {
            DataSourceConnectHandler.execute(dataSourceContainer);
        }
        FindObjectsDialog dialog = new FindObjectsDialog(HandlerUtil.getActiveShell(event), dataSourceContainer.getDataSource());
        dialog.open();
        return null;
    }


}