/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgresMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.driver.DriverClassFindJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.dialogs.net.SSLConfiguratorAbstractUI;
import org.jkiss.utils.CommonUtils;

/**
 * PostgreSSLConfigurator
 */
public class PostgreSSLConfigurator extends SSLConfiguratorAbstractUI
{
    public static final String[] SSL_MODES = {"","disable","allow","prefer","require","verify-ca","verify-full"};

    private TextWithOpen rootCertText;
    private TextWithOpen clientCertText;
    private TextWithOpen clientKeyText;

    private Combo sslModeCombo;
    private Combo sslFactoryCombo;

    @Override
    public void createControl(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);

        {
            Group certGroup = UIUtils.createControlGroup(composite, PostgresMessages.dialog_connection_network_postgres_ssl_certificates, 2, GridData.FILL_HORIZONTAL, -1);
            UIUtils.createControlLabel(certGroup, PostgresMessages.dialog_connection_network_postgres_ssl_certificates_root);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            rootCertText = new TextWithOpenFile(certGroup, PostgresMessages.dialog_connection_network_postgres_ssl_certificates_ca, new String[]{"*.*", "*.crt", "*.cert", "*.pem", "*"});
            rootCertText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(certGroup, PostgresMessages.dialog_connection_network_postgres_ssl_certificates_ssl);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            clientCertText = new TextWithOpenFile(certGroup, PostgresMessages.dialog_connection_network_postgres_ssl_certificates_ssl, new String[]{"*.*", "*.cert", "*.pem", "*"});
            clientCertText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(certGroup, PostgresMessages.dialog_connection_network_postgres_ssl_certificates_ssl_key);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            clientKeyText = new TextWithOpenFile(certGroup, PostgresMessages.dialog_connection_network_postgres_ssl_certificates_ssl, new String[]{"*.*", "*.cert", "*.pem", "*"});
            clientKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }


        {
            Group advGroup = UIUtils.createControlGroup(composite, PostgresMessages.dialog_connection_network_postgres_ssl_advanced, 2, GridData.FILL_HORIZONTAL, -1);
            sslModeCombo = UIUtils.createLabelCombo(advGroup, PostgresMessages.dialog_connection_network_postgres_ssl_advanced_ssl_mode, SWT.READ_ONLY | SWT.DROP_DOWN);
            sslModeCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            for (String mode : SSL_MODES) {
                sslModeCombo.add(mode);
            }
            sslFactoryCombo = UIUtils.createLabelCombo(advGroup, PostgresMessages.dialog_connection_network_postgres_ssl_advanced_ssl_factory, SWT.DROP_DOWN);
        }
    }

    @Override
    public void loadSettings(final DBWHandlerConfiguration configuration) {
        clientCertText.setText(CommonUtils.notEmpty(configuration.getProperties().get(PostgreConstants.PROP_SSL_CLIENT_CERT)));
        clientKeyText.setText(CommonUtils.notEmpty(configuration.getProperties().get(PostgreConstants.PROP_SSL_CLIENT_KEY)));
        rootCertText.setText(CommonUtils.notEmpty(configuration.getProperties().get(PostgreConstants.PROP_SSL_ROOT_CERT)));
        UIUtils.setComboSelection(sslModeCombo, CommonUtils.notEmpty(configuration.getProperties().get(PostgreConstants.PROP_SSL_MODE)));

        final Job resolveJob = new Job("Find factories") {
            {
                setUser(true);
            }
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                final DriverClassFindJob finder = new DriverClassFindJob(
                    configuration.getDriver(),
                    "javax/net/ssl/SSLSocketFactory",
                    false);
                finder.run(monitor);
                DBeaverUI.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        for (String cn : finder.getDriverClassNames()) {
                            sslFactoryCombo.add(cn);
                        }
                        final String factoryValue = configuration.getProperties().get(PostgreConstants.PROP_SSL_FACTORY);
                        if (!CommonUtils.isEmpty(factoryValue)) {
                            sslFactoryCombo.setText(factoryValue);
                        }
                    }
                });
                return Status.OK_STATUS;
            }
        };
        resolveJob.schedule();
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration) {
        configuration.getProperties().put(PostgreConstants.PROP_SSL_ROOT_CERT, rootCertText.getText());
        configuration.getProperties().put(PostgreConstants.PROP_SSL_CLIENT_CERT, clientCertText.getText());
        configuration.getProperties().put(PostgreConstants.PROP_SSL_CLIENT_KEY, clientKeyText.getText());
        configuration.getProperties().put(PostgreConstants.PROP_SSL_MODE, sslModeCombo.getText());
        configuration.getProperties().put(PostgreConstants.PROP_SSL_FACTORY, sslFactoryCombo.getText());
    }
}
