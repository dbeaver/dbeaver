/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.gaussdb.ui;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBDataSource;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreCharset;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTablespace;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class GaussDBCreateDatabaseDialog extends BaseDialog {
    private final GaussDBDataSource dataSource;
    private List<PostgreRole> allUsers;
    private List<PostgreCharset> allEncodings;
    private List<PostgreTablespace> allTablespaces;

    private String name;
    private PostgreRole owner;
    private String dbTemplate;
    private PostgreCharset encoding;
    private PostgreTablespace tablespace;
    private String compatibleMode;
    private Combo dbCompatibleMode;
    private Combo userCombo;
    private Combo encodingCombo;
    private Combo tablespaceCombo;

    public GaussDBCreateDatabaseDialog(Shell parentShell, GaussDBDataSource dataSource) {
        super(parentShell, PostgreMessages.dialog_create_db_title, null);
        this.dataSource = dataSource;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        boolean supportsRoles = dataSource.isServerVersionAtLeast(8, 1);
        boolean supportsEncodings = dataSource.getServerType().supportsEncodings();
        boolean supportsTablespaces = dataSource.getServerType().supportsTablespaces();

        final Composite composite = super.createDialogArea(parent);
        final Composite groupGeneral = UIUtils.createControlGroup(composite, PostgreMessages.dialog_create_db_group_general, 2,
            GridData.FILL_HORIZONTAL, SWT.NONE);
        final Text nameText = UIUtils.createLabelText(groupGeneral, PostgreMessages.dialog_create_db_label_db_name, "");
        nameText.addModifyListener(e -> {
            name = nameText.getText().trim();
            getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
        });

        final Composite groupDefinition = UIUtils.createControlGroup(composite, PostgreMessages.dialog_create_db_group_definition, 2,
            GridData.FILL_HORIZONTAL, SWT.NONE);
        supportsRoles(supportsRoles, groupGeneral);
        supportsEncodings(supportsEncodings, groupDefinition);
        supportsTablespaces(supportsTablespaces, groupDefinition);

        dbCompatibleMode = UIUtils.createLabelCombo(groupDefinition, "DataBase Compatibility Mode",
            SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        dbCompatibleMode.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                compatibleMode = dbCompatibleMode.getText();
            }
        });

        scheduleLoadUsersJob(supportsRoles, supportsEncodings, supportsTablespaces);

        return composite;
    }

    private void scheduleLoadUsersJob(boolean supportsRoles, boolean supportsEncodings, boolean supportsTablespaces) {
        new AbstractJob("Load users") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                monitor.beginTask("Create database", 1);
                try {
                    PostgreDatabase database = dataSource.getDefaultInstance();
                    allUsers = supportsRoles ? new ArrayList<>(database.getUsers(monitor)) : null;
                    allEncodings = supportsEncodings ? new ArrayList<>(database.getEncodings(monitor)) : null;
                    allTablespaces = supportsTablespaces ? new ArrayList<>(database.getTablespaces(monitor)) : null;

                    final PostgreRole dba = supportsRoles ? database.getDBA(monitor) : null;
                    final String defUserName = dba == null ? "" : dba.getName();
                    final PostgreCharset defCharset = supportsEncodings ? database.getDefaultEncoding(monitor) : null;
                    final PostgreTablespace defTablespace = supportsTablespaces ? database.getDefaultTablespace(monitor) : null;

                    UIUtils.syncExec(() -> populateDialog(defUserName, defCharset, defTablespace));
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                } finally {
                    monitor.done();
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private void populateDialog(String defUserName, PostgreCharset defCharset, PostgreTablespace defTablespace) {
        if (userCombo != null) {
            for (PostgreRole authId : allUsers) {
                String name = authId.getName();
                userCombo.add(name);
                if (name.equals(defUserName)) {
                    owner = authId;
                }
            }
            userCombo.setText(defUserName);
        }

        if (encodingCombo != null) {
            for (PostgreCharset charset : allEncodings) {
                encodingCombo.add(charset.getName());
                if (charset == defCharset) {
                    encoding = defCharset;
                }
            }
            if (defCharset != null) {
                encodingCombo.setText(defCharset.getName());
            }
        }

        if (tablespaceCombo != null) {
            tablespaceCombo.add(PostgreMessages.dialog_create_db_tablespace_default);
            for (PostgreTablespace ts : allTablespaces) {
                tablespaceCombo.add(ts.getName());
                if (ts == defTablespace) {
                    tablespace = defTablespace;
                }
            }
            if (defTablespace != null) {
                tablespaceCombo.setText(defTablespace.getName());
            }
        }
    }

    private void supportsTablespaces(boolean supportsTablespaces, final Composite groupDefinition) {
        if (supportsTablespaces) {
            tablespaceCombo = UIUtils.createLabelCombo(groupDefinition, PostgreMessages.dialog_create_db_label_tablesapce,
                SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            tablespaceCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (tablespaceCombo.getSelectionIndex() == 0) {
                        tablespace = null;
                    } else {
                        tablespace = allTablespaces.get(tablespaceCombo.getSelectionIndex() - 1);
                    }
                }
            });
        }
    }

    private void supportsEncodings(boolean supportsEncodings, final Composite groupDefinition) {
        if (supportsEncodings) {
            encodingCombo = UIUtils.createLabelCombo(groupDefinition, PostgreMessages.dialog_create_db_label_encoding,
                SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            encodingCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    encoding = allEncodings.get(encodingCombo.getSelectionIndex());
                }
            });
        }
    }

    private void supportsRoles(boolean supportsRoles, final Composite groupGeneral) {
        if (supportsRoles) {
            userCombo = UIUtils.createLabelCombo(groupGeneral, PostgreMessages.dialog_create_db_label_owner,
                SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            userCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    owner = allUsers.get(userCombo.getSelectionIndex());
                }
            });
        }
    }

    public String getCompatibleMode() {
        return this.compatibleMode;
    }

    public String getName() {
        return this.name;
    }

    public PostgreRole getOwner() {
        return this.owner;
    }

    public String getTemplateName() {
        return this.dbTemplate;
    }

    public PostgreCharset getEncoding() {
        return this.encoding;
    }

    public PostgreTablespace getTablespace() {
        return this.tablespace;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
}
