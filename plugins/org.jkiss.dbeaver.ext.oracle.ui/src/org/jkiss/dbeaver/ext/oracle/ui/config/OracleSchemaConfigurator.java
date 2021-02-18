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
package org.jkiss.dbeaver.ext.oracle.ui.config;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleUser;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * OracleSchemaConfigurator
 */
public class OracleSchemaConfigurator implements DBEObjectConfigurator<OracleSchema> {

    @Override
    public OracleSchema configureObject(DBRProgressMonitor monitor, Object container, OracleSchema newSchema) {
        return new UITask<OracleSchema>() {
            @Override
            protected OracleSchema runTask() {
                NewUserDialog dialog = new NewUserDialog(UIUtils.getActiveWorkbenchShell(), (OracleDataSource) container);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                newSchema.setName(dialog.getUser().getName());
                newSchema.setUser(dialog.getUser());

                return newSchema;
            }
        }.execute();
    }

    static class NewUserDialog extends Dialog {

        private OracleUser user;
        private Text nameText;
        private Text passwordText;

        NewUserDialog(Shell parentShell, OracleDataSource dataSource)
        {
            super(parentShell);
            this.user = new OracleUser(dataSource);
        }

        OracleUser getUser()
        {
            return user;
        }

        @Override
        protected boolean isResizable()
        {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("Set schema/user properties");

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, "Schema/User Name", null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            passwordText = UIUtils.createLabelText(composite, "User Password", null, SWT.BORDER | SWT.PASSWORD);
            passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createInfoLabel(composite, "Creating a schema is the same as creating a user.\nYou need to specify a password.", GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed()
        {
            user.setName(DBObjectNameCaseTransformer.transformObjectName(user, nameText.getText().trim()));
            user.setPassword(passwordText.getText());
            super.okPressed();
        }

    }

}

