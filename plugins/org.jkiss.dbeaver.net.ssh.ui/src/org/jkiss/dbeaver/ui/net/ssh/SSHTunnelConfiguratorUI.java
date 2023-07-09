/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;
import org.jkiss.dbeaver.model.net.ssh.SSHImplementationAbstract;
import org.jkiss.dbeaver.model.net.ssh.SSHTunnelImpl;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationDescriptor;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHImplementationRegistry;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ConfigurationFileSelector;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Locale;

/**
 * SSH tunnel configuration
 */
public class SSHTunnelConfiguratorUI implements IObjectPropertyConfigurator<Object, DBWHandlerConfiguration> {
    private static final Log log = Log.getLog(SSHTunnelConfiguratorUI.class);

    private DBWHandlerConfiguration savedConfiguration;

    private CredentialsPanel credentialsPanel;
    private CredentialsPanel jumpServerCredentialsPanel;
    private Button jumpServerEnabledCheck;

    private Combo tunnelImplCombo;
    private Button fingerprintVerificationCheck;
    private Text localHostText;
    private Text localPortSpinner;
    private Text remoteHostText;
    private Text remotePortSpinner;

    private Text keepAliveText;
    private Text tunnelTimeout;
    private VariablesHintLabel variablesHintLabel;

    @Override
    public void createControl(@NotNull Composite parent, Object object, @NotNull Runnable propertyChangeListener)
    {
        ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolledComposite.setLayout(new GridLayout(1, false));
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Composite composite = new Composite(scrolledComposite, SWT.NONE);
        final GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.widthHint = UIUtils.getFontHeight(composite) * 80;
        composite.setLayoutData(gridData);
        composite.setLayout(new GridLayout(1, false));

        scrolledComposite.setContent(composite);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        {
            Group settingsGroup = UIUtils.createControlGroup(composite, SSHUIMessages.model_ssh_configurator_group_settings, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, SWT.DEFAULT);
            credentialsPanel = new CredentialsPanel(settingsGroup, true);
        }

        {
            final ExpandableComposite group = new ExpandableComposite(composite, SWT.CHECK);
            group.addExpansionListener(new ExpansionAdapter() {
                @Override
                public void expansionStateChanged(ExpansionEvent e) {
                    scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                    UIUtils.resizeShell(parent.getShell());
                }
            });
            group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            group.setText(SSHUIMessages.model_ssh_configurator_group_jump_server_settings_text);

            final Composite client = new Composite(group, SWT.BORDER);
            client.setLayout(new GridLayout(2, false));
            client.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            group.setClient(client);

            jumpServerEnabledCheck = UIUtils.createCheckbox(client, SSHUIMessages.model_ssh_configurator_group_jump_server_checkbox_label, false);
            jumpServerEnabledCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));

