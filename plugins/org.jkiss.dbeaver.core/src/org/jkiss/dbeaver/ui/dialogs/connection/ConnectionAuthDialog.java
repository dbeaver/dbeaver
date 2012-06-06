/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * Connection auth dialog
 */
public class ConnectionAuthDialog extends BaseAuthDialog
{
    private DataSourceDescriptor dataSource;
    private DBWHandlerConfiguration networkHandler;

    public ConnectionAuthDialog(Shell parentShell, DataSourceDescriptor dataSource, DBWHandlerConfiguration networkHandler)
    {
        super(parentShell,
            networkHandler != null ?
                    "Specify password for " + networkHandler.getTitle() :
                    "'" + dataSource.getName() + CoreMessages.dialog_connection_auth_title, //$NON-NLS-1$
            dataSource.getDriver().getIcon());

        this.dataSource = dataSource;
        this.networkHandler = networkHandler;

        if (networkHandler != null) {
            setUserName(CommonUtils.getString(networkHandler.getUserName()));
            setUserPassword(CommonUtils.getString(networkHandler.getPassword()));
        } else {
            setUserName(CommonUtils.getString(dataSource.getConnectionInfo().getUserName()));
            setUserPassword(CommonUtils.getString(dataSource.getConnectionInfo().getUserPassword()));
        }
    }

    @Override
    protected void okPressed() {
        super.okPressed();

        if (networkHandler != null) {
            networkHandler.setUserName(getUserName());
            networkHandler.setPassword(getUserPassword());
            networkHandler.setSavePassword(isSavePassword());
        } else {
            dataSource.getConnectionInfo().setUserName(getUserName());
            dataSource.getConnectionInfo().setUserPassword(getUserPassword());
            dataSource.setSavePassword(isSavePassword());
        }
    }

}
