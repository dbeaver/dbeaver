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
package org.jkiss.dbeaver.ui.net.ssh;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;
import org.jkiss.dbeaver.model.net.ssh.SSHTunnelImpl;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationDescriptor;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationRegistry;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * SSH tunnel configuration
 */
public class SSHTunnelConfiguratorUI implements IObjectPropertyConfigurator<DBWHandlerConfiguration> {

    private DBWHandlerConfiguration savedConfiguration;

    private Text hostText;
    private Spinner portText;
    private Text userNameText;
    private Combo authMethodCombo;
    private TextWithOpen privateKeyText;
    private Label pwdLabel;
    private Text passwordText;
    private Button savePasswordCheckbox;
    private Label privateKeyLabel;
    private Combo tunnelImplCombo;
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
            Group settingsGroup = UIUtils.createControlGroup(composite, SSHUIMessages.model_ssh_configurator_group_settings, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, SWT.DEFAULT);

            hostText = UIUtils.createLabelText(settingsGroup, SSHUIMessages.model_ssh_configurator_label_host_ip, null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL)); //$NON-NLS-2$
            portText = UIUtils.createLabelSpinner(settingsGroup, SSHUIMessages.model_ssh_configurator_label_port, SSHConstants.DEFAULT_SSH_PORT, 0, 65535);
            userNameText = UIUtils.createLabelText(settingsGroup, SSHUIMessages.model_ssh_configurator_label_user_name, null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL)); //$NON-NLS-2$

            authMethodCombo = UIUtils.createLabelCombo(settingsGroup, SSHUIMessages.model_ssh_configurator_combo_auth_method, SWT.DROP_DOWN | SWT.READ_ONLY);
            authMethodCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            authMethodCombo.add(SSHUIMessages.model_ssh_configurator_combo_password);
            authMethodCombo.add(SSHUIMessages.model_ssh_configurator_combo_pub_key);

            privateKeyLabel = UIUtils.createControlLabel(settingsGroup, SSHUIMessages.model_ssh_configurator_label_private_key);
            privateKeyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            privateKeyText = new TextWithOpenFile(
                settingsGroup,
                SSHUIMessages.model_ssh_configurator_dialog_choose_private_key,
                new String[]{"*", "*.ssh", "*.pem", "*.*"});
            privateKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            {
                pwdLabel = UIUtils.createControlLabel(settingsGroup, SSHUIMessages.model_ssh_configurator_label_password);

                passwordText = new Text(settingsGroup, SWT.BORDER | SWT.PASSWORD);
                passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                savePasswordCheckbox = UIUtils.createLabelCheckbox(settingsGroup, SSHUIMessages.model_ssh_configurator_checkbox_save_pass, false);
            }
        }

        {
            Group advancedGroup = UIUtils.createControlGroup(composite, SSHUIMessages.model_ssh_configurator_group_advanced, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, SWT.DEFAULT);

            tunnelImplCombo = UIUtils.createLabelCombo(advancedGroup, SSHUIMessages.model_ssh_configurator_label_implementation, SWT.DROP_DOWN | SWT.READ_ONLY);
            tunnelImplCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            for (SSHImplementationDescriptor it : SSHImplementationRegistry.getInstance().getDescriptors()) {
                tunnelImplCombo.add(it.getLabel());
            }
            localPortSpinner = UIUtils.createLabelSpinner(advancedGroup, SSHUIMessages.model_ssh_configurator_label_local_port, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            localPortSpinner.setToolTipText(SSHUIMessages.model_ssh_configurator_label_local_port_description);
            keepAliveText = UIUtils.createLabelSpinner(advancedGroup, SSHUIMessages.model_ssh_configurator_label_keep_alive, 0, 0, Integer.MAX_VALUE);
            tunnelTimeout = UIUtils.createLabelSpinner(advancedGroup, SSHUIMessages.model_ssh_configurator_label_tunnel_timeout, SSHConstants.DEFAULT_CONNECT_TIMEOUT, 0, 300000);
        }

        Composite controlGroup = UIUtils.createPlaceholder(composite, 1);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        //gd.horizontalSpan = 2;
        controlGroup.setLayoutData(gd);

        Button testButton = UIUtils.createPushButton(controlGroup, SSHUIMessages.model_ssh_configurator_button_test_tunnel, null, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testTunnelConnection();
            }
        });
        //new Label(controlGroup, SWT.NONE);

        authMethodCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updatePrivateKeyVisibility();
            }
        });

    }

    private void testTunnelConnection() {
        DBWHandlerConfiguration configuration = new DBWHandlerConfiguration(savedConfiguration);
        saveSettings(configuration);

        try {
            final String[] tunnelVersions = new String[2];
            UIUtils.runInProgressDialog(monitor -> {
                monitor.beginTask("Instantiate SSH tunnel", 2);
                SSHTunnelImpl tunnel = new SSHTunnelImpl();
                DBPConnectionConfiguration connectionConfig = new DBPConnectionConfiguration();
                connectionConfig.setHostName("localhost");
                connectionConfig.setHostPort(configuration.getProperties().get(SSHConstants.PROP_PORT));
                try {
                    monitor.subTask("Initialize tunnel");
                    tunnel.initializeHandler(monitor, DBeaverCore.getInstance(), configuration, connectionConfig);
                    monitor.worked(1);
                    // Get info
                    tunnelVersions[0] = tunnel.getImplementation().getClientVersion();
                    tunnelVersions[1] = tunnel.getImplementation().getServerVersion();

                    // Close it
                    monitor.subTask("Close tunnel");
                    tunnel.closeTunnel(monitor);
                    monitor.worked(1);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
                monitor.done();
            });

            MessageDialog.openInformation(hostText.getShell(), CoreMessages.dialog_connection_wizard_start_connection_monitor_success,
                "Connected!\n\nClient version: " + tunnelVersions[0] + "\nServer version: " + tunnelVersions[1]);
        } catch (InvocationTargetException ex) {
            DBUserInterface.getInstance().showError(
                CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                null,
                GeneralUtils.makeExceptionStatus(ex.getTargetException()));
        }
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

        String implType = configuration.getProperties().get(SSHConstants.PROP_IMPLEMENTATION);
        if (CommonUtils.isEmpty(implType)) {
            tunnelImplCombo.select(0);
        } else {
            SSHImplementationDescriptor desc = SSHImplementationRegistry.getInstance().getDescriptor(implType);
            if (desc != null) {
                tunnelImplCombo.setText(desc.getLabel());
            } else {
                tunnelImplCombo.select(0);
            }
        }

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

        savedConfiguration = new DBWHandlerConfiguration(configuration);
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

        String implLabel = tunnelImplCombo.getText();
        for (SSHImplementationDescriptor it : SSHImplementationRegistry.getInstance().getDescriptors()) {
            if (it.getLabel().equals(implLabel)) {
                properties.put(SSHConstants.PROP_IMPLEMENTATION, it.getId());
                break;
            }
        }
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
        pwdLabel.setText(isPassword ? SSHUIMessages.model_ssh_configurator_label_password : SSHUIMessages.model_ssh_configurator_label_passphrase);

        UIUtils.asyncExec(() -> hostText.getParent().getParent().layout(true, true));
    }

    @Override
    public boolean isComplete()
    {
        return false;
    }
}
