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
package org.jkiss.dbeaver.ext.mimer.ui.actions;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mimer.model.MimerObjectPrivilege;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * "Add Privileges" dialog - collects one grantee/privilege/grant-option combination and applies
 * it as a separate {@code GRANT} against every table/view the user had selected in the navigator
 * (see {@link MimerAddPrivilegesHandler}), rather than the one-table-at-a-time "Create New
 * Privilege" flow each table's own Privileges folder already offers. Deliberately one privilege
 * type per run, matching that existing single-table dialog exactly - grant a second type by
 * running the action again, rather than adding a not-used-elsewhere-in-this-plugin
 * checkbox-list UI for picking several at once.
 *
 * @author Mimer Information Technology
 */
public class MimerAddPrivilegesDialog extends BaseDialog {

    private final List<String> tableNames;
    private final String[] identNames;

    private Combo granteeCombo;
    private String privilegeType = MimerObjectPrivilege.PRIVILEGE_TYPES[0];
    private String grantee = "";
    private boolean grantable;

    public MimerAddPrivilegesDialog(@NotNull Shell parentShell, @NotNull List<String> tableNames, @NotNull String[] identNames) {
        super(parentShell, "Add Privileges", null);
        this.tableNames = tableNames;
        this.identNames = identNames;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = super.createDialogArea(parent);
        Composite group = new Composite(area, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(group, tableNames.size() > 1 ? "Tables" : "Table", String.join(", ", tableNames), SWT.BORDER | SWT.READ_ONLY)
            .setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Combo privilegeCombo = UIUtils.createLabelCombo(group, "Privilege", SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String type : MimerObjectPrivilege.PRIVILEGE_TYPES) {
            privilegeCombo.add(type);
        }
        privilegeCombo.select(0);
        privilegeCombo.addModifyListener(e -> privilegeType = privilegeCombo.getText());

        // Editable (not READ_ONLY), same reasoning as the single-table dialog: existing
        // Users/Groups cover the common case, but Mimer SQL also allows PROGRAM idents and
        // PUBLIC as a grantee, neither excluded here.
        granteeCombo = UIUtils.createLabelCombo(group, "Grantee", SWT.DROP_DOWN);
        granteeCombo.setToolTipText(MimerUIMessages.tooltip_grantee_ident_public_program);
        for (String name : identNames) {
            granteeCombo.add(name);
        }
        granteeCombo.addModifyListener(e -> {
            grantee = granteeCombo.getText();
            updateOkState();
        });
        granteeCombo.setFocus();

        Button grantableCheck = UIUtils.createCheckbox(group, "With Grant Option", null, false, 2);
        grantableCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> grantable = grantableCheck.getSelection()));

        return area;
    }

    @Override
    protected Control createContents(@NotNull Composite parent) {
        Control contents = super.createContents(parent);
        updateOkState();
        return contents;
    }

    private void updateOkState() {
        enableButton(IDialogConstants.OK_ID, !CommonUtils.isEmptyTrimmed(grantee));
    }

    @NotNull
    public String getPrivilegeType() {
        return privilegeType;
    }

    @NotNull
    public String getGrantee() {
        return grantee.trim();
    }

    public boolean isGrantable() {
        return grantable;
    }
}
