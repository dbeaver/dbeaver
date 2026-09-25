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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.model.MimerSystemPrivilege;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;

/**
 * "Grant System Privilege" dialog - Privilege (one of {@link MimerConstants#SYSTEM_PRIVILEGE_TYPES})
 * and Grant Option; the grantee ident is fixed to whichever User/Group/Program folder this was
 * opened from, shown read-only - the identical Group/Program actions work the same way. {@link MimerConstants} (not
 * {@code MimerSystemPrivilegeManager}, which lives in the non-exported {@code .edit} package) is
 * what this class must reference - see the cross-plugin {@code class=} export gotcha documented
 * on {@link MimerConstants#SYSTEM_PRIVILEGE_TYPES}. Referencing a class from a non-exported
 * package fails silently at runtime, with no error in the console log - only a {@code
 * ClassNotFoundException} in the workspace's {@code .metadata/.log}.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateSystemPrivilegePage extends BaseObjectEditPage {

    private final MimerSystemPrivilege privilege;
    private final DBSObject ident;

    private String privilegeType = MimerConstants.SYSTEM_PRIVILEGE_TYPES[1];
    private boolean grantable;

    public MimerCreateSystemPrivilegePage(@NotNull MimerSystemPrivilege privilege, @NotNull DBSObject ident) {
        super("Grant system privilege");
        this.privilege = privilege;
        this.ident = ident;
    }

    @Override
    public DBSObject getObject() {
        return privilege;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite group = UIUtils.createComposite(parent, 2);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        Combo privilegeCombo = UIUtils.createLabelCombo(group, "Privilege", SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String type : MimerConstants.SYSTEM_PRIVILEGE_TYPES) {
            privilegeCombo.add(type);
        }
        privilegeCombo.select(1);
        privilegeCombo.addModifyListener(e -> privilegeType = privilegeCombo.getText());

        UIUtils.createLabelText(group, "Grantee", ident.getName()).setEditable(false);

        Button grantOptionButton = UIUtils.createLabelCheckbox(group, "With Grant Option", false);
        grantOptionButton.addSelectionListener(
            SelectionListener.widgetSelectedAdapter(e -> grantable = grantOptionButton.getSelection()));

        return group;
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }

    /**
     * Applies the collected values to the privilege. Call after {@link #edit()} returns {@code true}.
     */
    public void applyChanges() {
        privilege.setName(privilegeType);
        privilege.setGrantable(grantable);
    }
}
