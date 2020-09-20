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
package org.jkiss.dbeaver.ext.mysql.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.dialogs.net.SSLConfiguratorAbstractUI;
import org.jkiss.utils.CommonUtils;

/**
 * MySQLSSLConfigurator
 */
public class MySQLSSLConfigurator extends SSLConfiguratorAbstractUI
{
    private Button requireSSQL;
    private Button veryServerCert;
    private Button allowPublicKeyRetrieval;
    private TextWithOpen clientCertText;
    private TextWithOpen clientKeyText;
    private TextWithOpen clientCAText;
    private Text cipherSuitesText;
//    private Button debugSSL;

    @Override
    public void createControl(Composite parent, Runnable propertyChangeListener) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);

        createSSLConfigHint(composite, true, 1);

        {
            Group certGroup = UIUtils.createControlGroup(composite, MySQLUIMessages.mysql_ssl_configurator_legend_certificates, 2, GridData.FILL_HORIZONTAL, -1);
            UIUtils.createControlLabel(certGroup, MySQLUIMessages.mysql_ssl_configurator_label_ca_certificate);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            clientCAText = new TextWithOpenFile(certGroup, MySQLUIMessages.mysql_ssl_configurator_text_with_open_file_ca_certificate, new String[]{"*.*", "*.crt", "*.cert", "*.pem", "*"}); //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
            clientCAText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(certGroup, MySQLUIMessages.mysql_ssl_configurator_label_ssl_certificate);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            clientCertText = new TextWithOpenFile(certGroup, MySQLUIMessages.mysql_ssl_configurator_text_with_open_file_ssl_certificate, new String[]{"*.*", "*.cert", "*.pem", "*"}); //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            clientCertText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(certGroup, MySQLUIMessages.mysql_ssl_configurator_label_ssl_certificate_key);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            clientKeyText = new TextWithOpenFile(certGroup, MySQLUIMessages.mysql_ssl_configurator_text_with_open_file_ssl_certificate_key, new String[]{"*.*", "*.cert", "*.pem", "*"}); //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            clientKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            cipherSuitesText = UIUtils.createLabelText(certGroup, MySQLUIMessages.mysql_ssl_configurator_label_cipher_suites, ""); //$NON-NLS-2$
            cipherSuitesText.setToolTipText(MySQLUIMessages.mysql_ssl_configurator_label_cipher_suites_tip);
        }


        {
            Group advGroup = UIUtils.createControlGroup(composite, MySQLUIMessages.mysql_ssl_configurator_legend_advanced, 2, GridData.FILL_HORIZONTAL, -1);
            requireSSQL = UIUtils.createLabelCheckbox(advGroup, MySQLUIMessages.mysql_ssl_configurator_checkbox_require_ssl, MySQLUIMessages.mysql_ssl_configurator_checkbox_require_ssl_tip, false);
            veryServerCert = UIUtils.createLabelCheckbox(advGroup, MySQLUIMessages.mysql_ssl_configurator_checkbox_verify_server_certificate, MySQLUIMessages.mysql_ssl_configurator_checkbox_verify_server_certificate_tip, true);
            allowPublicKeyRetrieval = UIUtils.createLabelCheckbox(advGroup, MySQLUIMessages.mysql_ssl_configurator_checkbox_allow_public_key, MySQLUIMessages.mysql_ssl_configurator_checkbox_allow_public_key_tip, false);
        }
//        debugSSL = UIUtils.createLabelCheckbox(composite, "Debug SSL", "Prints debug information in standard output.", false);
    }

    @Override
    public void loadSettings(DBWHandlerConfiguration configuration) {
        requireSSQL.setSelection(configuration.getBooleanProperty(MySQLConstants.PROP_REQUIRE_SSL, false));
        veryServerCert.setSelection(configuration.getBooleanProperty(MySQLConstants.PROP_VERIFY_SERVER_SERT, true));
        allowPublicKeyRetrieval.setSelection(configuration.getBooleanProperty(MySQLConstants.PROP_SSL_PUBLIC_KEY_RETRIEVE, false));

        clientCertText.setText(CommonUtils.notEmpty(configuration.getStringProperty(MySQLConstants.PROP_SSL_CLIENT_CERT)));
        clientKeyText.setText(CommonUtils.notEmpty(configuration.getStringProperty(MySQLConstants.PROP_SSL_CLIENT_KEY)));
        clientCAText.setText(CommonUtils.notEmpty(configuration.getStringProperty(MySQLConstants.PROP_SSL_CA_CERT)));
        cipherSuitesText.setText(CommonUtils.notEmpty(configuration.getStringProperty(MySQLConstants.PROP_SSL_CIPHER_SUITES)));
//        debugSSL.setSelection(CommonUtils.getBoolean(configuration.getProperties().get(MySQLConstants.PROP_SSL_DEBUG), false));
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration) {
        configuration.setProperty(MySQLConstants.PROP_REQUIRE_SSL, String.valueOf(requireSSQL.getSelection()));
        configuration.setProperty(MySQLConstants.PROP_VERIFY_SERVER_SERT, String.valueOf(veryServerCert.getSelection()));
        configuration.setProperty(MySQLConstants.PROP_SSL_PUBLIC_KEY_RETRIEVE, String.valueOf(allowPublicKeyRetrieval.getSelection()));
        configuration.setProperty(MySQLConstants.PROP_SSL_CLIENT_CERT, clientCertText.getText());
        configuration.setProperty(MySQLConstants.PROP_SSL_CLIENT_KEY, clientKeyText.getText());
        configuration.setProperty(MySQLConstants.PROP_SSL_CA_CERT, clientCAText.getText());
        configuration.setProperty(MySQLConstants.PROP_SSL_CIPHER_SUITES, cipherSuitesText.getText());
//        configuration.setProperty(MySQLConstants.PROP_SSL_DEBUG, String.valueOf(debugSSL.getSelection()));
    }
}