            jumpServerCredentialsPanel = new CredentialsPanel(client, false);
            jumpServerEnabledCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    UIUtils.enableWithChildren(jumpServerCredentialsPanel, jumpServerEnabledCheck.getSelection());
                }
            });
            if (jumpServerCredentialsPanel != null && credentialsPanel.savePasswordCheckbox != null) {
                credentialsPanel.savePasswordCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        jumpServerCredentialsPanel.passwordText.setEnabled(credentialsPanel.savePasswordCheckbox.getSelection()
                                                                           && jumpServerEnabledCheck.getSelection());
                    }
                });
            }
        }

        {
            final ExpandableComposite group = new ExpandableComposite(composite, SWT.NONE);
            group.addExpansionListener(new ExpansionAdapter() {
                @Override
                public void expansionStateChanged(ExpansionEvent e) {
                    scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                    UIUtils.resizeShell(parent.getShell());
                }
            });
            group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            group.setText(SSHUIMessages.model_ssh_configurator_group_advanced);

            final Composite client = new Composite(group, SWT.BORDER);
            client.setLayout(new GridLayout(4, false));
            client.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            group.setClient(client);

            tunnelImplCombo = UIUtils.createLabelCombo(client, SSHUIMessages.model_ssh_configurator_label_implementation, SWT.DROP_DOWN | SWT.READ_ONLY);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
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

            fingerprintVerificationCheck = UIUtils.createCheckbox(client, SSHUIMessages.model_ssh_configurator_label_bypass_verification, false);
            GridData cgd = new GridData(GridData.FILL_HORIZONTAL);
            cgd.horizontalSpan = 2;
            fingerprintVerificationCheck.setLayoutData(cgd);
            fingerprintVerificationCheck.setToolTipText(SSHUIMessages.model_ssh_configurator_label_bypass_verification_description);

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
            Composite controlGroup = UIUtils.createComposite(parent, 3);
            controlGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createDialogButton(controlGroup, SSHUIMessages.model_ssh_configurator_button_test_tunnel, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    testTunnelConnection();
                }
            });
            String hint = SSHUIMessages.model_ssh_configurator_variables_hint_label;
            variablesHintLabel = new VariablesHintLabel(controlGroup, hint, hint, DBPConnectionConfiguration.INTERNAL_CONNECT_VARIABLES,
                false);

            UIUtils.createLink(controlGroup, SSHUIMessages.model_ssh_configurator_ssh_documentation_link, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ShellUtils.launchProgram(HelpUtils.getHelpExternalReference("SSH-Configuration"));
                }
            });
        }

        UIUtils.executeOnResize(parent, () -> parent.getParent().layout(true, true));
    }

    private static void setNumberEditStyles(Text text) {
        text.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.ENGLISH));
        GridData gdt = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gdt.widthHint = UIUtils.getFontHeight(text) * 7;
        text.setLayoutData(gdt);
    }

    @Nullable
    private static DBPAuthInfo promptCredentialsDialog(
        @NotNull SSHConstants.AuthType type,
        @NotNull String username, @NotNull String password
    ) {
        try {
            return DBWorkbench.getPlatformUI().promptUserCredentials(
                SSHUIMessages.model_ssh_dialog_credentials,
                SSHUIMessages.model_ssh_dialog_credentials_username,
                username,
                type.equals(SSHConstants.AuthType.PUBLIC_KEY)
                    ? SSHUIMessages.model_ssh_dialog_credentials_passphrase
                    : SSHUIMessages.model_ssh_dialog_credentials_password,
                password,
                type.equals(SSHConstants.AuthType.PUBLIC_KEY),
                false
            );
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError(
                CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                e.getMessage(),
                e);
            return null;
        }
    }

    private void testTunnelConnection() {
        DBWHandlerConfiguration configuration = new DBWHandlerConfiguration(savedConfiguration);
        configuration.setProperties(Collections.emptyMap());
        saveSettings(configuration);
        DBPDataSourceContainer dataSource = configuration.getDataSource();
        if (dataSource != null) {
            configuration.resolveDynamicVariables(new DataSourceVariableResolver(dataSource,
                dataSource.getConnectionConfiguration()));
        } else {
            configuration.resolveDynamicVariables(SystemVariablesResolver.INSTANCE);
        }

        final String[] tunnelVersions = new String[2];

        final TunnelConnectionTestJob job = new TunnelConnectionTestJob() {
            @Override
            protected void execute(@NotNull DBRProgressMonitor monitor) throws Throwable {
                monitor.beginTask("Instantiate SSH tunnel", 2);
                SSHTunnelImpl tunnel = new SSHTunnelImpl();
                DBPConnectionConfiguration connectionConfig = new DBPConnectionConfiguration();
                connectionConfig.setHostName("localhost");
                connectionConfig.setHostPort(configuration.getStringProperty(DBWHandlerConfiguration.PROP_PORT));
                try {
                    monitor.subTask("Initialize tunnel");
                    String authTypeName = configuration.getStringProperty("authType");
                    SSHConstants.AuthType authType = CommonUtils.valueOf(SSHConstants.AuthType.class, authTypeName);
                    if (!configuration.isSavePassword() && tunnel.getRequiredCredentials(configuration, null) != DBWTunnel.AuthCredentials.NONE) {
                        DBPAuthInfo dbpAuthInfo = promptCredentialsDialog(authType, configuration.getUserName(),
                            configuration.getPassword());
                        if (dbpAuthInfo != null) {
                            if (authType.equals(SSHConstants.AuthType.PASSWORD)) {
                                configuration.setUserName(dbpAuthInfo.getUserName());
                            }
                            configuration.setPassword(dbpAuthInfo.getUserPassword());
                        }
                        checkJumpServerConfiguration(tunnel);

                    }
                    tunnel.initializeHandler(monitor, configuration, connectionConfig);
                    try {
                        monitor.worked(1);
                        // Get info
                        tunnelVersions[0] = tunnel.getImplementation().getClientVersion();
                        tunnelVersions[1] = tunnel.getImplementation().getServerVersion();

                    } finally {
                        // Close it
                        monitor.subTask("Close tunnel");
                        tunnel.closeTunnel(monitor);
                    }
                    monitor.worked(1);
                } finally {
                    monitor.done();
                }
            }

            private void checkJumpServerConfiguration(SSHTunnelImpl tunnel) {
                SSHConstants.AuthType authType;
                String authTypeName;
                // If we are not saving password and have jump server enabled we need to show dialog
                // for user to write it
                if (configuration.getBooleanProperty(getJumpServerSettingsPrefix() + DBConstants.PROP_ID_ENABLED)) {
                    authTypeName = configuration.getStringProperty(getJumpServerSettingsPrefix() + SSHConstants.PROP_AUTH_TYPE);
                    authType = CommonUtils.valueOf(SSHConstants.AuthType.class,
                        authTypeName,
                        SSHConstants.AuthType.PASSWORD
                    );
                    if (tunnel.getRequiredCredentials(configuration, getJumpServerSettingsPrefix())
                        != DBWTunnel.AuthCredentials.NONE) {
                        DBPAuthInfo dbpAuthInfo = promptCredentialsDialog(authType,
                            configuration.getStringProperty(getJumpServerSettingsPrefix()
                                                            + DBConstants.PROP_ID_NAME),
                            configuration.getSecureProperty(getJumpServerSettingsPrefix()
                                                            + DBConstants.PROP_FEATURE_PASSWORD)
                        );
                        if (dbpAuthInfo != null) {
                            if (authType.equals(SSHConstants.AuthType.PASSWORD)) {
                                configuration.setProperty(getJumpServerSettingsPrefix() + DBConstants.PROP_ID_NAME,
                                    dbpAuthInfo.getUserName()
                                );
                            }
                            configuration.setSecureProperty(getJumpServerSettingsPrefix() + DBConstants.PROP_FEATURE_PASSWORD,
                                dbpAuthInfo.getUserPassword()
                            );
                        }
                    }
                }
            }
        };

        try {
            UIUtils.runInProgressDialog(monitor -> {
                job.setOwnerMonitor(monitor);
                job.schedule();

                while (job.getState() == Job.WAITING || job.getState() == Job.RUNNING) {
                    if (monitor.isCanceled()) {
                        job.cancel();
                        throw new InvocationTargetException(null);
                    }
                    RuntimeUtils.pause(50);
                }

                if (job.getConnectError() != null) {
                    throw new InvocationTargetException(job.getConnectError());
                }
            });

            MessageDialog.openInformation(credentialsPanel.getShell(), ModelMessages.dialog_connection_wizard_start_connection_monitor_success,
                "Connected!\n\nClient version: " + tunnelVersions[0] + "\nServer version: " + tunnelVersions[1]);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() != null) {
                DBWorkbench.getPlatformUI().showError(
                    CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                    null,
                    GeneralUtils.makeExceptionStatus(ex.getTargetException()));
            }
        }
    }

    @NotNull
    private String getJumpServerSettingsPrefix() {
        return SSHImplementationAbstract.getJumpServerSettingsPrefix(0);
    }

    @Override
    public void loadSettings(@NotNull DBWHandlerConfiguration configuration)
    {
        credentialsPanel.loadSettings(configuration, "");

        final String jumpServerSettingsPrefix = getJumpServerSettingsPrefix();
        jumpServerCredentialsPanel.loadSettings(configuration, jumpServerSettingsPrefix);
        jumpServerEnabledCheck.setSelection(configuration.getBooleanProperty(jumpServerSettingsPrefix + DBConstants.PROP_ID_ENABLED));
        if (credentialsPanel.savePasswordCheckbox != null) {
            jumpServerCredentialsPanel.passwordText.setEnabled(credentialsPanel.savePasswordCheckbox.getSelection());
            if (!credentialsPanel.savePasswordCheckbox.getSelection()) {
                jumpServerCredentialsPanel.passwordText.setText("");
            }
        }
        UIUtils.enableWithChildren(jumpServerCredentialsPanel, jumpServerEnabledCheck.getSelection());

        String implType = configuration.getStringProperty(SSHConstants.PROP_IMPLEMENTATION);
        if (CommonUtils.isEmpty(implType)) {
            // Try SSHJ by default
            tunnelImplCombo.setText("SSHJ");
            if (tunnelImplCombo.getSelectionIndex() == -1) {
                tunnelImplCombo.select(0);
            }
        } else {
            SSHImplementationDescriptor desc = SSHImplementationRegistry.getInstance().getDescriptor(implType);
            if (desc != null) {
                tunnelImplCombo.setText(desc.getLabel());
            } else {
                tunnelImplCombo.select(0);
            }
        }
        
        fingerprintVerificationCheck.setSelection(configuration.getBooleanProperty(SSHConstants.PROP_BYPASS_HOST_VERIFICATION));
        
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
    public void saveSettings(@NotNull DBWHandlerConfiguration configuration)
    {
        credentialsPanel.saveSettings(configuration, "");

        boolean jumpServersEnabled = jumpServerEnabledCheck.getSelection();
        if (jumpServersEnabled) {
            final String jumpServerSettingsPrefix = getJumpServerSettingsPrefix();
            jumpServerCredentialsPanel.saveSettings(configuration, jumpServerSettingsPrefix);
            configuration.setProperty(jumpServerSettingsPrefix + DBConstants.PROP_ID_ENABLED, jumpServersEnabled);
        }

        String implLabel = tunnelImplCombo.getText();
        for (SSHImplementationDescriptor it : SSHImplementationRegistry.getInstance().getDescriptors()) {
            if (it.getLabel().equals(implLabel)) {
                configuration.setProperty(SSHConstants.PROP_IMPLEMENTATION, it.getId());
                break;
            }
        }

        if (fingerprintVerificationCheck.getSelection()) {
            configuration.setProperty(SSHConstants.PROP_BYPASS_HOST_VERIFICATION, true);
        } else {
            configuration.setProperty(SSHConstants.PROP_BYPASS_HOST_VERIFICATION, null);
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
    public void resetSettings(@NotNull DBWHandlerConfiguration configuration) {

    }

    @Override
    public boolean isComplete() {
        return false;
    }

    private void updateJumpServerSettingsVisibility() {
/*
        final String name = tunnelImplCombo.getText();
        for (SSHImplementationDescriptor descriptor : SSHImplementationRegistry.getInstance().getDescriptors()) {
            if (descriptor.getLabel().equals(name)) {
                final Composite parent = jumpServerCredentialsPanel.getParent().getParent();
                UIUtils.setControlVisible(parent, descriptor.isSupportsJumpServer());
                parent.getParent().layout(true, true);
                break;
            }
        }
*/
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

            privateKeyText = new ConfigurationFileSelector(this, SSHUIMessages.model_ssh_configurator_dialog_choose_private_key, new String[]{"*", "*.ssh", "*.pem", "*.*"});
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
                    savePasswordCheckbox.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            passwordText.setEnabled(savePasswordCheckbox.getSelection());

                        }
                    });
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
            if (DBWorkbench.isDistributed()) {
                privateKeyText.setText(CommonUtils.notEmpty(configuration.getSecureProperty(prefix + SSHConstants.PROP_KEY_VALUE)));
            } else {
                privateKeyText.setText(CommonUtils.notEmpty(configuration.getStringProperty(prefix + SSHConstants.PROP_KEY_PATH)));
            }

            if (prefix.isEmpty()) {
                userNameText.setText(CommonUtils.notEmpty(configuration.getUserName()));
                if (savePasswordCheckbox != null) {
                    savePasswordCheckbox.setSelection(configuration.isSavePassword());
                    passwordText.setEnabled(savePasswordCheckbox.getSelection());
                }
                if (savePasswordCheckbox == null || savePasswordCheckbox.getSelection()) {
                    passwordText.setText(CommonUtils.notEmpty(configuration.getPassword()));
                }
            } else {
                userNameText.setText(CommonUtils.notEmpty(configuration.getStringProperty(prefix + DBConstants.PROP_ID_NAME)));
                passwordText.setText(CommonUtils.notEmpty(configuration.getSecureProperty(prefix + DBConstants.PROP_FEATURE_PASSWORD)));
                if (savePasswordCheckbox != null) {
                    savePasswordCheckbox.setSelection(configuration.getBooleanProperty(prefix + DBConstants.PROP_FEATURE_PASSWORD));
                }
                if (savePasswordCheckbox == null || savePasswordCheckbox.getSelection()) {
                    passwordText.setText(CommonUtils.notEmpty(configuration.getSecureProperty(prefix + DBConstants.PROP_FEATURE_PASSWORD)));
                }
            }
            updateAuthMethodVisibility();
        }

        public void saveSettings(@NotNull DBWHandlerConfiguration configuration, @NotNull String prefix) {
            configuration.setProperty(prefix + DBWHandlerConfiguration.PROP_HOST, hostNameText.getText().trim());
            configuration.setProperty(prefix + DBWHandlerConfiguration.PROP_PORT, CommonUtils.toInt(hostPortText.getText().trim()));
            configuration.setProperty(prefix + SSHConstants.PROP_AUTH_TYPE, SSHConstants.AuthType.values()[authMethodCombo.getSelectionIndex()].name());
            String privateKey = privateKeyText.getText().trim();
            if (CommonUtils.isEmpty(privateKey)) {
                privateKey = null;
            }
            if (DBWorkbench.isDistributed()) {
                configuration.setSecureProperty(prefix + SSHConstants.PROP_KEY_VALUE, privateKey);
            } else {
                configuration.setProperty(prefix + SSHConstants.PROP_KEY_PATH, privateKey);
            }

            if (prefix.isEmpty()) {
                configuration.setUserName(userNameText.getText().trim());

                if (savePasswordCheckbox != null) {
                    configuration.setSavePassword(savePasswordCheckbox.getSelection());
                }
                if (savePasswordCheckbox == null || configuration.isSavePassword()) {
                    configuration.setPassword(passwordText.getText());
                }
            } else {
                configuration.setProperty(prefix + RegistryConstants.ATTR_NAME, userNameText.getText().trim());
                if (passwordText.isEnabled()) {
                    configuration.setSecureProperty(prefix + RegistryConstants.ATTR_PASSWORD, passwordText.getText());
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
            authMethodCombo.getShell().layout(true, true);
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

    private static abstract class TunnelConnectionTestJob extends AbstractJob {
        private DBRProgressMonitor ownerMonitor;
        protected Throwable connectError;

        protected TunnelConnectionTestJob() {
            super("Test tunnel connection");
            setUser(false);
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            if (ownerMonitor != null) {
                monitor = ownerMonitor;
            }

            try {
                execute(monitor);
            } catch (Throwable e) {
                connectError = e;
            }

            return Status.OK_STATUS;
        }

        public void setOwnerMonitor(@Nullable DBRProgressMonitor ownerMonitor) {
            this.ownerMonitor = ownerMonitor;
        }

        @Nullable
        public Throwable getConnectError() {
            return connectError;
        }

        protected abstract void execute(@NotNull DBRProgressMonitor monitor) throws Throwable;
    }
}
