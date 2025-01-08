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
package org.jkiss.dbeaver.ui.net.ssh;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.net.ssh.SSHConstants;
import org.jkiss.dbeaver.model.net.ssh.SSHTunnelImpl;
import org.jkiss.dbeaver.model.net.ssh.SSHUtils;
import org.jkiss.dbeaver.model.net.ssh.config.SSHAuthConfiguration;
import org.jkiss.dbeaver.model.net.ssh.config.SSHHostConfiguration;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHSessionControllerDescriptor;
import org.jkiss.dbeaver.model.net.ssh.registry.SSHSessionControllerRegistry;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.configurator.DBPConnectionEditIntention;
import org.jkiss.dbeaver.runtime.AbstractTrackingJob;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ConfigurationFileSelector;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * SSH tunnel configuration
 */
public class SSHTunnelDefaultConfiguratorUI implements IObjectPropertyConfigurator<Object, DBWHandlerConfiguration> {
    private DBWHandlerConfiguration savedConfiguration;
    private final List<ConfigurationWrapper> configurations = new ArrayList<>();

    private CredentialsPanel credentialsPanel;

    private ExpandableComposite hostsComposite;
    private TableViewer hostsViewer;

    private Combo tunnelImplCombo;
    private Button fingerprintVerificationCheck;
    private Button enableTunnelSharingCheck;
    private Text localHostText;
    private Text localPortSpinner;
    private Text remoteHostText;
    private Text remotePortSpinner;

    private Text keepAliveText;
    private Text tunnelTimeout;
    private VariablesHintLabel variablesHintLabel;

    @Override
    public void createControl(@NotNull Composite parent, Object object, @NotNull Runnable propertyChangeListener) {
        final Composite composite = new Composite(parent, SWT.NONE);
        final GridData gridData = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(gridData);
        composite.setLayout(new GridLayout(1, false));

        {
            Group settingsGroup = UIUtils.createControlGroup(composite, SSHUIMessages.model_ssh_configurator_group_settings, 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, SWT.DEFAULT);
            credentialsPanel = new CredentialsPanel(settingsGroup, propertyChangeListener, DBPConnectionEditIntention.DEFAULT);
        }

        {
            hostsComposite = new ExpandableComposite(composite, SWT.NONE);
            hostsComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            hostsComposite.setText(SSHUIMessages.model_ssh_configurator_group_jump_server_settings_text);
            hostsComposite.addExpansionListener(new ExpansionAdapter() {
                @Override
                public void expansionStateChanged(ExpansionEvent e) {
                    UIUtils.resizeShell(parent.getShell());
                }
            });

            final Composite client = new Composite(hostsComposite, SWT.BORDER);
            client.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).create());
            client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            hostsComposite.setClient(client);

            final ToolBar toolBar = new ToolBar(client, SWT.FLAT | SWT.HORIZONTAL);
            final ToolItem createItem = UIUtils.createToolItem(toolBar, "Create new jump host", UIIcon.ROW_ADD, SelectionListener.widgetSelectedAdapter(e -> {
                final ConfigurationWrapper host = (ConfigurationWrapper) hostsViewer.getStructuredSelection().getFirstElement();
                final ConfigurationWrapper created = new ConfigurationWrapper();

                configurations.add(configurations.indexOf(host), created);
                updateConfigurationSelection(created);
            }));
            final ToolItem deleteItem = UIUtils.createToolItem(toolBar, "Delete jump host", UIIcon.ROW_DELETE, SelectionListener.widgetSelectedAdapter(e -> {
                final ConfigurationWrapper host = (ConfigurationWrapper) hostsViewer.getStructuredSelection().getFirstElement();
                if (DBWorkbench.getPlatformUI().confirmAction(
                    "Confirm host deletion",
                    "Are you sure you want to delete host '" + host.configuration.toDisplayString() + "'?"
                )) {
                    credentialsPanel.lastConfiguration = null;
                    final int index = configurations.indexOf(host);

                    configurations.remove(index);
                    updateConfigurationSelection(configurations.get(CommonUtils.clamp(index, 0, configurations.size() - 1)));
                }
            }));
            final ToolItem moveUpItem = UIUtils.createToolItem(toolBar, "Move up", UIIcon.ARROW_UP, SelectionListener.widgetSelectedAdapter(e -> {
                final ConfigurationWrapper host = (ConfigurationWrapper) hostsViewer.getStructuredSelection().getFirstElement();
                final int index = configurations.indexOf(host);

                configurations.set(index, configurations.get(index - 1));
                configurations.set(index - 1, host);
                updateConfigurationSelection(host);
            }));
            final ToolItem moveDownItem = UIUtils.createToolItem(toolBar, "Move down", UIIcon.ARROW_DOWN, SelectionListener.widgetSelectedAdapter(e -> {
                final ConfigurationWrapper host = (ConfigurationWrapper) hostsViewer.getStructuredSelection().getFirstElement();
                final int index = configurations.indexOf(host);

                configurations.set(index, configurations.get(index + 1));
                configurations.set(index + 1, host);
                updateConfigurationSelection(host);
            }));

