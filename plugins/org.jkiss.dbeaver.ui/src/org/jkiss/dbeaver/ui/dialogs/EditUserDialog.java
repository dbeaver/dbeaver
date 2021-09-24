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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.utils.CommonUtils;

public class EditUserDialog extends BaseDialog {

    private String name;
    private String password;
    private String verifyText;

    public EditUserDialog(Shell parentShell, String title) {
        super(parentShell, title, null);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        final Composite composite = super.createDialogArea(parent);

        final Composite groupGeneral = UIUtils.createControlGroup(composite, UIMessages.dialogs_name_and_password_dialog_group_settings, 2, GridData.FILL_HORIZONTAL, SWT.NONE);

        final Text nameText = UIUtils.createLabelText(groupGeneral, UIMessages.dialogs_name_and_password_dialog_label_name, ""); //$NON-NLS-2$
        nameText.addModifyListener(e -> {
            name = nameText.getText().trim();
            updateButtons();
        });

        final Text passwordText = UIUtils.createLabelText(groupGeneral, UIMessages.dialogs_name_and_password_dialog_label_password, "", SWT.BORDER | SWT.PASSWORD); //$NON-NLS-2$
        passwordText.addModifyListener(e -> {
            password = passwordText.getText();
            updateButtons();
        });

        Text verifyPasswordText = UIUtils.createLabelText(groupGeneral, UIMessages.dialogs_name_and_password_dialog_label_verify_password, "", SWT.BORDER | SWT.PASSWORD);
        verifyPasswordText.addModifyListener(e -> {
            verifyText = verifyPasswordText.getText();
            updateButtons();
        });

        return composite;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    private void updateButtons() {
        getButton(IDialogConstants.OK_ID).setEnabled(
            CommonUtils.isNotEmpty(name) &&
                CommonUtils.isNotEmpty(password) &&
                CommonUtils.equalObjects(password, verifyText));
    }
}
