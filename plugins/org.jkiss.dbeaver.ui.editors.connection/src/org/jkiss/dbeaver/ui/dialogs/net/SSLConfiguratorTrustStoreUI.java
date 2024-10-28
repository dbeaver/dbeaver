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
package org.jkiss.dbeaver.ui.dialogs.net;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.net.SSLConfigurationMethod;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.configurator.DBPConnectionEditIntention;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ConfigurationFileSelector;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

/**
 * CasSSLConfigurator
 */
public class SSLConfiguratorTrustStoreUI extends SSLConfiguratorAbstractUI {
    private Button certRadioButton;
    private Button keyStoreRadioButton;

    protected TextWithOpen caCertPath;
    protected TextWithOpen clientCertPath;
    protected TextWithOpen clientKeyPath;
    protected TextWithOpen keyStorePath;
    protected Text keyStorePassword;
    protected SSLConfigurationMethod method;

    protected Composite sslCertComposite;
    protected Composite sslKeyStoreComposite;

    @Override
    public void createControl(@NotNull Composite parent, Object object, @NotNull Runnable propertyChangeListener) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);

        createTrustStoreConfigGroup(composite);
    }

    protected Group createTrustStoreConfigGroup(Composite composite) {
        final boolean certificatesSupported = isCertificatesSupported();
        final boolean keyStoreSupported = isKeystoreSupported();

        assert certificatesSupported || keyStoreSupported;

        Group sslParameters = UIUtils.createControlGroup(composite, UIConnectionMessages.dialog_setting_ssl_configurator_legend_parameters, 1, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        if (certificatesSupported && keyStoreSupported) {
            final SelectionAdapter methodSwitcher = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showMethodControls((SSLConfigurationMethod) e.widget.getData());
                }
            };

            Composite sslMethodComposite = UIUtils.createComposite(sslParameters, 3);

            UIUtils.createControlLabel(sslMethodComposite, UIConnectionMessages.dialog_setting_ssl_configurator_method);
            certRadioButton = UIUtils.createRadioButton(sslMethodComposite, UIConnectionMessages.dialog_setting_ssl_configurator_method_certs, SSLConfigurationMethod.CERTIFICATES, methodSwitcher);
            keyStoreRadioButton = UIUtils.createRadioButton(sslMethodComposite, UIConnectionMessages.dialog_setting_ssl_configurator_method_keystore, SSLConfigurationMethod.KEYSTORE, methodSwitcher);
        }

        {
            sslCertComposite = UIUtils.createComposite(sslParameters, 2);
            sslCertComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            if (useCACertificate()) {
                UIUtils.createControlLabel(sslCertComposite, UIConnectionMessages.dialog_setting_ssl_configurator_certs_ca_name);
                caCertPath = new ConfigurationFileSelector(sslCertComposite, UIConnectionMessages.dialog_setting_ssl_configurator_certs_ca_title, new String[]{"*.*", "*.crt", "*"});
                caCertPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            }

            UIUtils.createControlLabel(sslCertComposite, UIConnectionMessages.dialog_setting_ssl_configurator_certs_client_name);
            clientCertPath = new ConfigurationFileSelector(sslCertComposite, UIConnectionMessages.dialog_setting_ssl_configurator_certs_client_title, new String[]{"*.*", "*.crt", "*"});
            clientCertPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(sslCertComposite, UIConnectionMessages.dialog_setting_ssl_configurator_certs_client_key_name);
            clientKeyPath = new ConfigurationFileSelector(sslCertComposite, UIConnectionMessages.dialog_setting_ssl_configurator_certs_client_key_title, new String[]{"*.*", "*.key", "*"});
            clientKeyPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        {
            sslKeyStoreComposite = UIUtils.createComposite(sslParameters, 2);
            sslKeyStoreComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(sslKeyStoreComposite, UIConnectionMessages.dialog_setting_ssl_configurator_keystore_name);
            keyStorePath = new ConfigurationFileSelector(sslKeyStoreComposite, UIConnectionMessages.dialog_setting_ssl_configurator_keystore_title, new String[]{"*.jks;*.pfx", "*.*"}, true);
            keyStorePath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(sslKeyStoreComposite, UIConnectionMessages.dialog_setting_ssl_configurator_keystore_password_name);
            keyStorePassword = new Text(sslKeyStoreComposite, SWT.BORDER | SWT.PASSWORD);
            keyStorePassword.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        return sslParameters;
    }

    private void showMethodControls(SSLConfigurationMethod configurationMethod) {
        boolean keystore = configurationMethod == SSLConfigurationMethod.KEYSTORE;
        showControl(sslCertComposite, !keystore);
        showControl(sslKeyStoreComposite, keystore);
        sslCertComposite.getParent().getParent().layout(true, true);
        method = configurationMethod;
    }

    private void showControl(Control control, boolean flag) {
        UIUtils.setControlVisible(control, flag);
    }

    protected boolean isCertificatesSupported() {
        return true;
    }

    protected boolean isKeystoreSupported() {
        return false;
    }

    protected boolean useCACertificate() {
        return false;
    }

    @Override
    public void loadSettings(@NotNull DBWHandlerConfiguration configuration) {
        if (isCertificatesSupported()) {
            if (caCertPath != null) {
                caCertPath.setText(getCertProperty(configuration, SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT));
            }
            clientCertPath.setText(getCertProperty(configuration, SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT));
            clientKeyPath.setText(getCertProperty(configuration, SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY));
        }

        if (isKeystoreSupported()) {
            keyStorePath.setText(getCertProperty(configuration, SSLHandlerTrustStoreImpl.PROP_SSL_KEYSTORE));
            keyStorePassword.setText(CommonUtils.notEmpty(
                DBWorkbench.isDistributed() ?
                    configuration.getSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_KEYSTORE_PASSWORD) :
                    configuration.getPassword()));
        }

        final SSLConfigurationMethod method;

        if (isCertificatesSupported() && isKeystoreSupported()) {
            method = CommonUtils.valueOf(
                SSLConfigurationMethod.class,
                configuration.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD),
                SSLConfigurationMethod.CERTIFICATES);

            if (method == SSLConfigurationMethod.CERTIFICATES) {
                certRadioButton.setSelection(true);
            } else {
                keyStoreRadioButton.setSelection(true);
            }
        } else {
            method = isCertificatesSupported()
                ? SSLConfigurationMethod.CERTIFICATES
                : SSLConfigurationMethod.KEYSTORE;
        }

        showMethodControls(method);
    }

    private String getCertProperty(DBWHandlerConfiguration configuration, String basePropName) {
        if (DBWorkbench.isDistributed()) {
            return CommonUtils.notEmpty(configuration.getSecureProperty(basePropName + SSLHandlerTrustStoreImpl.CERT_VALUE_SUFFIX));
        } else {
            return CommonUtils.notEmpty(configuration.getStringProperty(basePropName));
        }
    }

    private void setCertProperty(DBWHandlerConfiguration configuration, String basePropName, String propValue) {
        if (DBWorkbench.isDistributed()) {
            configuration.setSecureProperty(basePropName + SSLHandlerTrustStoreImpl.CERT_VALUE_SUFFIX, propValue);
        } else {
            configuration.setProperty(basePropName, propValue);
        }
    }

    @Override
    public void saveSettings(@NotNull DBWHandlerConfiguration configuration) {
        if (isCertificatesSupported()) {
            if (caCertPath != null) {
                setCertProperty(configuration, SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT, caCertPath.getText().trim());
            }
            setCertProperty(configuration, SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT, clientCertPath.getText().trim());
            setCertProperty(configuration, SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, clientKeyPath.getText().trim());
        }

        if (isKeystoreSupported()) {
            setCertProperty(configuration, SSLHandlerTrustStoreImpl.PROP_SSL_KEYSTORE, keyStorePath.getText().trim());

            final String password = keyStorePassword.getText().trim();
            if (!CommonUtils.isEmptyTrimmed(password)) {
                if (DBWorkbench.isDistributed()) {
                    configuration.setSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_KEYSTORE_PASSWORD, password);
                } else {
                    configuration.setPassword(password);
                }
                configuration.setSavePassword(true);
            }
        } else if (this.getEditIntention() == DBPConnectionEditIntention.CREDENTIALS_ONLY) {
            configuration.setSavePassword(true);
        }

        configuration.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD, method.name());
    }
}