            UIUtils.createLabelSeparator(client, SWT.HORIZONTAL);

            hostsViewer = new TableViewer(client, SWT.FULL_SELECTION | SWT.SINGLE);
            hostsViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
            hostsViewer.getTable().setHeaderVisible(true);
            hostsViewer.setContentProvider(ArrayContentProvider.getInstance());
            hostsViewer.setInput(configurations);
            hostsViewer.addSelectionChangedListener(e -> {
                final ConfigurationWrapper last = credentialsPanel.lastConfiguration;
                final ConfigurationWrapper current = (ConfigurationWrapper) e.getStructuredSelection().getFirstElement();

                if (current == null) {
                    return;
                }

                if (last != null && last != current) {
                    final SSHHostConfiguration updated = credentialsPanel.saveSettings();
                    if (!last.configuration.equals(updated)) {
                        last.configuration = updated;
                        hostsViewer.refresh();
                    }
                }

                final int index = configurations.indexOf(current);
                final int count = configurations.size();

                createItem.setEnabled(count < SSHConstants.MAX_JUMP_SERVERS);
                deleteItem.setEnabled(count > 1);
                moveUpItem.setEnabled(index > 0);
                moveDownItem.setEnabled(index < count - 1);

                loadConfiguration(current);
                propertyChangeListener.run();
            });

