/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

/**
 * PostgreCreateDatabaseDialog
 */
public class PostgreCreateRoleDialog extends BaseDialog
{
    private final PostgreDatabase database;

    private String name;
    private String password;
    private boolean isUser = true;

    public PostgreCreateRoleDialog(Shell parentShell, PostgreDatabase database) {
        super(parentShell, PostgreMessages.dialog_create_role_title, null);
        this.database = database;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        final Composite composite = super.createDialogArea(parent);

        final Composite groupGeneral = UIUtils.createControlGroup(composite, PostgreMessages.dialog_create_role_group_general, 2, GridData.FILL_HORIZONTAL, SWT.NONE);

        final Text nameText = UIUtils.createLabelText(groupGeneral, PostgreMessages.dialog_create_role_label_role_name, ""); //$NON-NLS-2$
        nameText.addModifyListener(e -> {
            name = nameText.getText();
            getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
        });

        final Text passwordText = UIUtils.createLabelText(groupGeneral, PostgreMessages.dialog_create_role_label_user_password, "", SWT.BORDER | SWT.PASSWORD); //$NON-NLS-2$
        passwordText.addModifyListener(e -> {
            password = passwordText.getText();
        });

        Button isUserCheck = UIUtils.createCheckbox(groupGeneral, PostgreMessages.dialog_create_role_label_user_role, null, true, 2);
        isUserCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                isUser = isUserCheck.getSelection();
                passwordText.setEnabled(isUser);
            }
        });

        return composite;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public boolean isUser() {
        return isUser;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
}
