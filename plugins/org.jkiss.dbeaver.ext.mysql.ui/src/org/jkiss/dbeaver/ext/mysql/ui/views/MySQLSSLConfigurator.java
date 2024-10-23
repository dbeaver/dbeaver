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
package org.jkiss.dbeaver.ext.mysql.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.configurator.DBPConnectionEditIntention;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.net.SSLConfiguratorTrustStoreUI;
import org.jkiss.utils.CommonUtils;

/**
 * MySQLSSLConfigurator
 */
public class MySQLSSLConfigurator extends SSLConfiguratorTrustStoreUI {
    private Button requireSSQL;
    private Button veryServerCert;
    private Button allowPublicKeyRetrieval;
    private Text cipherSuitesText;

    @Override
    public void createControl(@NotNull Composite parent, Object object, @NotNull Runnable propertyChangeListener) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);

        createSSLConfigHint(composite, true, 1);
        createTrustStoreConfigGroup(composite);

        {
            cipherSuitesText = UIUtils.createLabelText(sslCertComposite, MySQLUIMessages.mysql_ssl_configurator_label_cipher_suites, ""); //$NON-NLS-2$
            cipherSuitesText.setToolTipText(MySQLUIMessages.mysql_ssl_configurator_label_cipher_suites_tip);
        }

        {
            Group advGroup = UIUtils.createControlGroup(composite, MySQLUIMessages.mysql_ssl_configurator_legend_advanced, 2, GridData.FILL_HORIZONTAL, -1);
            requireSSQL = UIUtils.createLabelCheckbox(advGroup, MySQLUIMessages.mysql_ssl_configurator_checkbox_require_ssl, MySQLUIMessages.mysql_ssl_configurator_checkbox_require_ssl_tip, false);
            veryServerCert = UIUtils.createLabelCheckbox(advGroup, MySQLUIMessages.mysql_ssl_configurator_checkbox_verify_server_certificate, MySQLUIMessages.mysql_ssl_configurator_checkbox_verify_server_certificate_tip, true);
            allowPublicKeyRetrieval = UIUtils.createLabelCheckbox(advGroup, MySQLUIMessages.mysql_ssl_configurator_checkbox_allow_public_key, MySQLUIMessages.mysql_ssl_configurator_checkbox_allow_public_key_tip, false);
        }

        if (this.getEditIntention() == DBPConnectionEditIntention.CREDENTIALS_ONLY) {
            cipherSuitesText.setEditable(false);
            requireSSQL.setEnabled(false);
            veryServerCert.setEnabled(false);
            allowPublicKeyRetrieval.setEnabled(false);
        }
    }

    @Override
    protected boolean useCACertificate() {
        return true;
    }

    @Override
    public void loadSettings(@NotNull DBWHandlerConfiguration configuration) {
        super.loadSettings(configuration);

        if (CommonUtils.isEmpty(configuration.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD))) {
            // Backward compatibility
            caCertPath.setText(CommonUtils.notEmpty(configuration.getStringProperty(MySQLConstants.PROP_SSL_CA_CERT)));
            clientCertPath.setText(CommonUtils.notEmpty(configuration.getStringProperty(MySQLConstants.PROP_SSL_CLIENT_CERT)));
            clientKeyPath.setText(CommonUtils.notEmpty(configuration.getStringProperty(MySQLConstants.PROP_SSL_CLIENT_KEY)));
        }

        requireSSQL.setSelection(configuration.getBooleanProperty(MySQLConstants.PROP_REQUIRE_SSL, false));
        veryServerCert.setSelection(configuration.getBooleanProperty(MySQLConstants.PROP_VERIFY_SERVER_SERT, true));
        allowPublicKeyRetrieval.setSelection(configuration.getBooleanProperty(MySQLConstants.PROP_SSL_PUBLIC_KEY_RETRIEVE, false));

        cipherSuitesText.setText(CommonUtils.notEmpty(configuration.getStringProperty(MySQLConstants.PROP_SSL_CIPHER_SUITES)));
    }

    @Override
    public void saveSettings(@NotNull DBWHandlerConfiguration configuration) {
        super.saveSettings(configuration);

        configuration.setProperty(MySQLConstants.PROP_REQUIRE_SSL, String.valueOf(requireSSQL.getSelection()));
        configuration.setProperty(MySQLConstants.PROP_VERIFY_SERVER_SERT, String.valueOf(veryServerCert.getSelection()));
        configuration.setProperty(MySQLConstants.PROP_SSL_PUBLIC_KEY_RETRIEVE, String.valueOf(allowPublicKeyRetrieval.getSelection()));
        configuration.setProperty(MySQLConstants.PROP_SSL_CIPHER_SUITES, cipherSuitesText.getText());
    }
}
