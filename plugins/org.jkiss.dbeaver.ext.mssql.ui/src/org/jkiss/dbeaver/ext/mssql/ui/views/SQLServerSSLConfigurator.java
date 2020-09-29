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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.ui.SQLServerUIMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.dialogs.net.SSLConfiguratorAbstractUI;
import org.jkiss.utils.CommonUtils;

public class SQLServerSSLConfigurator extends SSLConfiguratorAbstractUI {
    private TextWithOpen keystoreFile;
    private Text keystorePassword;
    private Text keystoreHostname;
    private Button trustServerCertificate;

    @Override
    public void createControl(Composite parent, Runnable propertyChangeListener) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);

        createSSLConfigHint(composite, true, 1);

        /*
         * We should adopt this code and make it part of the
         * SSLConfiguratorTrustStoreUI, allowing user to
         * choose between using keystore file along with its
         * password, or use CA certificate and Client
         * certificate & password separately. (#9912)
         */

        {
            Group advancedGroup = UIUtils.createControlGroup(composite, SQLServerUIMessages.dialog_setting_ssl_advanced_title, 2, GridData.FILL_HORIZONTAL, -1);

            UIUtils.createControlLabel(advancedGroup, SQLServerUIMessages.dialog_setting_ssl_advanced_keystore_label);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            keystoreFile = new TextWithOpenFile(advancedGroup, SQLServerUIMessages.dialog_setting_ssl_advanced_keystore_title, new String[]{"*.jks;*.pfx"});
            keystoreFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(advancedGroup, SQLServerUIMessages.dialog_setting_ssl_advanced_keystore_password_label);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            keystorePassword = new Text(advancedGroup, SWT.BORDER | SWT.PASSWORD);
            keystorePassword.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(advancedGroup, SQLServerUIMessages.dialog_setting_ssl_advanced_hostname_label);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            keystoreHostname = new Text(advancedGroup, SWT.BORDER);
            keystoreHostname.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            keystoreHostname.setToolTipText(SQLServerUIMessages.dialog_setting_ssl_advanced_hostname_tip);

            trustServerCertificate = UIUtils.createCheckbox(advancedGroup, SQLServerUIMessages.dialog_setting_trust_server_certificate, SQLServerUIMessages.dialog_setting_trust_server_certificate_tip, true, 2);
        }
    }

    @Override
    public void loadSettings(DBWHandlerConfiguration configuration) {
        keystoreFile.setText(CommonUtils.notEmpty(configuration.getStringProperty(SQLServerConstants.PROP_SSL_KEYSTORE)));
        keystorePassword.setText(CommonUtils.notEmpty(configuration.getStringProperty(SQLServerConstants.PROP_SSL_KEYSTORE_PASSWORD)));
        keystoreHostname.setText(CommonUtils.notEmpty(configuration.getStringProperty(SQLServerConstants.PROP_SSL_KEYSTORE_HOSTNAME)));
        trustServerCertificate.setSelection(configuration.getBooleanProperty(SQLServerConstants.PROP_SSL_TRUST_SERVER_CERTIFICATE));
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration) {
        configuration.setProperty(SQLServerConstants.PROP_SSL_KEYSTORE, keystoreFile.getText().trim());
        configuration.setProperty(SQLServerConstants.PROP_SSL_KEYSTORE_PASSWORD, keystorePassword.getText().trim());
        configuration.setProperty(SQLServerConstants.PROP_SSL_KEYSTORE_HOSTNAME, keystoreHostname.getText().trim());
        configuration.setProperty(SQLServerConstants.PROP_SSL_TRUST_SERVER_CERTIFICATE, trustServerCertificate.getSelection());
    }
}
