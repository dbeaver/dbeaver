/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSecurity;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

/**
 * Database native auth model config
 */
public class DatabaseNativeAuthModelConfigurator implements IObjectPropertyConfigurator<DBPDataSourceContainer> {

    protected Text usernameText;
    protected Text passwordText;
    protected Button savePasswordCheck;
    protected ToolBar userManagementToolbar;

    protected DBPDataSourceContainer dataSource;

    @Override
    public void createControl(Composite authPanel, Runnable propertyChangeListener) {
        Label usernameLabel = UIUtils.createLabel(authPanel, UIConnectionMessages.dialog_connection_auth_label_username);
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        createUserNameControls(authPanel, propertyChangeListener);

        createPasswordControls(authPanel, propertyChangeListener);
    }

    protected void createUserNameControls(Composite authPanel, Runnable propertyChangeListener) {
        int fontHeight = UIUtils.getFontHeight(authPanel);

        usernameText = new Text(authPanel, SWT.BORDER);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = fontHeight * 20;
        usernameText.setLayoutData(gd);
        usernameText.addModifyListener(e -> propertyChangeListener.run());
    }

    @Override
    public void loadSettings(DBPDataSourceContainer dataSource) {
        this.dataSource = dataSource;

        if (this.usernameText != null) {
            this.usernameText.setText(CommonUtils.notEmpty(dataSource.getConnectionConfiguration().getUserName()));
        }
        if (this.passwordText != null) {
            this.passwordText.setText(CommonUtils.notEmpty(dataSource.getConnectionConfiguration().getUserPassword()));
            this.savePasswordCheck.setSelection(dataSource.isSavePassword());
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        if (this.usernameText != null) {
            dataSource.getConnectionConfiguration().setUserName(this.usernameText.getText());
        }
        if (this.passwordText != null) {
            dataSource.getConnectionConfiguration().setUserPassword(this.passwordText.getText());
            dataSource.setSavePassword(this.savePasswordCheck.getSelection());
        }
    }

    @Override
    public void resetSettings(DBPDataSourceContainer dataSource) {
        loadSettings(dataSource);
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    protected Text createPasswordText(Composite parent, String label) {
        if (label != null) {
            UIUtils.createControlLabel(parent, label);
        }
        Composite ph = UIUtils.createPlaceholder(parent, 1);
        ph.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        passwordText = new Text(ph, SWT.BORDER | SWT.PASSWORD);
        passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return passwordText;
    }

    protected void createPasswordControls(Composite parent, Runnable propertyChangeListener) {
        Label passwordLabel = UIUtils.createLabel(parent, UIConnectionMessages.dialog_connection_auth_label_password);
        passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        Composite passPH = UIUtils.createComposite(parent, 2);
        passPH.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createPasswordText(passPH, null);
        passwordText.addModifyListener(e -> propertyChangeListener.run());

        // We don't support password preview in standard project secure storage (as we need password encryption)
        UIServiceSecurity serviceSecurity = DBWorkbench.getService(UIServiceSecurity.class);
        boolean supportsPasswordView = serviceSecurity != null;

        Composite panel = UIUtils.createComposite(passPH, supportsPasswordView ? 2 : 1);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        panel.setLayoutData(gd);

        savePasswordCheck = UIUtils.createCheckbox(panel,
            UIConnectionMessages.dialog_connection_wizard_final_checkbox_save_password_locally,
            dataSource == null || dataSource.isSavePassword());
        savePasswordCheck.setToolTipText(UIConnectionMessages.dialog_connection_wizard_final_checkbox_save_password_locally);
        //savePasswordCheck.setLayoutData(gd);

        if (supportsPasswordView) {
            userManagementToolbar = new ToolBar(panel, SWT.HORIZONTAL);
            ToolItem showPasswordLabel = new ToolItem(userManagementToolbar, SWT.NONE);
            showPasswordLabel.setToolTipText(UIConnectionMessages.dialog_connection_auth_label_show_password);
            showPasswordLabel.setImage(DBeaverIcons.getImage(UIIcon.SHOW_ALL_DETAILS));
            showPasswordLabel.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showPasswordText(serviceSecurity);
                }
            });
        }

    }

    private void showPasswordText(UIServiceSecurity serviceSecurity) {
        Composite passContainer = passwordText.getParent();
        boolean passHidden = (passwordText.getStyle() & SWT.PASSWORD) == SWT.PASSWORD;
        if (passHidden) {
            if (!serviceSecurity.validatePassword(
                dataSource.getProject().getSecureStorage(),
                "Enter project password",
                "Enter project master password to unlock connection password view",
                true))
            {
                return;
            }
        }

        Object layoutData = passwordText.getLayoutData();
        String curValue = passwordText.getText();
        passwordText.dispose();

        if (passHidden) {
            passwordText = new Text(passContainer, SWT.BORDER);
        } else {
            passwordText = new Text(passContainer, SWT.PASSWORD | SWT.BORDER);
        }
        passwordText.setLayoutData(layoutData);
        passwordText.setText(curValue);
        passContainer.layout(true, true);
    }

}
