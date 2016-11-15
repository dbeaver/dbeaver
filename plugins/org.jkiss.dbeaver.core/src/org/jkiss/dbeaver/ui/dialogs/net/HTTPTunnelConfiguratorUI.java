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
package org.jkiss.dbeaver.ui.dialogs.net;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.model.impl.net.SSHConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * HTTP tunnel configuration
 */
public class HTTPTunnelConfiguratorUI implements IObjectPropertyConfigurator<DBWHandlerConfiguration> {

    private Text hostText;
    private Spinner portText;
    private Text userNameText;
    private Text passwordText;
    private Button savePasswordCheckbox;
    private Spinner keepAliveText;
    private Spinner tunnelTimeout;

    @Override
    public void createControl(Composite parent)
    {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 200;
        composite.setLayoutData(gd);
        composite.setLayout(new GridLayout(2, false));
        hostText = UIUtils.createLabelText(composite, CoreMessages.model_ssh_configurator_label_host_ip, null); //$NON-NLS-2$
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        portText = UIUtils.createLabelSpinner(composite, CoreMessages.model_ssh_configurator_label_port, SSHConstants.DEFAULT_SSH_PORT, 0, 65535);
        userNameText = UIUtils.createLabelText(composite, CoreMessages.model_ssh_configurator_label_user_name, null); //$NON-NLS-2$
        userNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        passwordText = UIUtils.createLabelText(composite, CoreMessages.model_ssh_configurator_label_password, "", SWT.BORDER | SWT.PASSWORD); //$NON-NLS-2$

        UIUtils.createPlaceholder(composite,1);
        savePasswordCheckbox = UIUtils.createCheckbox(composite, CoreMessages.model_ssh_configurator_checkbox_save_pass, false);

        keepAliveText = UIUtils.createLabelSpinner(composite, CoreMessages.model_ssh_configurator_label_keep_alive, 0, 0, Integer.MAX_VALUE);
        tunnelTimeout = UIUtils.createLabelSpinner(composite, CoreMessages.model_ssh_configurator_label_tunnel_timeout, SSHConstants.DEFAULT_CONNECT_TIMEOUT, 0, 300000);
    }

    @Override
    public void loadSettings(DBWHandlerConfiguration configuration)
    {
        hostText.setText(CommonUtils.notEmpty(configuration.getProperties().get(SSHConstants.PROP_HOST)));
        String portString = configuration.getProperties().get(SSHConstants.PROP_PORT);
        if (!CommonUtils.isEmpty(portString)) {
            portText.setSelection(CommonUtils.toInt(portString));
        }
        userNameText.setText(CommonUtils.notEmpty(configuration.getUserName()));
        SSHConstants.AuthType authType = SSHConstants.AuthType.PASSWORD;
        String authTypeName = configuration.getProperties().get(SSHConstants.PROP_AUTH_TYPE);
        if (!CommonUtils.isEmpty(authTypeName)) {
            authType = SSHConstants.AuthType.valueOf(authTypeName);
        }
        passwordText.setText(CommonUtils.notEmpty(configuration.getPassword()));
        savePasswordCheckbox.setSelection(configuration.isSavePassword());

        String kaString = configuration.getProperties().get(SSHConstants.PROP_ALIVE_INTERVAL);
        if (!CommonUtils.isEmpty(kaString)) {
            keepAliveText.setSelection(Integer.parseInt(kaString));
        }

        String timeoutString = configuration.getProperties().get(SSHConstants.PROP_CONNECT_TIMEOUT);
        if (!CommonUtils.isEmpty(timeoutString)) {
            tunnelTimeout.setSelection(CommonUtils.toInt(timeoutString));
        }
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration)
    {
        Map<String,String> properties = configuration.getProperties();
        properties.clear();
        properties.put(SSHConstants.PROP_HOST, hostText.getText());
        properties.put(SSHConstants.PROP_PORT, portText.getText());
        configuration.setUserName(userNameText.getText());
        configuration.setPassword(passwordText.getText());
        configuration.setSavePassword(savePasswordCheckbox.getSelection());
        int kaInterval = keepAliveText.getSelection();
        if (kaInterval <= 0) {
            properties.remove(SSHConstants.PROP_ALIVE_INTERVAL);
        } else {
            properties.put(SSHConstants.PROP_ALIVE_INTERVAL, String.valueOf(kaInterval));
        }
        properties.put(SSHConstants.PROP_CONNECT_TIMEOUT, tunnelTimeout.getText());
    }

    @Override
    public boolean isComplete()
    {
        return false;
    }
}
