/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * Base authentication dialog
 */
public class BaseAuthDialog extends Dialog
{
    private String title;
    private Image icon;
    protected Text usernameText;
    protected Text passwordText;
    protected Button savePasswordCheck;

    public BaseAuthDialog(Shell parentShell, String title, Image icon)
    {
        super(parentShell);
        this.title = title;
        this.icon = icon;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(title);
        getShell().setImage(icon);

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

            Label passwordLabel = new Label(credGroup, SWT.NONE);
            passwordLabel.setText(CoreMessages.dialog_connection_auth_label_password);
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = new Text(credGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            passwordText.setLayoutData(gd);
        }

        savePasswordCheck = new Button(addrGroup, SWT.CHECK);
        savePasswordCheck.setText(CoreMessages.dialog_connection_auth_checkbox_save_password);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        savePasswordCheck.setLayoutData(gd);

        passwordText.setFocus();

        return addrGroup;
    }

    @Override
    protected void okPressed() {
        super.okPressed();
    }

}
