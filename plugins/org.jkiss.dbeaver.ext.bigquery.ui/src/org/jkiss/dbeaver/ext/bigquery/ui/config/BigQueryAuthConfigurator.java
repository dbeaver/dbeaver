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
package org.jkiss.dbeaver.ext.bigquery.ui.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.bigquery.model.BigQueryConstants;
import org.jkiss.dbeaver.ext.bigquery.ui.internal.BigQueryMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ConfigurationFileSelector;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.utils.CommonUtils;

public class BigQueryAuthConfigurator extends DatabaseNativeAuthModelConfigurator {

    private Combo authTypeCombo;
    private TextWithOpenFile authCertFile;

    @Override
    public void createControl(@NotNull Composite authPanel, DBAAuthModel<?> object, @NotNull Runnable propertyChangeListener) {
        UIUtils.createControlLabel(authPanel, BigQueryMessages.label_oauth_type);
        authTypeCombo = new Combo(authPanel, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        authTypeCombo.add(BigQueryMessages.label_service_based);
        authTypeCombo.add(BigQueryMessages.label_user_based);
        authTypeCombo.select(0);

        UIUtils.createControlLabel(authPanel, BigQueryMessages.label_key_path);
        authCertFile = new ConfigurationFileSelector(authPanel, BigQueryMessages.label_private_key_path, new String[]{"*", "*.p12", "*.json"}, DBWorkbench.isDistributed());
        authCertFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
    }

    @Override
    public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.loadSettings(dataSource);

        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();

        if (authTypeCombo != null) {
            authTypeCombo.select(CommonUtils.toInt(connectionInfo.getProperty(BigQueryConstants.DRIVER_PROP_OAUTH_TYPE)));
        }
        String keyPath = connectionInfo.getProperty(
            DBWorkbench.isDistributed() ? BigQueryConstants.DRIVER_PROP_OAUTH_PVT_KEY : BigQueryConstants.DRIVER_PROP_OAUTH_PVT_KEYPATH);
        if (keyPath != null && authCertFile != null) {
            authCertFile.setText(keyPath);
        }
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);

        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();

        if (authTypeCombo != null) {
            connectionInfo.setProperty(BigQueryConstants.DRIVER_PROP_OAUTH_TYPE, String.valueOf(authTypeCombo.getSelectionIndex()));
        }
        if (authCertFile != null) {
            connectionInfo.setProperty(
                DBWorkbench.isDistributed() ? BigQueryConstants.DRIVER_PROP_OAUTH_PVT_KEY : BigQueryConstants.DRIVER_PROP_OAUTH_PVT_KEYPATH,
                authCertFile.getText().trim());
        }
    }
}
