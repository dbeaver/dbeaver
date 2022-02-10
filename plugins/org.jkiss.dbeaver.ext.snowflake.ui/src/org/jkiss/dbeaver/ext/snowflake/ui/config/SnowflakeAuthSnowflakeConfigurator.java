/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.snowflake.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.ext.snowflake.SnowflakeConstants;
import org.jkiss.dbeaver.ext.snowflake.ui.internal.SnowflakeMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

/**
 * Snowflake database native auth model config
 */
public class SnowflakeAuthSnowflakeConfigurator extends DatabaseNativeAuthModelConfigurator {

    private Combo userRoleCombo;

    @Override
    public void createControl(Composite parent, Runnable propertyChangeListener) {
        usernameLabel = UIUtils.createLabel(parent, UIConnectionMessages.dialog_connection_auth_label_username);
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        createUserNameControls(parent, propertyChangeListener);

        if (supportsPassword()) {
            createPasswordControls(parent, propertyChangeListener);
        }

        Label userRoleLabel = UIUtils.createControlLabel(parent, SnowflakeMessages.label_role);
        userRoleLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        userRoleCombo = new Combo(parent, SWT.DROP_DOWN);
        GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(userRoleCombo) * 10;
        userRoleCombo.setLayoutData(gd);
        userRoleCombo.select(0);
    }

    @Override
    public void loadSettings(DBPDataSourceContainer dataSource) {
        super.loadSettings(dataSource);

        String roleName = dataSource.getConnectionConfiguration().getAuthProperty(SnowflakeConstants.PROP_AUTH_ROLE);
        if (roleName != null) {
            userRoleCombo.setText(roleName);
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);

        String roleName = userRoleCombo.getText();
        if (!CommonUtils.isEmpty(roleName)) {
            dataSource.getConnectionConfiguration().setAuthProperty(
                SnowflakeConstants.PROP_AUTH_ROLE,
                roleName);
        }

        // Remove legacy properties
        dataSource.getConnectionConfiguration().removeProviderProperty(SnowflakeConstants.PROP_AUTHENTICATOR_LEGACY);
        dataSource.getConnectionConfiguration().removeProviderProperty(SnowflakeConstants.PROP_ROLE_LEGACY);
    }

    @Override
    public void resetSettings(DBPDataSourceContainer dataSource) {
        super.resetSettings(dataSource);
    }

    protected boolean supportsPassword() {
        return true;
    }

}
