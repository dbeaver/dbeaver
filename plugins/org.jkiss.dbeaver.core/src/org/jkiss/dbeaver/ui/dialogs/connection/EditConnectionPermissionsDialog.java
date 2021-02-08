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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourcePermission;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class EditConnectionPermissionsDialog extends BaseDialog {
    private List<DBPDataSourcePermission> accessRestrictions;
    private List<Button> restrictedPermissionButtons = new ArrayList<>();

    public EditConnectionPermissionsDialog(Shell shell, List<DBPDataSourcePermission> accessRestrictions) {
        super(shell, CoreMessages.dialog_connection_wizard_final_group_security, null);
        this.accessRestrictions = CommonUtils.safeList(accessRestrictions);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);
        for (DBPDataSourcePermission permission : DBPDataSourcePermission.values()) {
            Button permButton = UIUtils.createCheckbox(
                composite,
                permission.getLabel(),
                permission.getDescription(),
                accessRestrictions.contains(permission),
                1);
            permButton.setData(permission);
            permButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            restrictedPermissionButtons.add(permButton);
        }
        return composite;
    }

    @Override
    protected void okPressed() {
        List<DBPDataSourcePermission> restrictions = new ArrayList<>();
        for (Button permButton : restrictedPermissionButtons) {
            if (permButton.getSelection()) {
                restrictions.add((DBPDataSourcePermission) permButton.getData());
            }
        }
        accessRestrictions = restrictions;
        super.okPressed();
    }

    public List<DBPDataSourcePermission> getAccessRestrictions() {
        return accessRestrictions;
    }
}
