/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.net.SSLConfiguratorTrustStoreUI;
import org.jkiss.utils.CommonUtils;

public class SQLServerSSLConfigurator extends SSLConfiguratorTrustStoreUI {
    private Text keystoreHostname;

    @Override
    public void createControl(Composite parent, Runnable propertyChangeListener) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);

        createSSLConfigHint(composite, true, 1);
        createTrustStoreConfigGroup(composite);

        {
            Group settingsGroup = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_HORIZONTAL, -1);

            UIUtils.createControlLabel(settingsGroup, "Certificate hostname");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            keystoreHostname = new Text(settingsGroup, SWT.BORDER);
            keystoreHostname.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            keystoreHostname.setToolTipText("The host name to be used in validating the SQL Server TLS/SSL certificate.");
        }
    }

    @Override
    public void loadSettings(DBWHandlerConfiguration configuration) {
        super.loadSettings(configuration);
        keystoreHostname.setText(CommonUtils.notEmpty(configuration.getStringProperty(SQLServerConstants.PROP_SSL_KEYSTORE_HOSTNAME)));
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration) {
        super.saveSettings(configuration);
        configuration.setProperty(SQLServerConstants.PROP_SSL_KEYSTORE_HOSTNAME, keystoreHostname.getText().trim());
    }
}
