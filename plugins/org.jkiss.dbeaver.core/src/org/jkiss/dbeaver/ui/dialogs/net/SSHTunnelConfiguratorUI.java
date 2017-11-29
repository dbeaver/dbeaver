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
package org.jkiss.dbeaver.ui.dialogs.net;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.impl.net.SSHConstants;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * SSH tunnel configuration
 */
public class SSHTunnelConfiguratorUI implements IObjectPropertyConfigurator<DBWHandlerConfiguration> {

    private Text hostText;
    private Spinner portText;
    private Text userNameText;
    private Combo authMethodCombo;
    private TextWithOpen privateKeyText;
    private Label pwdLabel;
    private Text passwordText;
    private Button savePasswordCheckbox;
    private Label privateKeyLabel;
    private Spinner localPortSpinner;
    private Spinner keepAliveText;
    private Spinner tunnelTimeout;

    @Override
    public void createControl(Composite parent)
    {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        //gd.minimumHeight = 200;
        composite.setLayoutData(gd);
        composite.setLayout(new GridLayout(1, false));

        {
            Group settingsGroup = UIUtils.createControlGroup(composite, CoreMessages.model_ssh_configurator_group_settings, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

            hostText = UIUtils.createLabelText(settingsGroup, CoreMessages.model_ssh_configurator_label_host_ip, null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL)); //$NON-NLS-2$
            portText = UIUtils.createLabelSpinner(settingsGroup, CoreMessages.model_ssh_configurator_label_port, SSHConstants.DEFAULT_SSH_PORT, 0, 65535);
            userNameText = UIUtils.createLabelText(settingsGroup, CoreMessages.model_ssh_configurator_label_user_name, null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL)); //$NON-NLS-2$

            authMethodCombo = UIUtils.createLabelCombo(settingsGroup, CoreMessages.model_ssh_configurator_combo_auth_method, SWT.DROP_DOWN | SWT.READ_ONLY);
            authMethodCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            authMethodCombo.add(CoreMessages.model_ssh_configurator_combo_password);
            authMethodCombo.add(CoreMessages.model_ssh_configurator_combo_pub_key);

            privateKeyLabel = UIUtils.createControlLabel(settingsGroup, CoreMessages.model_ssh_configurator_label_private_key);
            privateKeyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            privateKeyText = new TextWithOpenFile(
                settingsGroup,
                CoreMessages.model_ssh_configurator_dialog_choose_private_key,
                new String[]{"*", "*.ssh", "*.pem", "*.*"});
            privateKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            {
                pwdLabel = UIUtils.createControlLabel(settingsGroup, CoreMessages.model_ssh_configurator_label_password);

                passwordText = new Text(settingsGroup, SWT.BORDER | SWT.PASSWORD);
                passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                savePasswordCheckbox = UIUtils.createLabelCheckbox(settingsGroup, CoreMessages.model_ssh_configurator_checkbox_save_pass, false);
            }
        }

        {
            Group advancedGroup = UIUtils.createControlGroup(composite, CoreMessages.model_ssh_configurator_group_advanced, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

            localPortSpinner = UIUtils.createLabelSpinner(advancedGroup, CoreMessages.model_ssh_configurator_label_local_port, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            localPortSpinner.setToolTipText(CoreMessages.model_ssh_configurator_label_local_port_description);
            keepAliveText = UIUtils.createLabelSpinner(advancedGroup, CoreMessages.model_ssh_configurator_label_keep_alive, 0, 0, Integer.MAX_VALUE);
            tunnelTimeout = UIUtils.createLabelSpinner(advancedGroup, CoreMessages.model_ssh_configurator_label_tunnel_timeout, SSHConstants.DEFAULT_CONNECT_TIMEOUT, 0, 300000);
        }

        authMethodCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updatePrivateKeyVisibility();
            }
        });

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
        authMethodCombo.select(authType == SSHConstants.AuthType.PASSWORD ? 0 : 1);
        privateKeyText.setText(CommonUtils.notEmpty(configuration.getProperties().get(SSHConstants.PROP_KEY_PATH)));
        passwordText.setText(CommonUtils.notEmpty(configuration.getPassword()));
        savePasswordCheckbox.setSelection(configuration.isSavePassword());

        String lpString = configuration.getProperties().get(SSHConstants.PROP_LOCAL_PORT);
        if (!CommonUtils.isEmpty(lpString)) {
            localPortSpinner.setSelection(Integer.parseInt(lpString));
        }

        String kaString = configuration.getProperties().get(SSHConstants.PROP_ALIVE_INTERVAL);
        if (!CommonUtils.isEmpty(kaString)) {
            keepAliveText.setSelection(Integer.parseInt(kaString));
        }

        String timeoutString = configuration.getProperties().get(SSHConstants.PROP_CONNECT_TIMEOUT);
        if (!CommonUtils.isEmpty(timeoutString)) {
            tunnelTimeout.setSelection(CommonUtils.toInt(timeoutString));
        }
        updatePrivateKeyVisibility();
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration)
    {
        Map<String,String> properties = configuration.getProperties();
        properties.clear();
        properties.put(SSHConstants.PROP_HOST, hostText.getText());
        properties.put(SSHConstants.PROP_PORT, portText.getText());
        properties.put(SSHConstants.PROP_AUTH_TYPE,
            authMethodCombo.getSelectionIndex() == 0 ?
                SSHConstants.AuthType.PASSWORD.name() :
                SSHConstants.AuthType.PUBLIC_KEY.name());
        properties.put(SSHConstants.PROP_KEY_PATH, privateKeyText.getText());
        configuration.setUserName(userNameText.getText());
        configuration.setPassword(passwordText.getText());
        configuration.setSavePassword(savePasswordCheckbox.getSelection());

        int localPort = localPortSpinner.getSelection();
        if (localPort <= 0) {
            properties.remove(SSHConstants.PROP_LOCAL_PORT);
        } else {
            properties.put(SSHConstants.PROP_LOCAL_PORT, String.valueOf(localPort));
        }

        int kaInterval = keepAliveText.getSelection();
        if (kaInterval <= 0) {
            properties.remove(SSHConstants.PROP_ALIVE_INTERVAL);
        } else {
            properties.put(SSHConstants.PROP_ALIVE_INTERVAL, String.valueOf(kaInterval));
        }
        properties.put(SSHConstants.PROP_CONNECT_TIMEOUT, tunnelTimeout.getText());
    }

    private void updatePrivateKeyVisibility()
    {
        boolean isPassword = authMethodCombo.getSelectionIndex() == 0;

        ((GridData)privateKeyLabel.getLayoutData()).exclude = isPassword;
        privateKeyLabel.setVisible(!isPassword);
        ((GridData)privateKeyText.getLayoutData()).exclude = isPassword;
        privateKeyText.setVisible(!isPassword);

//        pwdControlGroup.setVisible(isPassword);
//        ((GridData)pwdControlGroup.getLayoutData()).exclude = !isPassword;
//        pwdLabel.setVisible(isPassword);
//        ((GridData)pwdLabel.getLayoutData()).exclude = !isPassword;
//
//        if (!isPassword) {
//            savePasswordCheckbox.setSelection(true);
//        }
        pwdLabel.setText(isPassword ? CoreMessages.model_ssh_configurator_label_password : CoreMessages.model_ssh_configurator_label_passphrase);

        DBeaverUI.asyncExec(new Runnable() {
            @Override
            public void run() {
                hostText.getParent().getParent().layout(true, true);
            }
        });
    }

    @Override
    public boolean isComplete()
    {
        return false;
    }
}
