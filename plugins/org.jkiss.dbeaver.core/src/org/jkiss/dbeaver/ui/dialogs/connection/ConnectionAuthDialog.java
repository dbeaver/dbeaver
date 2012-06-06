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

    public ConnectionAuthDialog(Shell parentShell, DataSourceDescriptor dataSource)
    {
        this(parentShell, dataSource, null);
    }

    public ConnectionAuthDialog(Shell parentShell, DataSourceDescriptor dataSource, DBWHandlerConfiguration networkHandler)
    {
        super(parentShell,
            networkHandler != null ?
                    "Specify password for " + networkHandler.getTitle() :
                    "'" + dataSource.getName() + CoreMessages.dialog_connection_auth_title, //$NON-NLS-1$
            dataSource.getDriver().getIcon());

        this.dataSource = dataSource;
        this.networkHandler = networkHandler;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Control area = super.createDialogArea(parent);
        if (networkHandler != null) {
            usernameText.setText(CommonUtils.getString(networkHandler.getUserName()));
        } else {
            usernameText.setText(CommonUtils.getString(dataSource.getConnectionInfo().getUserName()));
        }
        if (networkHandler != null) {
            passwordText.setText(CommonUtils.getString(networkHandler.getPassword()));
        } else {
            passwordText.setText(CommonUtils.getString(dataSource.getConnectionInfo().getUserPassword()));
        }

        return area;
    }

    @Override
    protected void okPressed() {
        if (networkHandler != null) {
            networkHandler.setUserName(usernameText.getText());
            networkHandler.setPassword(passwordText.getText());
            networkHandler.setSavePassword(savePasswordCheck.getSelection());
        } else {
            dataSource.getConnectionInfo().setUserName(usernameText.getText());
            dataSource.getConnectionInfo().setUserPassword(passwordText.getText());
            dataSource.setSavePassword(savePasswordCheck.getSelection());
        }

        super.okPressed();
    }

}
