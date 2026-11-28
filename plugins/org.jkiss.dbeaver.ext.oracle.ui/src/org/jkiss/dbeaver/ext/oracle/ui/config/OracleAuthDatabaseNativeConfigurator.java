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
package org.jkiss.dbeaver.ext.oracle.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.oracle.model.auth.OracleAuthModelDatabaseNative;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionRole;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;

import java.util.Locale;

/**
 * Oracle database native auth model config
 */
public class OracleAuthDatabaseNativeConfigurator extends DatabaseNativeAuthModelConfigurator {

    private Combo userRoleCombo;

    @Override
    public void createControl(@NotNull Composite parent, DBAAuthModel<?> object, @NotNull Runnable propertyChangeListener) {
        Label usernameLabel = UIUtils.createLabel(parent, UIConnectionMessages.dialog_connection_auth_label_username);
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        Composite userGroup = UIUtils.createComposite(parent, 3);
        userGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createUserNameControls(userGroup, propertyChangeListener);

        Label userRoleLabel = UIUtils.createControlLabel(userGroup, OracleUIMessages.dialog_connection_role);
        userRoleLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        userRoleCombo = new Combo(userGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(userRoleCombo) * 10;
        userRoleCombo.setLayoutData(gd);
        for (OracleConnectionRole role : OracleConnectionRole.values()) {
            userRoleCombo.add(role.getTitle());
        }
        userRoleCombo.select(0);

        createPasswordControls(parent, propertyChangeListener);
    }

    @Override
    public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.loadSettings(dataSource);

        String roleName = OracleAuthModelDatabaseNative.getConnectionRole(dataSource.getConnectionConfiguration());
        userRoleCombo.setText(OracleConnectionRole.NORMAL.getTitle().equalsIgnoreCase(roleName)
            ? OracleConnectionRole.NORMAL.getTitle() : roleName.toUpperCase(Locale.ENGLISH));
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);

        OracleAuthModelDatabaseNative.setConnectionRole(dataSource.getConnectionConfiguration(), userRoleCombo.getText());
    }

    @Override
    public void resetSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.resetSettings(dataSource);
    }

}
