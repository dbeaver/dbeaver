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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;
import org.jkiss.dbeaver.model.net.ssh.SSHTunnelImpl;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationDescriptor;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationRegistry;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

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

    private Text localHostText;
    private Spinner localPortSpinner;
    private Text remoteHostText;
    private Spinner remotePortSpinner;

    private Spinner keepAliveText;
    private Spinner tunnelTimeout;
    private VariablesHintLabel variablesHintLabel;

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

            UIUtils.createControlLabel(settingsGroup, SSHUIMessages.model_ssh_configurator_label_host_ip);
            Composite hostPortComp = UIUtils.createComposite(settingsGroup, 3);
            hostPortComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            hostText = new Text(hostPortComp, SWT.BORDER); //$NON-NLS-2$
            hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            portText = UIUtils.createLabelSpinner(hostPortComp, SSHUIMessages.model_ssh_configurator_label_port, SSHConstants.DEFAULT_SSH_PORT, StandardConstants.MIN_PORT_VALUE, StandardConstants.MAX_PORT_VALUE);

            userNameText = UIUtils.createLabelText(settingsGroup, SSHUIMessages.model_ssh_configurator_label_user_name, null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL)); //$NON-NLS-2$

            authMethodCombo = UIUtils.createLabelCombo(settingsGroup, SSHUIMessages.model_ssh_configurator_combo_auth_method, SWT.DROP_DOWN | SWT.READ_ONLY);
            authMethodCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            authMethodCombo.add(SSHUIMessages.model_ssh_configurator_combo_password);
            authMethodCombo.add(SSHUIMessages.model_ssh_configurator_combo_pub_key);
            authMethodCombo.add(SSHUIMessages.model_ssh_configurator_combo_agent);

            privateKeyLabel = UIUtils.createControlLabel(settingsGroup, SSHUIMessages.model_ssh_configurator_label_private_key);
            privateKeyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            privateKeyText = new TextWithOpenFile(
                settingsGroup,
                SSHUIMessages.model_ssh_configurator_dialog_choose_private_key,
                new String[]{"*", "*.ssh", "*.pem", "*.*"});
            privateKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            {
                pwdLabel = UIUtils.createControlLabel(settingsGroup, SSHUIMessages.model_ssh_configurator_label_password);
                Composite passComp = UIUtils.createComposite(settingsGroup, 2);
                passComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                passwordText = new Text(passComp, SWT.BORDER | SWT.PASSWORD);
                passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                savePasswordCheckbox = UIUtils.createCheckbox(passComp, SSHUIMessages.model_ssh_configurator_checkbox_save_pass, false);
                savePasswordCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            }
        }

        {
            Group advancedGroup = UIUtils.createControlGroup(composite, SSHUIMessages.model_ssh_configurator_group_advanced, 4, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, SWT.DEFAULT);

            tunnelImplCombo = UIUtils.createLabelCombo(advancedGroup, SSHUIMessages.model_ssh_configurator_label_implementation, SWT.DROP_DOWN | SWT.READ_ONLY);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 3;
            tunnelImplCombo.setLayoutData(gd);
            for (SSHImplementationDescriptor it : SSHImplementationRegistry.getInstance().getDescriptors()) {
                tunnelImplCombo.add(it.getLabel());
            }

            localHostText = UIUtils.createLabelText(advancedGroup, SSHUIMessages.model_ssh_configurator_label_local_host, null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            localHostText.setToolTipText(SSHUIMessages.model_ssh_configurator_label_local_host_description);
            remoteHostText = UIUtils.createLabelText(advancedGroup, SSHUIMessages.model_ssh_configurator_label_remote_host, null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            remoteHostText.setToolTipText(SSHUIMessages.model_ssh_configurator_label_remote_host_description);
            localPortSpinner = UIUtils.createLabelSpinner(advancedGroup, SSHUIMessages.model_ssh_configurator_label_local_port, 0, StandardConstants.MIN_PORT_VALUE, StandardConstants.MAX_PORT_VALUE);
            localPortSpinner.setToolTipText(SSHUIMessages.model_ssh_configurator_label_local_port_description);
            remotePortSpinner = UIUtils.createLabelSpinner(advancedGroup, SSHUIMessages.model_ssh_configurator_label_remote_port, 0, StandardConstants.MIN_PORT_VALUE, StandardConstants.MAX_PORT_VALUE);
            remotePortSpinner.setToolTipText(SSHUIMessages.model_ssh_configurator_label_remote_port_description);

            UIUtils.createHorizontalLine(advancedGroup, 4, 0);

            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);

            keepAliveText = UIUtils.createLabelSpinner(advancedGroup, SSHUIMessages.model_ssh_configurator_label_keep_alive, 0, 0, Integer.MAX_VALUE);
            keepAliveText.setLayoutData(gd);
            tunnelTimeout = UIUtils.createLabelSpinner(advancedGroup, SSHUIMessages.model_ssh_configurator_label_tunnel_timeout, SSHConstants.DEFAULT_CONNECT_TIMEOUT, 0, 300000);
            tunnelTimeout.setLayoutData(gd);
        }

        {
            Composite controlGroup = UIUtils.createComposite(composite, 2);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            controlGroup.setLayoutData(gd);

            UIUtils.createDialogButton(controlGroup, SSHUIMessages.model_ssh_configurator_button_test_tunnel, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    testTunnelConnection();
                }
            });
            String hint = "You can use variables in SSH parameters.";
            variablesHintLabel = new VariablesHintLabel(controlGroup, hint, hint, DataSourceDescriptor.CONNECT_VARIABLES, false);
        }

        authMethodCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                updateAuthMethodVisibility();
            }
        });

    }

    private void testTunnelConnection() {
        DBWHandlerConfiguration configuration = new DBWHandlerConfiguration(savedConfiguration);
        configuration.setProperties(Collections.emptyMap());
        saveSettings(configuration);
        DBPDataSourceContainer dataSource = configuration.getDataSource();
        if (dataSource != null) {
            configuration.resolveDynamicVariables(
                new DataSourceVariableResolver(
                    dataSource,
                    dataSource == null ? null : dataSource.getConnectionConfiguration()));
        }

        try {
            final String[] tunnelVersions = new String[2];
            UIUtils.runInProgressDialog(monitor -> {
                monitor.beginTask("Instantiate SSH tunnel", 2);
                SSHTunnelImpl tunnel = new SSHTunnelImpl();
                DBPConnectionConfiguration connectionConfig = new DBPConnectionConfiguration();
                connectionConfig.setHostName("localhost");
                connectionConfig.setHostPort(configuration.getStringProperty(SSHConstants.PROP_PORT));
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
            DBWorkbench.getPlatformUI().showError(
                CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                null,
                GeneralUtils.makeExceptionStatus(ex.getTargetException()));
        }
    }

    @Override
    public void loadSettings(DBWHandlerConfiguration configuration)
    {
        hostText.setText(CommonUtils.notEmpty(configuration.getStringProperty(SSHConstants.PROP_HOST)));
        int portString = configuration.getIntProperty(SSHConstants.PROP_PORT);
        if (portString != 0) {
            portText.setSelection(portString);
        } else {
            portText.setSelection(SSHConstants.DEFAULT_SSH_PORT);
        }
        userNameText.setText(CommonUtils.notEmpty(configuration.getUserName()));
        SSHConstants.AuthType authType = SSHConstants.AuthType.PASSWORD;
        String authTypeName = configuration.getStringProperty(SSHConstants.PROP_AUTH_TYPE);
        if (!CommonUtils.isEmpty(authTypeName)) {
            authType = SSHConstants.AuthType.valueOf(authTypeName);
        }
        if (SSHConstants.AuthType.PASSWORD.equals(authType)) {
            authMethodCombo.select(0);
        } else if (SSHConstants.AuthType.PUBLIC_KEY.equals(authType)) {
            authMethodCombo.select(1);
        } else {
            authMethodCombo.select(2);
        }
        privateKeyText.setText(CommonUtils.notEmpty(configuration.getStringProperty(SSHConstants.PROP_KEY_PATH)));
        passwordText.setText(CommonUtils.notEmpty(configuration.getPassword()));
        savePasswordCheckbox.setSelection(configuration.isSavePassword());

        String implType = configuration.getStringProperty(SSHConstants.PROP_IMPLEMENTATION);
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

        localHostText.setText(CommonUtils.notEmpty(configuration.getStringProperty(SSHConstants.PROP_LOCAL_HOST)));
        int lpValue = configuration.getIntProperty(SSHConstants.PROP_LOCAL_PORT);
        if (lpValue != 0) {
            localPortSpinner.setSelection(lpValue);
        }

        remoteHostText.setText(CommonUtils.notEmpty(configuration.getStringProperty(SSHConstants.PROP_REMOTE_HOST)));
        int rpValue = configuration.getIntProperty(SSHConstants.PROP_REMOTE_PORT);
        if (rpValue != 0) {
            remotePortSpinner.setSelection(rpValue);
        }

        int kaValue = configuration.getIntProperty(SSHConstants.PROP_ALIVE_INTERVAL);
        if (kaValue != 0) {
            keepAliveText.setSelection(kaValue);
        }

        int timeoutValue = configuration.getIntProperty(SSHConstants.PROP_CONNECT_TIMEOUT);
        if (timeoutValue != 0) {
            tunnelTimeout.setSelection(timeoutValue);
        }
        updateAuthMethodVisibility();

        savedConfiguration = new DBWHandlerConfiguration(configuration);

        DBPDataSourceContainer dataSource = savedConfiguration.getDataSource();
        if (dataSource != null) {
            variablesHintLabel.setResolver(
                new DataSourceVariableResolver(
                    dataSource,
                    dataSource.getConnectionConfiguration()));
        }
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration)
    {
        configuration.setProperty(SSHConstants.PROP_HOST, hostText.getText());
        configuration.setProperty(SSHConstants.PROP_PORT, portText.getSelection());
        switch (authMethodCombo.getSelectionIndex()) {
            case 0: configuration.setProperty(SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.PASSWORD.name()); break;
            case 1: configuration.setProperty(SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.PUBLIC_KEY.name()); break;
            case 2: configuration.setProperty(SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.AGENT.name()); break;
        }
        configuration.setProperty(SSHConstants.PROP_KEY_PATH, privateKeyText.getText());
        configuration.setUserName(userNameText.getText());
        configuration.setPassword(passwordText.getText());
        configuration.setSavePassword(savePasswordCheckbox.getSelection());

        String implLabel = tunnelImplCombo.getText();
        for (SSHImplementationDescriptor it : SSHImplementationRegistry.getInstance().getDescriptors()) {
            if (it.getLabel().equals(implLabel)) {
                configuration.setProperty(SSHConstants.PROP_IMPLEMENTATION, it.getId());
                break;
            }
        }

        configuration.setProperty(SSHConstants.PROP_LOCAL_HOST, localHostText.getText());
        int localPort = localPortSpinner.getSelection();
        if (localPort <= 0) {
            configuration.setProperty(SSHConstants.PROP_LOCAL_PORT, null);
        } else {
            configuration.setProperty(SSHConstants.PROP_LOCAL_PORT, localPort);
        }

        configuration.setProperty(SSHConstants.PROP_REMOTE_HOST, remoteHostText.getText());
        int remotePort = remotePortSpinner.getSelection();
        if (remotePort <= 0) {
            configuration.setProperty(SSHConstants.PROP_REMOTE_PORT, null);
        } else {
            configuration.setProperty(SSHConstants.PROP_REMOTE_PORT, remotePort);
        }

        int kaInterval = keepAliveText.getSelection();
        if (kaInterval <= 0) {
            configuration.setProperty(SSHConstants.PROP_ALIVE_INTERVAL, null);
        } else {
            configuration.setProperty(SSHConstants.PROP_ALIVE_INTERVAL, kaInterval);
        }
        int conTimeout = tunnelTimeout.getSelection();
        if (conTimeout != 0 && conTimeout != SSHConstants.DEFAULT_CONNECT_TIMEOUT) {
            configuration.setProperty(SSHConstants.PROP_CONNECT_TIMEOUT, conTimeout);
        }
    }

    @Override
    public void resetSettings(DBWHandlerConfiguration configuration) {

    }

    private void updateAuthMethodVisibility()
    {
        boolean isPassword = authMethodCombo.getSelectionIndex() == 0;
        boolean isPublicKey = authMethodCombo.getSelectionIndex() == 1;
        boolean isAgent = authMethodCombo.getSelectionIndex() == 2;

        if (isPublicKey) {
            showPrivateKeyField(true);
            showPasswordField(true, SSHUIMessages.model_ssh_configurator_label_passphrase);
        } else if (isAgent) {
            showPrivateKeyField(false);
            showPasswordField(false, null);
        } else if (isPassword) {
            showPrivateKeyField(false);
            showPasswordField(true, SSHUIMessages.model_ssh_configurator_label_password);
        }

        hostText.getParent().getParent().getParent().layout(true, true);
    }

    private void showPasswordField(boolean show, String pwdLabelText) {
        ((GridData)pwdLabel.getLayoutData()).exclude = !show;
        pwdLabel.setVisible(show);

        Composite passComp = passwordText.getParent();
        ((GridData)passComp.getLayoutData()).exclude = !show;
        passComp.setVisible(show);
        if (pwdLabelText != null) {
            pwdLabel.setText(pwdLabelText);
        }
    }

    private void showPrivateKeyField(boolean show) {
        ((GridData)privateKeyLabel.getLayoutData()).exclude = !show;
        privateKeyLabel.setVisible(show);

        ((GridData)privateKeyText.getLayoutData()).exclude = !show;
        privateKeyText.setVisible(show);
    }

    @Override
    public boolean isComplete() {
        return false;
    }
}
