/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;

/**
 * ConnectionAuthDialog
 */
public class ConnectionAuthDialog extends Dialog
{
    private DataSourceDescriptor dataSource;
    private Text usernameText;
    private Text passwordText;
    private Button savePasswordCheck;

    public ConnectionAuthDialog(Shell parentShell, DataSourceDescriptor dataSource)
    {
        super(parentShell);
        this.dataSource = dataSource;
    }

    protected Control createDialogArea(Composite parent)
    {
        Composite addrGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = 20;
        gl.marginWidth = 20;
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Label usernameLabel = new Label(addrGroup, SWT.NONE);
        usernameLabel.setText("Username:");
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        usernameText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        //gd.horizontalSpan = 3;
        usernameText.setLayoutData(gd);
        if (!CommonUtils.isEmpty(dataSource.getConnectionInfo().getUserName())) {
            usernameText.setText(dataSource.getConnectionInfo().getUserName());
        }

        Label passwordLabel = new Label(addrGroup, SWT.NONE);
        passwordLabel.setText("Password:");
        passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        passwordText = new Text(addrGroup, SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        passwordText.setLayoutData(gd);
        if (!CommonUtils.isEmpty(dataSource.getConnectionInfo().getUserPassword())) {
            passwordText.setText(dataSource.getConnectionInfo().getUserPassword());
        }

        savePasswordCheck = new Button(addrGroup, SWT.CHECK);
        savePasswordCheck.setText("Save password");
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        savePasswordCheck.setLayoutData(gd);

        return addrGroup;
    }

    protected void okPressed() {
        dataSource.getConnectionInfo().setUserName(usernameText.getText());
        dataSource.getConnectionInfo().setUserPassword(passwordText.getText());
        if (savePasswordCheck.getSelection()) {
            dataSource.setSavePassword(true);
        }

        super.okPressed();
    }

}
