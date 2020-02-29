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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreCreateSchemaDialog
 */
public class PostgreCreateSchemaDialog extends BaseDialog
{
    private final PostgreSchema schema;
    private List<PostgreRole> allUsers;
    private String name;
    private PostgreRole owner;

    public PostgreCreateSchemaDialog(Shell parentShell, PostgreSchema schema) {
        super(parentShell, PostgreMessages.dialog_create_schema_title, null);
        this.schema = schema;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        final Composite composite = super.createDialogArea(parent);

        final Composite group = new Composite(composite, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Text nameText = UIUtils.createLabelText(group, PostgreMessages.dialog_create_schema_name, ""); //$NON-NLS-2$
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                name = nameText.getText();
                getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
            }
        });
        final Text databaseText = UIUtils.createLabelText(group, "Database", schema.getDatabase().getName(), SWT.BORDER | SWT.READ_ONLY); //$NON-NLS-2$

        final Combo userCombo = UIUtils.createLabelCombo(group, PostgreMessages.dialog_create_schema_owner, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);

        userCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                owner = allUsers.get(userCombo.getSelectionIndex());
            }
        });

        new AbstractJob("Load users") {

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    final List<String> userNames = new ArrayList<>();
                    allUsers = new ArrayList<>(schema.getDatabase().getUsers(monitor));
                    final PostgreRole dba = schema.getDatabase().getDBA(monitor);
                    final String defUserName = dba == null ? "" : dba.getName(); //$NON-NLS-1$

                    UIUtils.syncExec(() -> {
                        for (PostgreRole authId : allUsers) {
                            String name = authId.getName();
                            userCombo.add(name);
                            if (name.equals(defUserName)) {
                                owner = authId;
                            }
                        }
                        userCombo.setText(defUserName);
                    });
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();

        return composite;
    }

    public String getName() {
        return name;
    }

    public PostgreRole getOwner() {
        return owner;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
}
