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
    private Composite pwdControlGroup;
    private Text passwordText;
    private Button savePasswordCheckbox;
    private Label privateKeyLabel;
    private Composite pkControlGroup;
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

        authMethodCombo = UIUtils.createLabelCombo(composite, CoreMessages.model_ssh_configurator_combo_auth_method, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 130;
        authMethodCombo.setLayoutData(gd);
        authMethodCombo.add(CoreMessages.model_ssh_configurator_combo_password);
        authMethodCombo.add(CoreMessages.model_ssh_configurator_combo_pub_key);

        privateKeyLabel = UIUtils.createControlLabel(composite, CoreMessages.model_ssh_configurator_label_private_key);
        privateKeyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        pkControlGroup = UIUtils.createPlaceholder(composite, 1);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 130;
        pkControlGroup.setLayoutData(gd);
        privateKeyText = new TextWithOpenFile(
            pkControlGroup,
            CoreMessages.model_ssh_configurator_dialog_choose_private_key,
            new String[] {"*", "*.ssh", "*.pem", "*.*"});
        privateKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        pwdLabel = UIUtils.createControlLabel(composite, CoreMessages.model_ssh_configurator_label_password);
        pwdControlGroup = UIUtils.createPlaceholder(composite, 3);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 200;
        pwdControlGroup.setLayoutData(gd);

        passwordText = new Text(pwdControlGroup, SWT.BORDER | SWT.PASSWORD);
        passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(pwdControlGroup, SWT.NONE).setText("   ");
        savePasswordCheckbox = UIUtils.createCheckbox(pwdControlGroup, CoreMessages.model_ssh_configurator_checkbox_save_pass, false);

        keepAliveText = UIUtils.createLabelSpinner(composite, CoreMessages.model_ssh_configurator_label_keep_alive, 0, 0, Integer.MAX_VALUE);
        tunnelTimeout = UIUtils.createLabelSpinner(composite, CoreMessages.model_ssh_configurator_label_tunnel_timeout, SSHConstants.DEFAULT_CONNECT_TIMEOUT, 0, 300000);

        authMethodCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updatePrivateKeyVisibility();
                composite.layout();
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
        ((GridData)pkControlGroup.getLayoutData()).exclude = isPassword;
        pkControlGroup.setVisible(!isPassword);
        ((GridData)privateKeyLabel.getLayoutData()).exclude = isPassword;
        privateKeyLabel.setVisible(!isPassword);

//        pwdControlGroup.setVisible(isPassword);
//        ((GridData)pwdControlGroup.getLayoutData()).exclude = !isPassword;
//        pwdLabel.setVisible(isPassword);
//        ((GridData)pwdLabel.getLayoutData()).exclude = !isPassword;
//
//        if (!isPassword) {
//            savePasswordCheckbox.setSelection(true);
//        }
        pwdLabel.setText(isPassword ? CoreMessages.model_ssh_configurator_label_password : CoreMessages.model_ssh_configurator_label_passphrase);
    }

    @Override
    public boolean isComplete()
    {
        return false;
    }
}
