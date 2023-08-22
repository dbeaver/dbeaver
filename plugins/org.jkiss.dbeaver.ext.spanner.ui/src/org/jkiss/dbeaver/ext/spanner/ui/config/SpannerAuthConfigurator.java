/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.spanner.ui.config;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.spanner.model.SpannerConstants;
import org.jkiss.dbeaver.ext.spanner.ui.internal.SpannerMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ConfigurationFileSelector;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.utils.CommonUtils;

public class SpannerAuthConfigurator extends DatabaseNativeAuthModelConfigurator {

    private TextWithOpenFile privateKeyFile;

    @Override
    public void createControl(@NotNull Composite authPanel, DBAAuthModel<?> object, @NotNull Runnable propertyChangeListener) {
        UIUtils.createControlLabel(authPanel, SpannerMessages.label_private_key_path);
        privateKeyFile = new ConfigurationFileSelector(authPanel, SpannerMessages.label_private_key_path, new String[] { "*", "*.json" } );
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
        privateKeyFile.setLayoutData(gd);
    }

    @Override
    public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.loadSettings(dataSource);
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (privateKeyFile != null) {
            privateKeyFile.setText(CommonUtils.notEmpty(connectionInfo.getProviderProperty(SpannerConstants.DRIVER_PROP_PVTKEYPATH)));
        }
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (privateKeyFile != null) {
            connectionInfo.setProviderProperty(SpannerConstants.DRIVER_PROP_PVTKEYPATH, privateKeyFile.getText().trim());
        }
        super.saveSettings(dataSource);
    }
}
