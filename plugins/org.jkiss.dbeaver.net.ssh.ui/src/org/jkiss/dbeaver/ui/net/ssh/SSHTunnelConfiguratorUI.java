/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;
import org.jkiss.dbeaver.model.net.ssh.SSHImplementationAbstract;
import org.jkiss.dbeaver.model.net.ssh.SSHTunnelImpl;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationDescriptor;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationRegistry;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Locale;

/**
 * SSH tunnel configuration
 */
public class SSHTunnelConfiguratorUI implements IObjectPropertyConfigurator<DBWHandlerConfiguration> {

    private DBWHandlerConfiguration savedConfiguration;

    private CredentialsPanel credentialsPanel;
    private CredentialsPanel jumpServerCredentialsPanel;
    private Button jumpServerEnabledCheck;

    private Combo tunnelImplCombo;
    private Text localHostText;
    private Text localPortSpinner;
    private Text remoteHostText;
    private Text remotePortSpinner;

    private Text keepAliveText;
    private Text tunnelTimeout;
    private VariablesHintLabel variablesHintLabel;

    @Override
    public void createControl(Composite parent, Runnable propertyChangeListener)
    {
        final Composite composite = new Composite(parent, SWT.NONE);
        //gd.minimumHeight = 200;
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout(1, false));

        {
            Group settingsGroup = UIUtils.createControlGroup(composite, SSHUIMessages.model_ssh_configurator_group_settings, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, SWT.DEFAULT);
            credentialsPanel = new CredentialsPanel(settingsGroup, true);
        }

