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
import org.jkiss.dbeaver.ext.mimer.model.MimerRoutinePrivilege;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * {@code GRANT EXECUTE} dialog for a Mimer SQL procedure/function - collects the grantee and
 * whether it should be able to grant EXECUTE on to others in turn. Only one privilege type
 * exists for routines, so unlike {@link MimerCreateObjectPrivilegePage} there's no privilege
 * picker.
 *
 * @author Mimer Information Technology
 */
public class MimerCreateRoutinePrivilegePage extends BaseObjectEditPage implements MimerCreatePage {

    private final MimerRoutinePrivilege privilege;

    private String grantee = "";
    private boolean grantable;

    public MimerCreateRoutinePrivilegePage(@NotNull MimerRoutinePrivilege privilege) {
        super("Grant EXECUTE");
        this.privilege = privilege;
    }

    @NotNull
    @Override
    public DBSObject getObject() {
        return privilege;
    }

    @NotNull
    @Override
    protected Control createPageContents(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 2);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        UIUtils.createLabelText(composite, "Routine", privilege.getProcedure().getName()).setEditable(false);

        Combo granteeCombo = UIUtils.createLabelCombo(composite, "Grantee", SWT.DROP_DOWN);
        granteeCombo.setToolTipText(MimerUIMessages.tooltip_grantee_ident_public_program);
        for (String name : MimerIdentPickerUtils.loadIdentNames(privilege.getDataSource())) {
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
            public void widgetSelected(@NotNull org.eclipse.swt.events.SelectionEvent e) {
                grantable = grantableCheck.getSelection();
            }
        });

        return composite;
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
        privilege.setGrantable(grantable);
    }
}
