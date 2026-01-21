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
package org.jkiss.dbeaver.ext.mssql.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDataSource;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class SQLServerCreateDatabaseDialog extends BaseDialog {
    private final SQLServerDataSource dataSource;

    private String name;

    public SQLServerCreateDatabaseDialog(Shell parentShell, SQLServerDataSource dataSource) {
        super(parentShell, SQLServerUIMessages.dialog_create_db_title, null);
        this.dataSource = dataSource;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite composite = super.createDialogArea(parent);
        Composite groupGeneral = UIUtils.createTitledComposite(composite, SQLServerUIMessages.dialog_create_db_group_general, 2, GridData.FILL_HORIZONTAL);

        final Text nameText = UIUtils.createLabelText(groupGeneral, SQLServerUIMessages.dialog_create_db_label_db_name, ""); //$NON-NLS-2$
        nameText.addModifyListener(e -> {
            name = nameText.getText().trim();
            enableButton(IDialogConstants.OK_ID, !name.isEmpty());
        });

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        super.createButtonsForButtonBar(parent);
        enableButton(IDialogConstants.OK_ID, false);
    }

    public String getName() {
        return name;
    }
}
