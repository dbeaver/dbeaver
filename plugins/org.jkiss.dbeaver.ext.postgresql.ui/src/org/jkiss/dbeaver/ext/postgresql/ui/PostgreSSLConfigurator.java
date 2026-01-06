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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.registry.configurator.DBPConnectionEditIntention;
import org.jkiss.dbeaver.registry.driver.DriverClassFindJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.net.SSLConfiguratorTrustStoreUI;
import org.jkiss.utils.CommonUtils;

import javax.net.ssl.SSLSocketFactory;

/**
 * PostgreSSLConfigurator
 */
public class PostgreSSLConfigurator extends SSLConfiguratorTrustStoreUI {
    private static final boolean ENABLE_PROXY = false;

    public static final String[] SSL_MODES = {"","disable","allow","prefer","require","verify-ca","verify-full"};

    private Combo sslModeCombo;
    private Combo sslFactoryCombo;
    private Button useProxyService;
    private boolean sslClassesResolved;

    @Override
    public void createControl(@NotNull Composite parent, Object object, @NotNull Runnable propertyChangeListener) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);

        createSSLConfigHint(composite, true, 1);
        createTrustStoreConfigGroup(composite);

        if (this.getEditIntention() != DBPConnectionEditIntention.CREDENTIALS_ONLY) {
            Group advGroup = UIUtils.createControlGroup(composite, PostgreMessages.dialog_connection_network_postgres_ssl_advanced, 2, GridData.FILL_HORIZONTAL, -1);
            sslModeCombo = UIUtils.createLabelCombo(advGroup, PostgreMessages.dialog_connection_network_postgres_ssl_advanced_ssl_mode, SWT.READ_ONLY | SWT.DROP_DOWN);
            sslModeCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            for (String mode : SSL_MODES) {
                sslModeCombo.add(mode);
            }
            sslFactoryCombo = UIUtils.createLabelCombo(advGroup, PostgreMessages.dialog_connection_network_postgres_ssl_advanced_ssl_factory, SWT.DROP_DOWN);
            if (ENABLE_PROXY) {
                useProxyService = UIUtils.createCheckbox(
                    advGroup,
                    PostgreMessages.dialog_connection_network_postgres_ssl_advanced_use_proxy,
                    PostgreMessages.dialog_connection_network_postgres_ssl_advanced_use_proxy_tip,
                    false, 2);
            }
        }
    }

    @Override
    protected boolean useCACertificate() {
        return true;
    }

    @Override
    public void loadSettings(@NotNull final DBWHandlerConfiguration configuration) {
        super.loadSettings(configuration);

        if (CommonUtils.isEmpty(configuration.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD))) {
            // Backward compatibility
            caCertPath.setText(CommonUtils.notEmpty(configuration.getStringProperty(PostgreConstants.PROP_SSL_ROOT_CERT)));
            clientCertPath.setText(CommonUtils.notEmpty(configuration.getStringProperty(PostgreConstants.PROP_SSL_CLIENT_CERT)));
            clientKeyPath.setText(CommonUtils.notEmpty(configuration.getStringProperty(PostgreConstants.PROP_SSL_CLIENT_KEY)));
        }

        if (this.getEditIntention() != DBPConnectionEditIntention.CREDENTIALS_ONLY) {
            UIUtils.setComboSelection(sslModeCombo, CommonUtils.notEmpty(configuration.getStringProperty(PostgreConstants.PROP_SSL_MODE)));
            if (ENABLE_PROXY) {
                useProxyService.setSelection(configuration.getBooleanProperty(PostgreConstants.PROP_SSL_PROXY));
            }

            PaintListener paintListener = new PaintListener() {
                @Override
                public void paintControl(PaintEvent e) {
                    if (!sslClassesResolved) {
                        sslClassesResolved = true;
                        sslFactoryCombo.removePaintListener(this);
                        final Job resolveJob = new Job("Find factories") {
                            {
                                setUser(true);
                            }

                            @Override
                            protected IStatus run(IProgressMonitor monitor) {
                                final DriverClassFindJob finder = new DriverClassFindJob(
                                        configuration.getDriver(),
                                        SSLSocketFactory.class.getName(),
                                        false);
                                finder.run(new DefaultProgressMonitor(monitor));
                                UIUtils.syncExec(() -> {
                                    sslFactoryCombo.removeAll();
                                    for (String cn : finder.getDriverClassNames()) {
                                        sslFactoryCombo.add(cn);
                                    }
                                    final String factoryValue = configuration.getStringProperty(PostgreConstants.PROP_SSL_FACTORY);
                                    if (!CommonUtils.isEmpty(factoryValue)) {
                                        sslFactoryCombo.setText(factoryValue);
                                    }
                                });
                                return Status.OK_STATUS;
                            }
                        };
                        resolveJob.schedule();
                    }
                }
            };
            sslFactoryCombo.addPaintListener(paintListener);
        }
    }

    @Override
    public void saveSettings(@NotNull DBWHandlerConfiguration configuration) {
        super.saveSettings(configuration);

        if (this.getEditIntention() != DBPConnectionEditIntention.CREDENTIALS_ONLY) {
            configuration.setProperty(PostgreConstants.PROP_SSL_MODE, sslModeCombo.getText());
            configuration.setProperty(PostgreConstants.PROP_SSL_FACTORY, sslFactoryCombo.getText());
            if (ENABLE_PROXY) {
                configuration.setProperty(PostgreConstants.PROP_SSL_PROXY, useProxyService.getSelection());
            }
        }
    }
}
