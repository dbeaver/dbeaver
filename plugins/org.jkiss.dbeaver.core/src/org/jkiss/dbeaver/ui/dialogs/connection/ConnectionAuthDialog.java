/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;

/**
 * ConnectionAuthDialog
 */
public class ConnectionAuthDialog extends Dialog
{
    private DataSourceDescriptor dataSource;
    private DBWHandlerConfiguration networkHandler;
    private Text usernameText;
    private Text passwordText;
    private Button savePasswordCheck;

    public ConnectionAuthDialog(Shell parentShell, DataSourceDescriptor dataSource)
    {
        this(parentShell, dataSource, null);
    }

    public ConnectionAuthDialog(Shell parentShell, DataSourceDescriptor dataSource, DBWHandlerConfiguration networkHandler)
    {
        super(parentShell);
        this.dataSource = dataSource;
        this.networkHandler = networkHandler;
    }

    protected boolean isResizable()
    {
        return true;
    }

    protected Control createDialogArea(Composite parent)
    {
        String title = networkHandler != null ?
            "Specify password for " + networkHandler.getTitle() :
            "'" + dataSource.getName() + CoreMessages.dialog_connection_auth_title; //$NON-NLS-1$
        getShell().setText(title);
        getShell().setImage(dataSource.getDriver().getIcon());

        Composite addrGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 20;
        gl.marginWidth = 20;
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        {
            Group credGroup = new Group(addrGroup, SWT.NONE);
            credGroup.setText(CoreMessages.dialog_connection_auth_group_user_cridentials);
            gl = new GridLayout(2, false);
            gl.marginHeight = 5;
            gl.marginWidth = 5;
            credGroup.setLayout(gl);
            gd = new GridData(GridData.FILL_BOTH);
            credGroup.setLayoutData(gd);

            Label usernameLabel = new Label(credGroup, SWT.NONE);
            usernameLabel.setText(CoreMessages.dialog_connection_auth_label_username);
            usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            usernameText = new Text(credGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 120;
            //gd.horizontalSpan = 3;
            usernameText.setLayoutData(gd);
            if (networkHandler != null) {
                usernameText.setText(CommonUtils.getString(networkHandler.getUserName()));
            } else {
                usernameText.setText(CommonUtils.getString(dataSource.getConnectionInfo().getUserName()));
            }

            Label passwordLabel = new Label(credGroup, SWT.NONE);
            passwordLabel.setText(CoreMessages.dialog_connection_auth_label_password);
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = new Text(credGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            passwordText.setLayoutData(gd);
            if (networkHandler != null) {
                passwordText.setText(CommonUtils.getString(networkHandler.getPassword()));
            } else {
                passwordText.setText(CommonUtils.getString(dataSource.getConnectionInfo().getUserPassword()));
            }
        }

        savePasswordCheck = new Button(addrGroup, SWT.CHECK);
        savePasswordCheck.setText(CoreMessages.dialog_connection_auth_checkbox_save_password);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        savePasswordCheck.setLayoutData(gd);

        passwordText.setFocus();

        return addrGroup;
    }

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
