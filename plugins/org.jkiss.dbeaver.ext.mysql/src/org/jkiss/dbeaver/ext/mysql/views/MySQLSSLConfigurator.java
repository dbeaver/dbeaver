/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.mysql.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.impl.net.SSHConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.dialogs.net.SSLConfiguratorAbstractUI;
import org.jkiss.utils.CommonUtils;

/**
 * MySQLSSLConfigurator
 */
public class MySQLSSLConfigurator extends SSLConfiguratorAbstractUI
{
    private Button requireSSQL;
    private Button veryServerCert;
    private TextWithOpen clientCertText;
    private TextWithOpen clientKeyText;
    private TextWithOpen clientCAText;
    private Text cipherSuitesText;

    @Override
    public void createControl(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);

        requireSSQL = UIUtils.createLabelCheckbox(composite, "Require SSL", false);
        veryServerCert = UIUtils.createLabelCheckbox(composite, "Verify server certificate", true);

        UIUtils.createControlLabel(composite, "SSL certificate");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 130;
        clientCertText = new TextWithOpenFile(composite, "SSL Certificate", new String[] {"*.*", "*.cert", "*.pem", "*"} );
        clientCertText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createControlLabel(composite, "SSL certificate key");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 130;
        clientKeyText = new TextWithOpenFile(composite, "SSL Certificate", new String[] {"*.*", "*.cert", "*.pem", "*"} );
        clientKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createControlLabel(composite, "CA certificate (optional)");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 130;
        clientCAText = new TextWithOpenFile(composite, "CA Certificate", new String[] {"*.*", "*.cert", "*.pem", "*"} );
        clientCAText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        cipherSuitesText = UIUtils.createLabelText(composite, "Cipher suites (optional)", "");
    }

    @Override
    public void loadSettings(DBWHandlerConfiguration configuration) {
        requireSSQL.setSelection(CommonUtils.getBoolean(configuration.getProperties().get(MySQLConstants.PROP_REQUIRE_SSL), false));
        veryServerCert.setSelection(CommonUtils.getBoolean(configuration.getProperties().get(MySQLConstants.PROP_VERIFY_SERVER_SERT), true));
        clientCertText.setText(CommonUtils.notEmpty(configuration.getProperties().get(MySQLConstants.PROP_SSL_CLIENT_CERT)));
        clientKeyText.setText(CommonUtils.notEmpty(configuration.getProperties().get(MySQLConstants.PROP_SSL_CLIENT_KEY)));
        clientCAText.setText(CommonUtils.notEmpty(configuration.getProperties().get(MySQLConstants.PROP_SSL_CA_CERT)));
        cipherSuitesText.setText(CommonUtils.notEmpty(configuration.getProperties().get(MySQLConstants.PROP_SSL_CIPHER_SUITES)));
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration) {
        configuration.getProperties().put(MySQLConstants.PROP_REQUIRE_SSL, String.valueOf(requireSSQL.getSelection()));
        configuration.getProperties().put(MySQLConstants.PROP_VERIFY_SERVER_SERT, String.valueOf(veryServerCert.getSelection()));
        configuration.getProperties().put(MySQLConstants.PROP_SSL_CLIENT_CERT, clientCertText.getText());
        configuration.getProperties().put(MySQLConstants.PROP_SSL_CLIENT_KEY, clientKeyText.getText());
        configuration.getProperties().put(MySQLConstants.PROP_SSL_CA_CERT, clientCAText.getText());
        configuration.getProperties().put(MySQLConstants.PROP_SSL_CIPHER_SUITES, cipherSuitesText.getText());
    }
}
