/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

/**
 * Base authentication dialog
 */
public class BaseAuthDialog extends BaseDialog
{

    public static class AuthInfo {
        public String userName;
        public String userPassword;
        public boolean savePassword;
    }

    private AuthInfo authInfo = new AuthInfo();

    private Text usernameText;
    private Text passwordText;
    private Button savePasswordCheck;

    public BaseAuthDialog(Shell parentShell, String title, Image icon)
    {
        super(parentShell, title, icon);
    }

    public AuthInfo getAuthInfo()
    {
        return authInfo;
    }

    public String getUserName() {
        return authInfo.userName;
    }

    public void setUserName(String userName) {
        this.authInfo.userName = userName;
    }

    public String getUserPassword() {
        return authInfo.userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.authInfo.userPassword = userPassword;
    }

    public boolean isSavePassword() {
        return authInfo.savePassword;
    }

    public void setSavePassword(boolean savePassword) {
        this.authInfo.savePassword = savePassword;
    }

    @Override
    protected Control createDialogArea(Composite parent)
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

            Label usernameLabel = new Label(credGroup, SWT.NONE);
            usernameLabel.setText(CoreMessages.dialog_connection_auth_label_username);
            usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            usernameText = new Text(credGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 120;
            //gd.horizontalSpan = 3;
            usernameText.setLayoutData(gd);
            if (authInfo.userName != null) {
                usernameText.setText(authInfo.userName);
            }

            Label passwordLabel = new Label(credGroup, SWT.NONE);
            passwordLabel.setText(CoreMessages.dialog_connection_auth_label_password);
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = new Text(credGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            passwordText.setLayoutData(gd);
            if (authInfo.userPassword != null) {
                passwordText.setText(authInfo.userPassword);
            }
        }

        savePasswordCheck = new Button(addrGroup, SWT.CHECK);
        savePasswordCheck.setText(CoreMessages.dialog_connection_auth_checkbox_save_password);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        savePasswordCheck.setLayoutData(gd);
        savePasswordCheck.setSelection(authInfo.savePassword);

        if (!CommonUtils.isEmpty(usernameText.getText())) {
            passwordText.setFocus();
        }

        return addrGroup;
    }

    @Override
    protected void okPressed() {
        authInfo.userName = usernameText.getText();
        authInfo.userPassword = passwordText.getText();
        authInfo.savePassword = savePasswordCheck.getSelection();

        super.okPressed();
    }

}
