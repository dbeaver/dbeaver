/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.PlatformUI;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.utils.DBeaverUtils;

public class ReconnectAction extends DataSourceAction
{
    @Override
    protected void updateAction(IAction action) {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(false);
        action.setEnabled(dataSourceContainer != null && dataSourceContainer.isConnected());
    }

    public void run(IAction action)
    {
        Display.getCurrent().asyncExec(new Runnable() {
            public void run()
            {
                DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(false);
                if (dataSourceContainer != null && dataSourceContainer.isConnected()) {
                    try {
                        dataSourceContainer.invalidate(ReconnectAction.this);
                    }
                    catch (DBException ex) {
                        DBeaverUtils.showErrorDialog(
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                            "Disconnect", "Can't disconnect from '" + dataSourceContainer.getName() + "'", ex);
                    }
                }
            }
        });
    }

}