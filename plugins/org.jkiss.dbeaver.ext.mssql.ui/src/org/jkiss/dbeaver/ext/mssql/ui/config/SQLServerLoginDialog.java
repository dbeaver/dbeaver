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
package org.jkiss.dbeaver.ext.mssql.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerLogin;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerWindowsAccount;
import org.jkiss.dbeaver.ext.mssql.ui.SQLServerUIMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseTitleDialog;
import org.jkiss.dbeaver.ui.internal.UIMessages;

import java.util.List;

class SQLServerLoginDialog extends BaseTitleDialog {
    private final SQLServerLogin login;
    private final boolean windowsSupported;
    private final List<String> domains;
    private Combo authentication;
    private Composite windowsPanel;
    private Composite sqlPanel;
    private Combo domain;
    private Text account;
    private Text loginName;
    private Text password;
    private Text verifyPassword;
    private Text qualifiedName;

    SQLServerLoginDialog(
        @NotNull Shell shell,
        @NotNull SQLServerLogin login,
        boolean windowsSupported,
        @NotNull List<String> domains
    ) {
        super(shell, DBIcon.TREE_USER);
        this.login = login;
        this.windowsSupported = windowsSupported;
        this.domains = domains;
    }

    @Override
    protected void configureShell(@NotNull Shell shell) {
        super.configureShell(shell);
        shell.setText(SQLServerUIMessages.dialog_create_login_shell_title);
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite fields = UIUtils.createComposite(area, 2);
        fields.setLayoutData(new GridData(GridData.FILL_BOTH));
        authentication = UIUtils.createLabelCombo(fields, SQLServerUIMessages.dialog_create_login_authentication,
            SWT.DROP_DOWN | SWT.READ_ONLY);
        authentication.add(SQLServerUIMessages.dialog_create_login_sql_authentication);
        if (windowsSupported) {
            authentication.add(SQLServerUIMessages.dialog_create_login_windows_authentication);
        }
        authentication.select(0);
        authentication.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> updateAuthentication()));

        sqlPanel = UIUtils.createComposite(fields, 2);
        sqlPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        loginName = UIUtils.createLabelText(sqlPanel, UIMessages.dialogs_name_and_password_dialog_label_name, "");
        password = UIUtils.createLabelText(sqlPanel, UIMessages.dialogs_name_and_password_dialog_label_password,
            "", SWT.BORDER | SWT.PASSWORD);
        verifyPassword = UIUtils.createLabelText(sqlPanel, UIMessages.dialogs_name_and_password_dialog_label_verify_password,
            "", SWT.BORDER | SWT.PASSWORD);
        loginName.addModifyListener(event -> updateButtons());
        password.addModifyListener(event -> updateButtons());
        verifyPassword.addModifyListener(event -> updateButtons());

        windowsPanel = UIUtils.createComposite(fields, 2);
        windowsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        domain = UIUtils.createLabelCombo(windowsPanel, SQLServerUIMessages.dialog_create_login_domain, SWT.DROP_DOWN);
        domain.setItems(domains.toArray(String[]::new));
        domain.setToolTipText(SQLServerUIMessages.dialog_create_login_domain_hint);
        account = UIUtils.createLabelText(windowsPanel, SQLServerUIMessages.dialog_create_login_account, "");
        account.setMessage(SQLServerUIMessages.dialog_create_login_account_placeholder);
        qualifiedName = UIUtils.createLabelText(windowsPanel, SQLServerUIMessages.dialog_create_login_full_name,
            "", SWT.BORDER | SWT.READ_ONLY);
        domain.addModifyListener(event -> updateButtons());
        domain.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> updateButtons()));
        account.addModifyListener(event -> updateButtons());
        updateAuthentication();
        return area;
    }

    private boolean isWindows() {
        return authentication.getSelectionIndex() == 1;
    }

    private void updateAuthentication() {
        boolean windows = isWindows();
        windowsPanel.setVisible(windows);
        ((GridData) windowsPanel.getLayoutData()).exclude = !windows;
        sqlPanel.setVisible(!windows);
        ((GridData) sqlPanel.getLayoutData()).exclude = windows;
        setTitle(SQLServerUIMessages.dialog_create_login_shell_title);
        setMessage(windows ? SQLServerUIMessages.dialog_create_login_windows_hint : SQLServerUIMessages.dialog_create_login_sql_hint);
        updateButtons();
        windowsPanel.getParent().layout(true, true);
    }

    @Nullable
    private String validationMessage() {
        if (isWindows()) {
            String domainName = domain.getText().trim();
            String accountName = account.getText().trim();
            if (domainName.isEmpty() || accountName.isEmpty()) {
                return SQLServerUIMessages.dialog_create_login_enter_account;
            }
            if (domainName.contains("\\") || accountName.contains("\\") || getLoginName().length() > 128) {
                return SQLServerUIMessages.dialog_create_login_invalid_account;
            }
        } else if (loginName.getText().trim().isEmpty() || password.getText().isEmpty()) {
            return SQLServerUIMessages.dialog_create_login_enter_credentials;
        } else if (!password.getText().equals(verifyPassword.getText())) {
            return SQLServerUIMessages.dialog_create_login_password_mismatch;
        }
        return null;
    }

    private void updateButtons() {
        setErrorMessage(null);
        if (qualifiedName != null) {
            String domainName = domain.getText().trim();
            String accountName = account.getText().trim();
            qualifiedName.setText(domainName.isEmpty() ? accountName : domainName + "\\" + accountName);
        }
        if (getButton(IDialogConstants.OK_ID) != null) {
            String error = validationMessage();
            getButton(IDialogConstants.OK_ID).setEnabled(error == null);
            if ((isWindows() && !domain.getText().isEmpty() && !account.getText().isEmpty())
                || (!isWindows() && !verifyPassword.getText().isEmpty())) {
                setErrorMessage(error);
            }
        }
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateButtons();
    }

    @NotNull
    private String getLoginName() {
        return isWindows() ? domain.getText().trim() + "\\" + account.getText().trim() : loginName.getText().trim();
    }

    @Override
    protected void okPressed() {
        String error = validationMessage();
        if (error != null) {
            setErrorMessage(error);
            return;
        }
        if (isWindows()) {
            String name = getLoginName();
            try {
                SQLServerWindowsAccount.CheckResult result = UIUtils.runWithMonitor(
                    monitor -> SQLServerWindowsAccount.checkName(monitor, login.getDataSource(), name));
                if (result == null) {
                    return;
                }
                if (result != SQLServerWindowsAccount.CheckResult.AVAILABLE) {
                    setErrorMessage(NLS.bind(result == SQLServerWindowsAccount.CheckResult.LOGIN_EXISTS
                        ? SQLServerUIMessages.dialog_create_login_exists : SQLServerUIMessages.dialog_create_login_not_found, name));
                    return;
                }
            } catch (DBException e) {
                setErrorMessage(NLS.bind(SQLServerUIMessages.dialog_create_login_check_failed, e.getMessage()));
                return;
            }
        }
        login.setLoginName(getLoginName());
        login.setLoginType(isWindows() ? SQLServerLogin.LoginType.U : SQLServerLogin.LoginType.S);
        login.setPassword(isWindows() ? null : password.getText());
        super.okPressed();
    }
}
