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
package org.jkiss.dbeaver.ui.dialogs.net;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.utils.CommonUtils;

/**
 * CasSSLConfigurator
 */
public class SSLConfiguratorTrustStoreUI extends SSLConfiguratorAbstractUI
{
    private TextWithOpen caCertPath;
    private TextWithOpen clientCertPath;
    private TextWithOpen clientKeyPath;

    @Override
    public void createControl(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);

        createTrustStoreConfigGroup(composite);
    }

    protected Composite createTrustStoreConfigGroup(Composite composite) {
        Group certGroup = UIUtils.createControlGroup(composite, "Client Certificate", 2, GridData.FILL_HORIZONTAL, -1);

        if (useCACertificate()) {
            UIUtils.createControlLabel(certGroup, "CA Certificate");
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.minimumWidth = 130;
            caCertPath = new TextWithOpenFile(certGroup, "CA Certificate", new String[]{"*.*", "*.crt", "*"});
            caCertPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        UIUtils.createControlLabel(certGroup, "Client Certificate");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 130;
        clientCertPath = new TextWithOpenFile(certGroup, "Client Certificate", new String[]{"*.*", "*.crt", "*"});
        clientCertPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createControlLabel(certGroup, "Client Private Key");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 130;
        clientKeyPath = new TextWithOpenFile(certGroup, "Client Key", new String[]{"*.*", "*.key", "*"});
        clientKeyPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        return certGroup;
    }

    protected boolean useCACertificate() {
        return false;
    }

    @Override
    public void loadSettings(DBWHandlerConfiguration configuration) {
        if (caCertPath != null) {
            caCertPath.setText(CommonUtils.notEmpty(configuration.getProperties().get(SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT)));
        }
        clientCertPath.setText(CommonUtils.notEmpty(configuration.getProperties().get(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT)));
        clientKeyPath.setText(CommonUtils.notEmpty(configuration.getProperties().get(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY)));
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration) {
        if (caCertPath != null) {
            configuration.getProperties().put(SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT, caCertPath.getText());
        }
        configuration.getProperties().put(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT, clientCertPath.getText());
        configuration.getProperties().put(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, clientKeyPath.getText());
    }
}