        {
            final ExpandableComposite group = new ExpandableComposite(composite, SWT.CHECK);
            group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            group.setText("Jump server settings");

            final Composite client = new Composite(group, SWT.BORDER);
            client.setLayout(new GridLayout(2, false));
            client.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            group.setClient(client);

            jumpServerEnabledCheck = UIUtils.createCheckbox(client, "Use jump server", false);
            jumpServerEnabledCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
            jumpServerEnabledCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    UIUtils.enableWithChildren(jumpServerCredentialsPanel, jumpServerEnabledCheck.getSelection());
                }
            });

            jumpServerCredentialsPanel = new CredentialsPanel(client, false);
        }

        {
            final ExpandableComposite group = new ExpandableComposite(composite, SWT.NONE);
            group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            group.setText(SSHUIMessages.model_ssh_configurator_group_advanced);

            final Composite client = new Composite(group, SWT.BORDER);
            client.setLayout(new GridLayout(4, false));
            client.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            group.setClient(client);

            tunnelImplCombo = UIUtils.createLabelCombo(client, SSHUIMessages.model_ssh_configurator_label_implementation, SWT.DROP_DOWN | SWT.READ_ONLY);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 3;
            tunnelImplCombo.setLayoutData(gd);
            tunnelImplCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateJumpServerSettingsVisibility();
                }
            });
            for (SSHImplementationDescriptor it : SSHImplementationRegistry.getInstance().getDescriptors()) {
                tunnelImplCombo.add(it.getLabel());
            }

            localHostText = UIUtils.createLabelText(client, SSHUIMessages.model_ssh_configurator_label_local_host, null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            localHostText.setToolTipText(SSHUIMessages.model_ssh_configurator_label_local_host_description);
            localHostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            remoteHostText = UIUtils.createLabelText(client, SSHUIMessages.model_ssh_configurator_label_remote_host, null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            remoteHostText.setToolTipText(SSHUIMessages.model_ssh_configurator_label_remote_host_description);
            remoteHostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            localPortSpinner = UIUtils.createLabelText(client, SSHUIMessages.model_ssh_configurator_label_local_port, String.valueOf(0));
            localPortSpinner.setToolTipText(SSHUIMessages.model_ssh_configurator_label_local_port_description);
            setNumberEditStyles(localPortSpinner);

            remotePortSpinner = UIUtils.createLabelText(client, SSHUIMessages.model_ssh_configurator_label_remote_port, String.valueOf(0));
            remotePortSpinner.setToolTipText(SSHUIMessages.model_ssh_configurator_label_remote_port_description);
            setNumberEditStyles(remotePortSpinner);

            UIUtils.createHorizontalLine(client, 4, 0);

            keepAliveText = UIUtils.createLabelText(client, SSHUIMessages.model_ssh_configurator_label_keep_alive, String.valueOf(0));
            setNumberEditStyles(keepAliveText);

            tunnelTimeout = UIUtils.createLabelText(client, SSHUIMessages.model_ssh_configurator_label_tunnel_timeout, String.valueOf(SSHConstants.DEFAULT_CONNECT_TIMEOUT));
            setNumberEditStyles(tunnelTimeout);
        }

        {
            Composite sc = new Composite(composite, SWT.NONE);
            sc.setLayout(new GridLayout());
            sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        }

        {
            Composite controlGroup = UIUtils.createComposite(composite, 3);
            controlGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createDialogButton(controlGroup, SSHUIMessages.model_ssh_configurator_button_test_tunnel, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    testTunnelConnection();
                }
            });
            String hint = "You can use variables in SSH parameters.";
            variablesHintLabel = new VariablesHintLabel(controlGroup, hint, hint, DataSourceDescriptor.CONNECT_VARIABLES, false);

            UIUtils.createLink(controlGroup, "<a>SSH Documentation</a>", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ShellUtils.launchProgram(HelpUtils.getHelpExternalReference("SSH-Configuration"));
                }
            });
        }
    }

    private static void setNumberEditStyles(Text text) {
        text.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.ENGLISH));
        GridData gdt = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gdt.widthHint = UIUtils.getFontHeight(text) * 7;
        text.setLayoutData(gdt);
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
                    dataSource.getConnectionConfiguration()));
        } else {
            configuration.resolveDynamicVariables(SystemVariablesResolver.INSTANCE);
        }

        try {
            final String[] tunnelVersions = new String[2];
            UIUtils.runInProgressDialog(monitor -> {
                monitor.beginTask("Instantiate SSH tunnel", 2);
                SSHTunnelImpl tunnel = new SSHTunnelImpl();
                DBPConnectionConfiguration connectionConfig = new DBPConnectionConfiguration();
                connectionConfig.setHostName("localhost");
                connectionConfig.setHostPort(configuration.getStringProperty(DBWHandlerConfiguration.PROP_PORT));
                try {
                    monitor.subTask("Initialize tunnel");
                    tunnel.initializeHandler(monitor, DBWorkbench.getPlatform(), configuration, connectionConfig);
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

            MessageDialog.openInformation(credentialsPanel.getShell(), ModelMessages.dialog_connection_wizard_start_connection_monitor_success,
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
        credentialsPanel.loadSettings(configuration, "");

        final String jumpServerSettingsPrefix = SSHImplementationAbstract.getJumpServerSettingsPrefix(0);
        jumpServerCredentialsPanel.loadSettings(configuration, jumpServerSettingsPrefix);
        jumpServerEnabledCheck.setSelection(configuration.getBooleanProperty(jumpServerSettingsPrefix + RegistryConstants.ATTR_ENABLED));
        UIUtils.enableWithChildren(jumpServerCredentialsPanel, jumpServerEnabledCheck.getSelection());

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
            localPortSpinner.setText(String.valueOf(lpValue));
        }

        remoteHostText.setText(CommonUtils.notEmpty(configuration.getStringProperty(SSHConstants.PROP_REMOTE_HOST)));
        int rpValue = configuration.getIntProperty(SSHConstants.PROP_REMOTE_PORT);
        if (rpValue != 0) {
            remotePortSpinner.setText(String.valueOf(rpValue));
        }

        int kaValue = configuration.getIntProperty(SSHConstants.PROP_ALIVE_INTERVAL);
        if (kaValue != 0) {
            keepAliveText.setText(String.valueOf(kaValue));
        }

        int timeoutValue = configuration.getIntProperty(SSHConstants.PROP_CONNECT_TIMEOUT);
        if (timeoutValue != 0) {
            tunnelTimeout.setText(String.valueOf(timeoutValue));
        }

        savedConfiguration = new DBWHandlerConfiguration(configuration);

        DBPDataSourceContainer dataSource = savedConfiguration.getDataSource();
        if (dataSource != null) {
            variablesHintLabel.setResolver(
                new DataSourceVariableResolver(
                    dataSource,
                    dataSource.getConnectionConfiguration()));
        }

        updateJumpServerSettingsVisibility();
    }

    @Override
    public void saveSettings(DBWHandlerConfiguration configuration)
    {
        credentialsPanel.saveSettings(configuration, "");

        final String jumpServerSettingsPrefix = SSHImplementationAbstract.getJumpServerSettingsPrefix(0);
        jumpServerCredentialsPanel.saveSettings(configuration, jumpServerSettingsPrefix);
        configuration.setProperty(jumpServerSettingsPrefix + RegistryConstants.ATTR_ENABLED, jumpServerEnabledCheck.getSelection());

        String implLabel = tunnelImplCombo.getText();
        for (SSHImplementationDescriptor it : SSHImplementationRegistry.getInstance().getDescriptors()) {
            if (it.getLabel().equals(implLabel)) {
                configuration.setProperty(SSHConstants.PROP_IMPLEMENTATION, it.getId());
                break;
            }
        }

        configuration.setProperty(SSHConstants.PROP_LOCAL_HOST, localHostText.getText().trim());
        int localPort = CommonUtils.toInt(localPortSpinner.getText());
        if (localPort <= 0) {
            configuration.setProperty(SSHConstants.PROP_LOCAL_PORT, null);
        } else {
            configuration.setProperty(SSHConstants.PROP_LOCAL_PORT, localPort);
        }

        configuration.setProperty(SSHConstants.PROP_REMOTE_HOST, remoteHostText.getText().trim());
        int remotePort = CommonUtils.toInt(remotePortSpinner.getText());
        if (remotePort <= 0) {
            configuration.setProperty(SSHConstants.PROP_REMOTE_PORT, null);
        } else {
            configuration.setProperty(SSHConstants.PROP_REMOTE_PORT, remotePort);
        }

        int kaInterval = CommonUtils.toInt(keepAliveText.getText());
        if (kaInterval <= 0) {
            configuration.setProperty(SSHConstants.PROP_ALIVE_INTERVAL, null);
        } else {
            configuration.setProperty(SSHConstants.PROP_ALIVE_INTERVAL, kaInterval);
        }
        int conTimeout = CommonUtils.toInt(tunnelTimeout.getText());
        if (conTimeout != 0 && conTimeout != SSHConstants.DEFAULT_CONNECT_TIMEOUT) {
            configuration.setProperty(SSHConstants.PROP_CONNECT_TIMEOUT, conTimeout);
        }
    }

    @Override
    public void resetSettings(DBWHandlerConfiguration configuration) {

    }

    @Override
    public boolean isComplete() {
        return false;
    }

    private void updateJumpServerSettingsVisibility() {
        final String name = tunnelImplCombo.getText();
        for (SSHImplementationDescriptor descriptor : SSHImplementationRegistry.getInstance().getDescriptors()) {
            if (descriptor.getLabel().equals(name)) {
                final Composite parent = jumpServerCredentialsPanel.getParent().getParent();
                UIUtils.setControlVisible(parent, descriptor.isSupportsJumpServer());
                parent.getParent().layout(true, true);
                break;
            }
        }
    }

    private static class CredentialsPanel extends Composite {
        private final Text hostNameText;
        private final Text hostPortText;
        private final Text userNameText;
        private final Combo authMethodCombo;
        private final Label privateKeyLabel;
        private final TextWithOpen privateKeyText;
        private final Label passwordLabel;
        private final Text passwordText;
        private final Button savePasswordCheckbox;

        public CredentialsPanel(@NotNull Composite parent, boolean showSavePasswordCheckbox) {
            super(parent, SWT.NONE);

            setLayout(new GridLayout(2, false));
            setLayoutData(new GridData(GridData.FILL_BOTH));

            {
                UIUtils.createControlLabel(this, SSHUIMessages.model_ssh_configurator_label_host_ip);

                Composite hostPortComp = UIUtils.createComposite(this, 3);
                hostPortComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                hostNameText = new Text(hostPortComp, SWT.BORDER);
                hostNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                hostPortText = UIUtils.createLabelText(hostPortComp, SSHUIMessages.model_ssh_configurator_label_port, String.valueOf(SSHConstants.DEFAULT_SSH_PORT));
                setNumberEditStyles(hostPortText);
            }

            userNameText = UIUtils.createLabelText(this, SSHUIMessages.model_ssh_configurator_label_user_name, null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));

            authMethodCombo = UIUtils.createLabelCombo(this, SSHUIMessages.model_ssh_configurator_combo_auth_method, SWT.DROP_DOWN | SWT.READ_ONLY);
            authMethodCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            authMethodCombo.add(SSHUIMessages.model_ssh_configurator_combo_password);
            authMethodCombo.add(SSHUIMessages.model_ssh_configurator_combo_pub_key);
            authMethodCombo.add(SSHUIMessages.model_ssh_configurator_combo_agent);
            authMethodCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateAuthMethodVisibility();
                }
            });

            privateKeyLabel = UIUtils.createControlLabel(this, SSHUIMessages.model_ssh_configurator_label_private_key);
            privateKeyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            privateKeyText = new TextWithOpenFile(this, SSHUIMessages.model_ssh_configurator_dialog_choose_private_key, new String[]{"*", "*.ssh", "*.pem", "*.*"});
            privateKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            passwordLabel = UIUtils.createControlLabel(this, SSHUIMessages.model_ssh_configurator_label_password);
            privateKeyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            {
                Composite passComp = UIUtils.createComposite(this, 2);
                passComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                passwordText = new Text(passComp, SWT.BORDER | SWT.PASSWORD);
                passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                if (showSavePasswordCheckbox) {
                    savePasswordCheckbox = UIUtils.createCheckbox(passComp, SSHUIMessages.model_ssh_configurator_checkbox_save_pass, false);
                    savePasswordCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
                } else {
                    savePasswordCheckbox = null;
                    ((GridData) passwordText.getLayoutData()).horizontalSpan = 2;
                }
            }
        }

        public void loadSettings(@NotNull DBWHandlerConfiguration configuration, @NotNull String prefix) {
            hostNameText.setText(CommonUtils.notEmpty(configuration.getStringProperty(prefix + DBWHandlerConfiguration.PROP_HOST)));
            final int portString = configuration.getIntProperty(prefix + DBWHandlerConfiguration.PROP_PORT);
            if (portString != 0) {
                hostPortText.setText(String.valueOf(portString));
            } else {
                hostPortText.setText(String.valueOf(SSHConstants.DEFAULT_SSH_PORT));
            }
            authMethodCombo.select(CommonUtils.valueOf(SSHConstants.AuthType.class, configuration.getStringProperty(prefix + SSHConstants.PROP_AUTH_TYPE), SSHConstants.AuthType.PASSWORD).ordinal());
            privateKeyText.setText(CommonUtils.notEmpty(configuration.getStringProperty(SSHConstants.PROP_KEY_PATH)));
            if (prefix.isEmpty()) {
                userNameText.setText(CommonUtils.notEmpty(configuration.getUserName()));
                passwordText.setText(CommonUtils.notEmpty(configuration.getPassword()));
                if (savePasswordCheckbox != null) {
                    savePasswordCheckbox.setSelection(configuration.isSavePassword());
                }
            } else {
                userNameText.setText(CommonUtils.notEmpty(configuration.getStringProperty(prefix + RegistryConstants.ATTR_NAME)));
                passwordText.setText(CommonUtils.notEmpty(configuration.getSecureProperty(prefix + RegistryConstants.ATTR_PASSWORD)));
                if (savePasswordCheckbox != null) {
                    savePasswordCheckbox.setSelection(configuration.getBooleanProperty(prefix + RegistryConstants.ATTR_SAVE_PASSWORD));
                }
            }
            updateAuthMethodVisibility();
        }

        public void saveSettings(@NotNull DBWHandlerConfiguration configuration, @NotNull String prefix) {
            configuration.setProperty(prefix + DBWHandlerConfiguration.PROP_HOST, hostNameText.getText().trim());
            configuration.setProperty(prefix + DBWHandlerConfiguration.PROP_PORT, CommonUtils.toInt(hostPortText.getText().trim()));
            configuration.setProperty(prefix + SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.values()[authMethodCombo.getSelectionIndex()].name());
            configuration.setProperty(prefix + SSHConstants.PROP_KEY_PATH, privateKeyText.getText().trim());

            if (prefix.isEmpty()) {
                configuration.setUserName(userNameText.getText().trim());
                configuration.setPassword(passwordText.getText());
                if (savePasswordCheckbox != null) {
                    configuration.setSavePassword(savePasswordCheckbox.getSelection());
                }
            } else {
                configuration.setProperty(prefix + RegistryConstants.ATTR_NAME, userNameText.getText().trim());
                configuration.setSecureProperty(prefix + RegistryConstants.ATTR_PASSWORD, passwordText.getText());
                if (savePasswordCheckbox != null) {
                    configuration.setProperty(prefix + RegistryConstants.ATTR_SAVE_PASSWORD, savePasswordCheckbox.getSelection());
                }
            }
        }

        private void updateAuthMethodVisibility() {
            switch (authMethodCombo.getSelectionIndex()) {
                case 0:
                    showPrivateKeyField(false);
                    showPasswordField(true, SSHUIMessages.model_ssh_configurator_label_password);
                    break;
                case 1:
                    showPrivateKeyField(true);
                    showPasswordField(true, SSHUIMessages.model_ssh_configurator_label_passphrase);
                    break;
                case 2:
                    showPrivateKeyField(false);
                    showPasswordField(false, null);
                    break;
                default:
                    break;
            }

            getParent().getParent().getParent().layout(true, true);
        }

        private void showPasswordField(boolean show, String passwordLabelText) {
            UIUtils.setControlVisible(passwordLabel, show);
            UIUtils.setControlVisible(passwordText.getParent(), show);

            if (passwordLabelText != null) {
                passwordLabel.setText(passwordLabelText);
            }
        }

        private void showPrivateKeyField(boolean show) {
            UIUtils.setControlVisible(privateKeyLabel, show);
            UIUtils.setControlVisible(privateKeyText, show);
        }
    }
}
