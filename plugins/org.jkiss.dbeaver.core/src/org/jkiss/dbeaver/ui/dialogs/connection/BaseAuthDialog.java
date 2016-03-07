/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

/**
 * Base authentication dialog
 */
public class BaseAuthDialog extends BaseDialog
{
    private boolean passwordOnly;
    private DBAAuthInfo authInfo = new DBAAuthInfo();

    private Text usernameText;
    private Text passwordText;
    private Button savePasswordCheck;

    public BaseAuthDialog(Shell parentShell, String title, boolean passwordOnly)
    {
        super(parentShell, title, DBIcon.CONNECTIONS);
        this.passwordOnly = passwordOnly;
    }

    public DBAAuthInfo getAuthInfo()
    {
        return authInfo;
    }

    public String getUserName() {
        return authInfo.getUserName();
    }

    public void setUserName(String userName) {
        this.authInfo.setUserName(userName);
    }

    public String getUserPassword() {
        return authInfo.getUserPassword();
    }

    public void setUserPassword(String userPassword) {
        this.authInfo.setUserPassword(userPassword);
    }

    public boolean isSavePassword() {
        return authInfo.isSavePassword();
    }

    public void setSavePassword(boolean savePassword) {
        this.authInfo.setSavePassword(savePassword);
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite addrGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
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
            if (!passwordOnly) {
                Label usernameLabel = new Label(credGroup, SWT.NONE);
                usernameLabel.setText(CoreMessages.dialog_connection_auth_label_username);
                usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

                usernameText = new Text(credGroup, SWT.BORDER);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.grabExcessHorizontalSpace = true;
                gd.widthHint = 120;
                //gd.horizontalSpan = 3;
                usernameText.setLayoutData(gd);
                if (authInfo.getUserName() != null) {
                    usernameText.setText(authInfo.getUserName());
                }
            }

            Label passwordLabel = new Label(credGroup, SWT.NONE);
            passwordLabel.setText(CoreMessages.dialog_connection_auth_label_password);
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = new Text(credGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            passwordText.setLayoutData(gd);
            if (authInfo.getUserPassword() != null) {
                passwordText.setText(authInfo.getUserPassword());
            }
        }

        savePasswordCheck = new Button(addrGroup, SWT.CHECK);
        savePasswordCheck.setText(CoreMessages.dialog_connection_auth_checkbox_save_password);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        savePasswordCheck.setLayoutData(gd);
        savePasswordCheck.setSelection(authInfo.isSavePassword());

        if (passwordOnly || !CommonUtils.isEmpty(usernameText.getText())) {
            passwordText.setFocus();
        }

        return addrGroup;
    }

    @Override
    protected void okPressed() {
        if (!passwordOnly) {
            authInfo.setUserName(usernameText.getText());
        }
        authInfo.setUserPassword(passwordText.getText());
        authInfo.setSavePassword(savePasswordCheck.getSelection());

        super.okPressed();
    }

}
