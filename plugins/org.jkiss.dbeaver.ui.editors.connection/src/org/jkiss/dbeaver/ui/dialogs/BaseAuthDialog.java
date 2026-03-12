/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.registry.ApplicationPolicyProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

/**
 * Base authentication dialog
 */
public class BaseAuthDialog extends BaseDialog implements BlockingPopupDialog {

    private String userNameLabel = UIConnectionMessages.dialog_connection_auth_label_username;
    private String passwordLabel = UIConnectionMessages.dialog_connection_auth_label_password;
    private final boolean passwordOnly;
    private final boolean showSavePassword;
    private final DBPAuthInfo authInfo = new DBPAuthInfo();
    private String savePasswordText;
    private String savePasswordToolTipText;
    private String description;

    protected Text usernameText;
    protected Text passwordText;
    private Button savePasswordCheck;

    public BaseAuthDialog(Shell parentShell, String title, boolean passwordOnly, boolean showSavePassword) {
        super(parentShell, title, DBIcon.TREE_USER);
        this.passwordOnly = passwordOnly;
        this.showSavePassword = showSavePassword &&
            !ApplicationPolicyProvider.getInstance()
                .isPolicyEnabled(ApplicationPolicyProvider.POLICY_CREDENTIALS_EDIT);
    }

//    @Override
//    protected IDialogSettings getDialogBoundsSettings() {
//        return UIUtils.getDialogSettings(DIALOG_ID);
//    }

    public void setUserNameLabel(String userNameLabel) {
        this.userNameLabel = userNameLabel;
    }

    public void setPasswordLabel(String passwordLabel) {
        this.passwordLabel = passwordLabel;
    }

    public DBPAuthInfo getAuthInfo() {
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

    public String getSavePasswordText() {
        return savePasswordText;
    }

    public void setSavePasswordText(String text) {
        this.savePasswordText = text;
    }

    public String getSavePasswordToolTipText() {
        return savePasswordToolTipText;
    }

    public void setSavePasswordToolTipText(String text) {
        this.savePasswordToolTipText = text;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite addrGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        if (CommonUtils.isNotEmpty(description)) {
            UIUtils.createInfoLabel(addrGroup, description);
        }

        {
            Composite credGroup = UIUtils.createTitledComposite(addrGroup, UIConnectionMessages.dialog_connection_auth_group_user_cridentials, 2, GridData.FILL_BOTH);
            if (!passwordOnly) {
                Label usernameLabel = new Label(credGroup, SWT.NONE);
                usernameLabel.setText(this.userNameLabel);
                usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

                usernameText = new Text(credGroup, SWT.BORDER);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.grabExcessHorizontalSpace = true;
                gd.widthHint = 200;
                //gd.horizontalSpan = 3;
                usernameText.setLayoutData(gd);
                if (authInfo.getUserName() != null) {
                    usernameText.setText(authInfo.getUserName());
                }
            }

            Label passwordLabel = new Label(credGroup, SWT.NONE);
            passwordLabel.setText(this.passwordLabel);
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            passwordText = new Text(credGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            passwordText.setLayoutData(gd);
            if (authInfo.getUserPassword() != null && authInfo.isSavePassword()) {
                passwordText.setText(authInfo.getUserPassword());
            }
        }

        if (showSavePassword) {
            savePasswordCheck = new Button(addrGroup, SWT.CHECK);
            savePasswordCheck.setEnabled(showSavePassword);
            savePasswordCheck.setText(CommonUtils.toString(savePasswordText, UIConnectionMessages.dialog_connection_auth_checkbox_save_password));
            savePasswordCheck.setToolTipText(savePasswordToolTipText);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            savePasswordCheck.setLayoutData(gd);
            savePasswordCheck.setSelection(authInfo.isSavePassword());
        }

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
        if (showSavePassword) {
            authInfo.setSavePassword(savePasswordCheck.getSelection());
        }

        super.okPressed();
    }

}
