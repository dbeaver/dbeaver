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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerGroup;
import org.jkiss.dbeaver.ext.mimer.model.MimerObjectPrivilege;
import org.jkiss.dbeaver.ext.mimer.model.MimerProgram;
import org.jkiss.dbeaver.ext.mimer.model.MimerUser;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

import java.util.stream.Stream;

/**
 * {@code GRANT} dialog for a Mimer SQL table/view privilege - collects the grantee, which
 * privilege to grant (SELECT/INSERT/UPDATE/DELETE/REFERENCES, or {@code ALL PRIVILEGES} for all
 * at once - whole-object only, no column list, see {@link MimerObjectPrivilege}), and whether it
 * should be able to grant that privilege on to others in turn.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateObjectPrivilegePage extends BaseObjectEditPage {

    private static final Log log = Log.getLog(MimerCreateObjectPrivilegePage.class);

    private final MimerObjectPrivilege privilege;

    private String grantee = "";
    private String privilegeType = MimerObjectPrivilege.PRIVILEGE_TYPES[0];
    private boolean grantable;

    public MimerCreateObjectPrivilegePage(@NotNull MimerObjectPrivilege privilege) {
        super("Grant privilege");
        this.privilege = privilege;
    }

    @Override
    public DBSObject getObject() {
        return privilege;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 2);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(composite, "Table", privilege.getTable().getName()).setEditable(false);

        Combo privilegeCombo = UIUtils.createLabelCombo(composite, "Privilege", SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String type : MimerObjectPrivilege.PRIVILEGE_TYPES) {
            privilegeCombo.add(type);
        }
        privilegeCombo.select(0);
        privilegeCombo.addModifyListener(e -> privilegeType = privilegeCombo.getText());

        // Editable (not READ_ONLY): existing Users/Groups cover the common case, but Mimer SQL
        // also allows PROGRAM idents, and PUBLIC (unlike group membership) is a normal grant
        // target here - not modeled/excluded, so typing one in stays possible.
        Combo granteeCombo = UIUtils.createLabelCombo(composite, "Grantee", SWT.DROP_DOWN);
        granteeCombo.setToolTipText(MimerUIMessages.tooltip_grantee_ident_public_program);
        for (String name : loadIdentNames()) {
            granteeCombo.add(name);
        }
        granteeCombo.addModifyListener(e -> {
            grantee = granteeCombo.getText();
            updatePageState();
        });
        granteeCombo.setFocus();

        Button grantableCheck = UIUtils.createCheckbox(composite, "With Grant Option", null, false, 2);
        grantableCheck.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                grantable = grantableCheck.getSelection();
            }
        });

        return composite;
    }

    @NotNull
    private String[] loadIdentNames() {
        try {
            if (privilege.getDataSource() instanceof MimerDataSource ds) {
                VoidProgressMonitor monitor = new VoidProgressMonitor();
                return Stream.of(
                        ds.getUsers(monitor).stream().map(MimerUser::getName),
                        ds.getGroups(monitor).stream().map(MimerGroup::getName),
                        ds.getPrograms(monitor).stream().map(MimerProgram::getName))
                    .flatMap(s -> s)
                    .sorted()
                    .toArray(String[]::new);
            }
        } catch (Exception e) {
            log.debug("Can't load ident list for the grant-privilege dialog", e);
        }
        return new String[0];
    }

    @Override
    public boolean isPageComplete() {
        return !CommonUtils.isEmptyTrimmed(grantee);
    }

    /**
     * Applies the collected values to the privilege. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        privilege.setGrantee(grantee.trim());
        privilege.setPrivilegeType(privilegeType);
        privilege.setGrantable(grantable);
    }
}
