/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerLogin;
import org.jkiss.dbeaver.ext.mssql.ui.SQLServerUIMessages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class SQLServerLoginConfigurator implements DBEObjectConfigurator<SQLServerLogin> {

    @Override
    public SQLServerLogin configureObject(DBRProgressMonitor monitor, Object container, SQLServerLogin login) {
        return new UITask<SQLServerLogin>() {
            @Override
            protected SQLServerLogin runTask() {
                SQLServerCreateLoginDialog dialog = new SQLServerCreateLoginDialog(UIUtils.getActiveWorkbenchShell());
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                login.setLoginName(dialog.getName());
                login.setPassword(dialog.getPassword());
                return login;
            }
        }.execute();
    }

    class SQLServerCreateLoginDialog extends BaseDialog {

        private String name;
        private String password;

        SQLServerCreateLoginDialog(Shell parentShell) {
            super(parentShell, SQLServerUIMessages.dialog_create_login_shell_title, null);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite composite = super.createDialogArea(parent);

            final Composite groupGeneral = UIUtils.createControlGroup(composite, SQLServerUIMessages.dialog_create_login_group, 2, GridData.FILL_HORIZONTAL, SWT.NONE);

            final Text nameText = UIUtils.createLabelText(groupGeneral, SQLServerUIMessages.dialog_create_login_label_name, ""); //$NON-NLS-2$
            nameText.addModifyListener(e -> {
                name = nameText.getText().trim();
                getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty() && password != null && !password.isEmpty());
            });

            final Text passwordText = UIUtils.createLabelText(groupGeneral, SQLServerUIMessages.dialog_create_login_label_password, "", SWT.BORDER | SWT.PASSWORD); //$NON-NLS-2$
            passwordText.addModifyListener(e -> {
                password = passwordText.getText();
                getButton(IDialogConstants.OK_ID).setEnabled(!password.isEmpty() && !name.isEmpty());
            });

            return composite;
        }

        String getName() {
            return name;
        }

        String getPassword() {
            return password;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            super.createButtonsForButtonBar(parent);
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        }
    }
}