            final ViewerColumnController<Object, ConfigurationWrapper> controller = new ViewerColumnController<>("ssh_hosts", hostsViewer);
            controller.addColumn("Order", "Order of the jump server", SWT.LEFT, true, true, wrapper -> {
                return String.valueOf(isDestinationHost(wrapper) ? "Target" : "Jump #%d".formatted(configurations.indexOf(wrapper) + 1));
            }, null);
            controller.addColumn("Host", "Host name", SWT.LEFT, true, true, wrapper -> {
                return wrapper.configuration.hostname() + ':' + wrapper.configuration.port();
            }, null);
            controller.addColumn("User", "User name", SWT.LEFT, true, true, wrapper -> {
                return wrapper.configuration.username();
            }, null);
            controller.addColumn("Authentication", "Authentication method", SWT.LEFT, true, true, wrapper -> {
                final SSHAuthConfiguration auth = wrapper.configuration.auth();
                if (auth instanceof SSHAuthConfiguration.Password) {
                    return "Password";
                } else if (auth instanceof SSHAuthConfiguration.KeyData || auth instanceof SSHAuthConfiguration.KeyFile) {
                    return "Public Key";
                } else {
                    return "Agent";
                }
            }, null);
            controller.createColumns(true);

        }

        {
            final ExpandableComposite group = new ExpandableComposite(composite, SWT.NONE);
            group.addExpansionListener(new ExpansionAdapter() {
                @Override
                public void expansionStateChanged(ExpansionEvent e) {
                    UIUtils.resizeShell(parent.getShell());
                }
            });
            group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            group.setText(SSHUIMessages.model_ssh_configurator_group_advanced);

            final Composite client = new Composite(group, SWT.NONE);
            client.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
            client.setLayoutData(new GridData(GridData.FILL_BOTH));
            group.setClient(client);

            final Group generalGroup = UIUtils.createControlGroup(
                client,
                SSHUIMessages.model_ssh_configurator_group_general_text,
                2,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING,
                0
            );

            tunnelImplCombo = UIUtils.createLabelCombo(
                generalGroup,
                SSHUIMessages.model_ssh_configurator_label_implementation,
                SWT.DROP_DOWN | SWT.READ_ONLY
            );
            tunnelImplCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            tunnelImplCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    // updateJumpServerSettingsVisibility();
                }
            });
            for (SSHSessionControllerDescriptor it : SSHSessionControllerRegistry.getInstance().getDescriptors()) {
                tunnelImplCombo.add(it.getLabel());
            }

            fingerprintVerificationCheck = UIUtils.createCheckbox(
                generalGroup,
                SSHUIMessages.model_ssh_configurator_label_bypass_verification,
                SSHUIMessages.model_ssh_configurator_label_bypass_verification_description,
                false,
                2
            );

            enableTunnelSharingCheck = UIUtils.createCheckbox(
                generalGroup,
                SSHUIMessages.model_ssh_configurator_label_share_tunnels,
                SSHUIMessages.model_ssh_configurator_label_share_tunnels_description,
                true,
                2
            );

            // Hide tunnel sharing option if it's disabled
            UIUtils.setControlVisible(enableTunnelSharingCheck, !SSHUtils.DISABLE_SESSION_SHARING);

            final Group timeoutsGroup = UIUtils.createControlGroup(
                client,
                SSHUIMessages.model_ssh_configurator_group_timeouts_text,
                2,
                GridData.VERTICAL_ALIGN_FILL,
                0
            );
            keepAliveText = UIUtils.createLabelText(
                timeoutsGroup,
                SSHUIMessages.model_ssh_configurator_label_keep_alive,
                String.valueOf(0)
            );
            setNumberEditStyles(keepAliveText);
            tunnelTimeout = UIUtils.createLabelText(
                timeoutsGroup,
                SSHUIMessages.model_ssh_configurator_label_tunnel_timeout,
                String.valueOf(SSHConstants.DEFAULT_CONNECT_TIMEOUT)
            );
            setNumberEditStyles(tunnelTimeout);

            final Group portForwardingGroup = UIUtils.createControlGroup(
                client,
                SSHUIMessages.model_ssh_configurator_group_port_forwarding_text,
                4,
                GridData.FILL_HORIZONTAL,
                0
            );
            ((GridData) portForwardingGroup.getLayoutData()).horizontalSpan = 2;
            localHostText = UIUtils.createLabelText(
                portForwardingGroup,
                SSHUIMessages.model_ssh_configurator_label_local_host,
                null,
                SWT.BORDER,
                new GridData(GridData.FILL_HORIZONTAL)
            );
            localHostText.setToolTipText(SSHUIMessages.model_ssh_configurator_label_local_host_description);
            localHostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            localPortSpinner = UIUtils.createLabelText(
                portForwardingGroup,
                SSHUIMessages.model_ssh_configurator_label_local_port,
                String.valueOf(0)
            );
            localPortSpinner.setToolTipText(SSHUIMessages.model_ssh_configurator_label_local_port_description);
            setNumberEditStyles(localPortSpinner);

            remoteHostText = UIUtils.createLabelText(
                portForwardingGroup,
                SSHUIMessages.model_ssh_configurator_label_remote_host,
                null,
                SWT.BORDER,
                new GridData(GridData.FILL_HORIZONTAL)
            );
            remoteHostText.setToolTipText(SSHUIMessages.model_ssh_configurator_label_remote_host_description);
            remoteHostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            remotePortSpinner = UIUtils.createLabelText(
                portForwardingGroup,
                SSHUIMessages.model_ssh_configurator_label_remote_port,
                String.valueOf(0)
            );
            remotePortSpinner.setToolTipText(SSHUIMessages.model_ssh_configurator_label_remote_port_description);
            setNumberEditStyles(remotePortSpinner);
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
                false
            );

            UIUtils.createLink(controlGroup, SSHUIMessages.model_ssh_configurator_ssh_documentation_link, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ShellUtils.launchProgram(HelpUtils.getHelpExternalReference("SSH-Configuration"));
                }
            });
        }

        UIUtils.executeOnResize(parent, () -> parent.getParent().layout(true, true));
        UIUtils.asyncExec(() -> UIUtils.resizeShell(parent.getShell()));
    }

    private void loadConfiguration(@NotNull ConfigurationWrapper wrapper) {
        // TODO: For now, we enforce password saving for jump hosts
        credentialsPanel.loadSettings(wrapper, !isDestinationHost(wrapper));
    }

    private void updateConfigurationSelection(@NotNull ConfigurationWrapper wrapper) {
        hostsViewer.refresh();
        hostsViewer.setSelection(new StructuredSelection(wrapper), true);
    }

    private boolean isDestinationHost(@NotNull ConfigurationWrapper wrapper) {
        return configurations.get(configurations.size() - 1).equals(wrapper);
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
        @Nullable String username,
        @Nullable String password
    ) {
        try {
            return DBWorkbench.getPlatformUI().promptUserCredentials(
                SSHUIMessages.model_ssh_dialog_credentials,
                null,
                SSHUIMessages.model_ssh_dialog_credentials_username,
                CommonUtils.notEmpty(username),
                type.equals(SSHConstants.AuthType.PUBLIC_KEY)
                    ? SSHUIMessages.model_ssh_dialog_credentials_passphrase
                    : SSHUIMessages.model_ssh_dialog_credentials_password,
                CommonUtils.notEmpty(password),
                false,
                false
            );
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError(
                "SSH error",
                e.getMessage(),
                e
            );
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
                dataSource.getConnectionConfiguration()
            ));
        } else {
            configuration.resolveDynamicVariables(SystemVariablesResolver.INSTANCE);
        }

        final String[] tunnelVersions = new String[2];

        var job = new AbstractTrackingJob("Test tunnel connection") {
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
                    SSHConstants.AuthType authType = CommonUtils.valueOf(SSHConstants.AuthType.class, authTypeName, SSHConstants.AuthType.PASSWORD);
                    if (!configuration.isSavePassword() && tunnel.getRequiredCredentials(configuration) != DBWTunnel.AuthCredentials.NONE) {
                        DBPAuthInfo dbpAuthInfo = promptCredentialsDialog(authType, configuration.getUserName(), configuration.getPassword());
                        if (dbpAuthInfo != null) {
                            configuration.setUserName(dbpAuthInfo.getUserName());
                            configuration.setPassword(dbpAuthInfo.getUserPassword());
                        }
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
        };

        try {
            AbstractTrackingJob.executeInProgressMonitor(job);

            MessageDialog.openInformation(credentialsPanel.getShell(), ModelMessages.dialog_connection_wizard_start_connection_monitor_success,
                "Connected!\n\nClient version: " + tunnelVersions[0] + "\nServer version: " + tunnelVersions[1]
            );
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() != null) {
                DBWorkbench.getPlatformUI().showError(
                    "SSH error",
                    null,
                    GeneralUtils.makeExceptionStatus(ex.getTargetException())
                );
            }
        }
    }

    @Override
    public void loadSettings(@NotNull DBWHandlerConfiguration configuration) {
        try {
            configurations.clear();
            for (SSHHostConfiguration host : SSHUtils.loadHostConfigurations(configuration, false)) {
                configurations.add(new ConfigurationWrapper(host));
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("SSH configuration", "Unable to load SSH configuration due to an error", e);
            return;
        }

        hostsViewer.refresh();
        hostsViewer.setSelection(new StructuredSelection(configurations.get(configurations.size() - 1)));
        hostsComposite.setExpanded(configurations.size() > 1);

        String implType = configuration.getStringProperty(SSHConstants.PROP_IMPLEMENTATION);
        if (CommonUtils.isEmpty(implType)) {
            // Try SSHJ by default
            tunnelImplCombo.setText("SSHJ");
            if (tunnelImplCombo.getSelectionIndex() == -1) {
                tunnelImplCombo.select(0);
            }
        } else {
            SSHSessionControllerDescriptor desc = SSHSessionControllerRegistry.getInstance().getDescriptor(implType);
            if (desc != null) {
                tunnelImplCombo.setText(desc.getLabel());
            } else {
                tunnelImplCombo.select(0);
            }
        }

        fingerprintVerificationCheck.setSelection(configuration.getBooleanProperty(SSHConstants.PROP_BYPASS_HOST_VERIFICATION));
        enableTunnelSharingCheck.setSelection(configuration.getBooleanProperty(SSHConstants.PROP_SHARE_TUNNELS, true));

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
                    dataSource.getConnectionConfiguration()
                ));
        }
    }

    @Override
    public void saveSettings(@NotNull DBWHandlerConfiguration configuration) {
        // Save current configuration just in case
        credentialsPanel.lastConfiguration.configuration = credentialsPanel.saveSettings();

        final SSHHostConfiguration[] hosts = configurations.stream()
            .map(wrapper -> wrapper.configuration)
            .toArray(SSHHostConfiguration[]::new);

        SSHUtils.saveHostConfigurations(configuration, hosts);

        String implLabel = tunnelImplCombo.getText();
        for (SSHSessionControllerDescriptor it : SSHSessionControllerRegistry.getInstance().getDescriptors()) {
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

        configuration.setProperty(SSHConstants.PROP_SHARE_TUNNELS, enableTunnelSharingCheck.getSelection());

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
        return getErrorMessage() == null;
    }

    @Nullable
    @Override
    public String getErrorMessage() {
        // Validate the current configuration first so that we get real-time feedback
        if (credentialsPanel.lastConfiguration != null) {
            final String message = validateConfiguration(credentialsPanel.saveSettings());
            if (message != null) {
                return message;
            }
        }
        // Then validate all remaining configurations
        for (ConfigurationWrapper wrapper : configurations) {
            if (credentialsPanel.lastConfiguration != null && wrapper == credentialsPanel.lastConfiguration) {
                continue;
            }
            final String message = validateConfiguration(wrapper.configuration);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    @Nullable
    static String validateConfiguration(@NotNull SSHHostConfiguration configuration) {
        if (configuration.hostname().isBlank()) {
            return "Hostname is not specified";
        }
        final SSHAuthConfiguration auth = configuration.auth();
        if (auth instanceof SSHAuthConfiguration.KeyFile keyFile && keyFile.path().isBlank() ||
            auth instanceof SSHAuthConfiguration.KeyData keyData && keyData.data().isBlank()
        ) {
            return "Private key is not specified";
        }
        return null;
    }

    static class CredentialsPanel extends Composite {
        private ConfigurationWrapper lastConfiguration;

        private final Text hostNameText;
        private final Text hostPortText;
        private final Text userNameText;
        private final Combo authMethodCombo;
        private final Label privateKeyLabel;
        private final ConfigurationFileSelector privateKeyText;
        private final Label passwordLabel;
        private final Text passwordText;
        private final Button savePasswordCheckbox;

        public CredentialsPanel(
            @NotNull Composite parent,
            @NotNull Runnable propertyChangeListener,
            @NotNull DBPConnectionEditIntention editIntention
        ) {
            super(parent, SWT.NONE);

            setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
            setLayoutData(new GridData(GridData.FILL_BOTH));

            final ModifyListener listener = e -> propertyChangeListener.run();

            {
                UIUtils.createControlLabel(this, SSHUIMessages.model_ssh_configurator_label_host_ip);

                Composite hostPortComp = UIUtils.createComposite(this, 3);
                hostPortComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                hostNameText = new Text(hostPortComp, SWT.BORDER);
                hostNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                hostNameText.addModifyListener(listener);
                hostPortText = UIUtils.createLabelText(hostPortComp, SSHUIMessages.model_ssh_configurator_label_port, String.valueOf(SSHConstants.DEFAULT_PORT));
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
                    propertyChangeListener.run();
                }
            });

            privateKeyLabel = UIUtils.createControlLabel(this, SSHUIMessages.model_ssh_configurator_label_private_key);
            privateKeyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            privateKeyText = new ConfigurationFileSelector(
                this,
                SSHUIMessages.model_ssh_configurator_dialog_choose_private_key, new String[]{"*", "*.ssh", "*.pem", "*.*"},
                false,
                DBWorkbench.isDistributed()
            );
            privateKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            privateKeyText.getTextControl().addModifyListener(listener);
            if (DBWorkbench.isDistributed()) {
                privateKeyText.getTextControl().setEditable(false);
            }

            passwordLabel = UIUtils.createControlLabel(this, SSHUIMessages.model_ssh_configurator_label_password);
            privateKeyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            {
                Composite passComp = UIUtils.createComposite(this, 2);
                passComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                passwordText = new Text(passComp, SWT.BORDER | SWT.PASSWORD);
                passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                savePasswordCheckbox = UIUtils.createCheckbox(passComp, SSHUIMessages.model_ssh_configurator_checkbox_save_pass, false);
                savePasswordCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        passwordText.setEnabled(savePasswordCheckbox.getSelection());

                    }
                });
                savePasswordCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            }

            if (editIntention == DBPConnectionEditIntention.CREDENTIALS_ONLY) {
                hostNameText.setEditable(false);
                hostPortText.setEditable(false);
                authMethodCombo.setEnabled(false);
            }
        }

        public void loadSettings(@NotNull ConfigurationWrapper wrapper, boolean forceSavePassword) {
            final SSHHostConfiguration configuration = wrapper.configuration;

            userNameText.setText(configuration.username());
            hostNameText.setText(configuration.hostname());
            hostPortText.setText(String.valueOf(configuration.port()));

            if (configuration.auth() instanceof SSHAuthConfiguration.WithPassword password) {
                final boolean savePassword = forceSavePassword || password.savePassword();
                passwordText.setText(CommonUtils.notEmpty(password.password()));
                savePasswordCheckbox.setSelection(savePassword);
                savePasswordCheckbox.setEnabled(!forceSavePassword);
            }

            if (configuration.auth() instanceof SSHAuthConfiguration.Password) {
                authMethodCombo.select(SSHConstants.AuthType.PASSWORD.ordinal());
            } else if (configuration.auth() instanceof SSHAuthConfiguration.KeyData key) {
                privateKeyText.setText(key.data());
                authMethodCombo.select(SSHConstants.AuthType.PUBLIC_KEY.ordinal());
            } else if (configuration.auth() instanceof SSHAuthConfiguration.KeyFile key) {
                privateKeyText.setText(key.path());
                authMethodCombo.select(SSHConstants.AuthType.PUBLIC_KEY.ordinal());
            } else if (configuration.auth() instanceof SSHAuthConfiguration.Agent) {
                authMethodCombo.select(SSHConstants.AuthType.AGENT.ordinal());
            }

            updateAuthMethodVisibility();
            lastConfiguration = wrapper;
        }

        @NotNull
        public SSHHostConfiguration saveSettings() {
            final String password = CommonUtils.nullIfEmpty(passwordText.getText().trim());
            final boolean savePassword = savePasswordCheckbox.getSelection();

            final SSHAuthConfiguration auth = switch (getAuthMethod()) {
                case PASSWORD -> new SSHAuthConfiguration.Password(password, savePassword);
                case PUBLIC_KEY -> {
                    final String privateKey = privateKeyText.getText().trim();
                    if (DBWorkbench.isDistributed()) {
                        yield new SSHAuthConfiguration.KeyData(privateKey, password, savePassword);
                    } else {
                        yield new SSHAuthConfiguration.KeyFile(privateKey, password, savePassword);
                    }
                }
                case AGENT -> new SSHAuthConfiguration.Agent();
            };

            final String username = userNameText.getText().trim();
            final String hostname = hostNameText.getText().trim();
            final int port = CommonUtils.toInt(hostPortText.getText().trim());

            return new SSHHostConfiguration(username, hostname, port, auth);
        }

        @NotNull
        private SSHConstants.AuthType getAuthMethod() {
            return SSHConstants.AuthType.values()[authMethodCombo.getSelectionIndex()];
        }

        private void updateAuthMethodVisibility() {
            switch (getAuthMethod()) {
                case PASSWORD -> {
                    showPrivateKeyField(false);
                    showPasswordField(true, SSHUIMessages.model_ssh_configurator_label_password);
                }
                case PUBLIC_KEY -> {
                    showPrivateKeyField(true);
                    showPasswordField(true, SSHUIMessages.model_ssh_configurator_label_passphrase);
                }
                case AGENT -> {
                    showPrivateKeyField(false);
                    showPasswordField(false, null);
                }
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

    static class ConfigurationWrapper {
        private SSHHostConfiguration configuration;

        ConfigurationWrapper(@NotNull SSHHostConfiguration configuration) {
            this.configuration = configuration;
        }

        private ConfigurationWrapper() {
            this.configuration = new SSHHostConfiguration(
                SSHConstants.DEFAULT_USER_NAME,
                "",
                SSHConstants.DEFAULT_PORT,
                new SSHAuthConfiguration.Password("", true)
            );
        }
    }
}
