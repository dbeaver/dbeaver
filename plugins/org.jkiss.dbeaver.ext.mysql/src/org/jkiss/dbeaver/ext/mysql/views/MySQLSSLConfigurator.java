/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
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
    public void createControl(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);

        {
            Group certGroup = UIUtils.createControlGroup(composite, "Certificates", 2, GridData.FILL_HORIZONTAL, -1);
            UIUtils.createControlLabel(certGroup, "CA certificate");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            clientCAText = new TextWithOpenFile(certGroup, "CA Certificate", new String[]{"*.*", "*.crt", "*.cert", "*.pem", "*"});
            clientCAText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(certGroup, "SSL certificate");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            clientCertText = new TextWithOpenFile(certGroup, "SSL Certificate", new String[]{"*.*", "*.cert", "*.pem", "*"});
            clientCertText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(certGroup, "SSL certificate key");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            clientKeyText = new TextWithOpenFile(certGroup, "SSL Certificate", new String[]{"*.*", "*.cert", "*.pem", "*"});
            clientKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            cipherSuitesText = UIUtils.createLabelText(certGroup, "Cipher suites (optional)", "");
            cipherSuitesText.setToolTipText("Overrides the cipher suites enabled for use on the underlying SSL sockets.\nThis may be required when using external JSSE providers or to specify cipher suites compatible with both MySQL server and used JVM.");
        }


        {
            Group advGroup = UIUtils.createControlGroup(composite, "Advanced", 2, GridData.FILL_HORIZONTAL, -1);
            requireSSQL = UIUtils.createLabelCheckbox(advGroup, "Require SSL", "Require server support of SSL connection.", false);
            veryServerCert = UIUtils.createLabelCheckbox(advGroup, "Verify server certificate", "Should the driver verify the server's certificate?\nWhen using this feature, the explicit certificate parameters should be specified, rather than system properties.", true);
            allowPublicKeyRetrieval = UIUtils.createLabelCheckbox(advGroup, "Allow public key retrieval", "Allows special handshake roundtrip to get server RSA public key directly from server.", false);
        }
//        debugSSL = UIUtils.createLabelCheckbox(composite, "Debug SSL", "Prints debug information in standard output.", false);
    }

    @Override
    public void loadSettings(DBWHandlerConfiguration configuration) {
        requireSSQL.setSelection(CommonUtils.getBoolean(configuration.getProperties().get(MySQLConstants.PROP_REQUIRE_SSL), false));
        veryServerCert.setSelection(CommonUtils.getBoolean(configuration.getProperties().get(MySQLConstants.PROP_VERIFY_SERVER_SERT), true));
        allowPublicKeyRetrieval.setSelection(CommonUtils.getBoolean(configuration.getProperties().get(MySQLConstants.PROP_SSL_PUBLIC_KEY_RETRIEVE), false));
        clientCertText.setText(CommonUtils.notEmpty(configuration.getProperties().get(MySQLConstants.PROP_SSL_CLIENT_CERT)));
        clientKeyText.setText(CommonUtils.notEmpty(configuration.getProperties().get(MySQLConstants.PROP_SSL_CLIENT_KEY)));
        clientCAText.setText(CommonUtils.notEmpty(configuration.getProperties().get(MySQLConstants.PROP_SSL_CA_CERT)));
        cipherSuitesText.setText(CommonUtils.notEmpty(configuration.getProperties().get(MySQLConstants.PROP_SSL_CIPHER_SUITES)));
//        debugSSL.setSelection(CommonUtils.getBoolean(configuration.getProperties().get(MySQLConstants.PROP_SSL_DEBUG), false));
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration) {
        configuration.getProperties().put(MySQLConstants.PROP_REQUIRE_SSL, String.valueOf(requireSSQL.getSelection()));
        configuration.getProperties().put(MySQLConstants.PROP_VERIFY_SERVER_SERT, String.valueOf(veryServerCert.getSelection()));
        configuration.getProperties().put(MySQLConstants.PROP_SSL_PUBLIC_KEY_RETRIEVE, String.valueOf(allowPublicKeyRetrieval.getSelection()));
        configuration.getProperties().put(MySQLConstants.PROP_SSL_CLIENT_CERT, clientCertText.getText());
        configuration.getProperties().put(MySQLConstants.PROP_SSL_CLIENT_KEY, clientKeyText.getText());
        configuration.getProperties().put(MySQLConstants.PROP_SSL_CA_CERT, clientCAText.getText());
        configuration.getProperties().put(MySQLConstants.PROP_SSL_CIPHER_SUITES, cipherSuitesText.getText());
//        configuration.getProperties().put(MySQLConstants.PROP_SSL_DEBUG, String.valueOf(debugSSL.getSelection()));
    }
}
