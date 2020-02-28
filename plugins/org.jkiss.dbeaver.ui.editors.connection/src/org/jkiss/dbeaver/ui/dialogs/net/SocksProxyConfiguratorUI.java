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
package org.jkiss.dbeaver.ui.dialogs.net;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.impl.net.SocksConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

/**
 * SOCKS proxy configuration
 */
public class SocksProxyConfiguratorUI implements IObjectPropertyConfigurator<DBWHandlerConfiguration> {

    private Text hostText;
    private Spinner portText;
    private Text userNameText;
    private Text passwordText;
    private Button savePasswordCheckbox;

    @Override
    public void createControl(Composite parent)
    {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        createSocksGroup(parent);
    }

    protected void createSocksGroup(Composite parent) {
        final Composite composite = UIUtils.createControlGroup(parent, "SOCKS", 4, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        hostText = UIUtils.createLabelText(composite, UIConnectionMessages.dialog_connection_network_socket_label_host, null); //$NON-NLS-2$
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        portText = UIUtils.createLabelSpinner(composite, UIConnectionMessages.dialog_connection_network_socket_label_port, SocksConstants.DEFAULT_SOCKS_PORT, 0, 65535);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = 50;
        portText.setLayoutData(gd);

        userNameText = UIUtils.createLabelText(composite, UIConnectionMessages.dialog_connection_network_socket_label_username, null); //$NON-NLS-2$
        userNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        passwordText = UIUtils.createLabelText(composite, UIConnectionMessages.dialog_connection_network_socket_label_password, "", SWT.BORDER | SWT.PASSWORD); //$NON-NLS-2$
        UIUtils.createEmptyLabel(composite,1, 1);
        savePasswordCheckbox = UIUtils.createCheckbox(composite, UIConnectionMessages.dialog_connection_auth_checkbox_save_password, false);
    }

    @Override
    public void loadSettings(DBWHandlerConfiguration configuration)
    {
        hostText.setText(CommonUtils.notEmpty(configuration.getStringProperty(SocksConstants.PROP_HOST)));
        String portString = configuration.getStringProperty(SocksConstants.PROP_PORT);
        if (!CommonUtils.isEmpty(portString)) {
            portText.setSelection(CommonUtils.toInt(portString));
        } else {
            portText.setSelection(SocksConstants.DEFAULT_SOCKS_PORT);
        }
        userNameText.setText(CommonUtils.notEmpty(configuration.getUserName()));
        passwordText.setText(CommonUtils.notEmpty(configuration.getPassword()));
        savePasswordCheckbox.setSelection(configuration.isSavePassword());
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration)
    {
        configuration.setProperty(SocksConstants.PROP_HOST, hostText.getText());
        configuration.setProperty(SocksConstants.PROP_PORT, portText.getSelection());
        configuration.setUserName(userNameText.getText());
        configuration.setPassword(passwordText.getText());
        configuration.setSavePassword(savePasswordCheckbox.getSelection());
    }

    @Override
    public void resetSettings(DBWHandlerConfiguration configuration) {

    }

    @Override
    public boolean isComplete()
    {
        return false;
    }
}
