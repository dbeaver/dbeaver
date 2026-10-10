/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSubscription;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.DatabaseLabelProviders;
import org.jkiss.dbeaver.ui.controls.ExpandableCompositeEx;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.dbeaver.ui.dialogs.net.SSLConfiguratorTrustStoreUI;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Publisher settings only: no JDBC, metadata, network tunnel, or datasource persistence controls.
 */
public class PostgreSubscriptionConnectionPanel extends Composite {
    private final Runnable onChange;
    private final Runnable onLayoutChange;
    private final DataSourceDescriptor publisher;
    private DBWHandlerConfiguration ssl;
    private final PublisherCredentialsConfigurator credentials = new PublisherCredentialsConfigurator();
    private final PublisherSSLConfigurator sslConfigurator = new PublisherSSLConfigurator();
    private Text hostText;
    private Text portText;
    private Text databaseText;
    private Label errorLabel;
    private String connectionInfo = "";
    private boolean loadingSettings;
    private Composite connectionSettings;

    public PostgreSubscriptionConnectionPanel(
        @NotNull Composite parent,
        @NotNull PostgreDatabase subscriberDatabase,
        @NotNull DBPConnectionConfiguration initialConfiguration,
        @NotNull Runnable onChange,
        @NotNull Runnable onLayoutChange
    ) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(1, false));
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.onChange = onChange;
        this.onLayoutChange = onLayoutChange;
        DBPDataSourceContainer subscriber = subscriberDatabase.getDataSource().getContainer();
        DBPConnectionConfiguration workingConfiguration = DBPConnectionConfiguration.copyWithIndependentRuntimeAttributes(initialConfiguration);
        publisher = subscriber.getRegistry().createDataSource(
            DataSourceDescriptor.generateNewId(subscriber.getDriver()), subscriber.getDriver(), workingConfiguration);
        publisher.setTemporary(true);
        publisher.setSavePassword(true);
        initializeSSL(workingConfiguration);
        createControls();
        validateInput();
        addDisposeListener(event -> {
            if (!publisher.isDisposed()) {
                publisher.dispose();
            }
        });
    }

    private void initializeSSL(@NotNull DBPConnectionConfiguration workingConfiguration) {
        DBWHandlerConfiguration initialSSL = workingConfiguration.getHandler(PostgreConstants.HANDLER_SSL);
        ssl = initialSSL == null || !initialSSL.isEnabled()
            ? new DBWHandlerConfiguration(Objects.requireNonNull(
                NetworkHandlerRegistry.getInstance().getDescriptor(PostgreConstants.HANDLER_SSL)), publisher)
            : new DBWHandlerConfiguration(initialSSL);
        ssl.setDataSource(publisher);
        ssl.setEnabled(true);
        if (CommonUtils.isEmpty(ssl.getStringProperty(PostgreConstants.PROP_SSL_MODE))) {
            String mode = workingConfiguration.getProperty("sslmode");
            if (CommonUtils.isEmpty(mode)) {
                boolean sslRequested = initialSSL != null && initialSSL.isEnabled()
                    || CommonUtils.toBoolean(workingConfiguration.getProperty(PostgreConstants.PROP_SSL));
                mode = sslRequested ? "verify-full" : "prefer";
            }
            ssl.setProperty(PostgreConstants.PROP_SSL_MODE, mode);
        }
        String[][] certificates = {
            {"sslrootcert", SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT},
            {"sslcert", SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT},
            {"sslkey", SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY}
        };
        for (String[] certificate : certificates) {
            if (CommonUtils.isEmpty(ssl.getStringProperty(certificate[1]))) {
                ssl.setProperty(certificate[1], workingConfiguration.getProperty(certificate[0]));
            }
            workingConfiguration.removeProperty(certificate[0]);
        }
        if (CommonUtils.isEmpty(ssl.getPassword())) {
            ssl.setPassword(workingConfiguration.getProperty("sslpassword"));
        }
        workingConfiguration.removeProperty("sslmode");
        workingConfiguration.removeProperty("sslpassword");
        workingConfiguration.removeProperty(PostgreConstants.PROP_SSL);
        workingConfiguration.updateHandler(ssl);
    }

    private void createControls() {
        Composite area = this;
        connectionSettings = UIUtils.createComposite(area, 1);
        connectionSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // A shared grid keeps the saved-connection selector and all connection fields in the same label column.
        Composite connection = UIUtils.createComposite(connectionSettings, 2);
        connection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createControlLabel(connection, PostgreMessages.dialog_create_subscription_existing_connection);
        // Native combo popups are not shell children and cannot enter the dialog's layout during resizing.
        ComboViewer connections = new ComboViewer(connection, SWT.DROP_DOWN | SWT.READ_ONLY);
        connections.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        connections.setContentProvider(ArrayContentProvider.getInstance());
        connections.setLabelProvider(new DatabaseLabelProviders.ConnectionLabelProvider() {
                @NotNull
                @Override
                public String getText(@Nullable Object element) {
                    return element instanceof DBPDataSourceContainer ? super.getText(element)
                        : PostgreMessages.dialog_create_subscription_manual_connection;
                }
            });
        List<Object> items = new ArrayList<>();
        items.add(PostgreMessages.dialog_create_subscription_manual_connection);
        connections.getCombo().setToolTipText(PostgreMessages.dialog_create_subscription_existing_connection_tip);
        for (DBPDataSourceContainer candidate : publisher.getRegistry().getDataSources()) {
            if (!candidate.isHidden() && !candidate.isTemporary() && PostgreSubscription.supportsPublisherConnection(candidate)) {
                items.add(candidate);
            }
        }
        connections.setInput(items);
        connections.setSelection(new StructuredSelection(items.getFirst()));
        connections.addSelectionChangedListener(event -> {
            Object selected = event.getStructuredSelection().getFirstElement();
            if (selected instanceof DBPDataSourceContainer container && !loadConnection(container)) {
                connections.setSelection(new StructuredSelection(items.getFirst()));
                onChange.run();
            }
        });
        DBPConnectionConfiguration workingConfiguration = publisher.getConnectionConfiguration();
        UIUtils.createControlLabel(connection, PostgreMessages.dialog_setting_connection_host);
        Composite endpoint = UIUtils.createComposite(connection, 3);
        endpoint.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout endpointLayout = (GridLayout) endpoint.getLayout();
        endpointLayout.marginWidth = 0;
        endpointLayout.marginHeight = 0;
        hostText = new Text(endpoint, SWT.BORDER);
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hostText.setText(CommonUtils.notEmpty(workingConfiguration.getHostName()));
        portText = UIUtils.createLabelText(endpoint, PostgreMessages.dialog_setting_connection_port,
            CommonUtils.isEmpty(workingConfiguration.getHostPort())
                ? Integer.toString(PostgreConstants.DEFAULT_PORT) : workingConfiguration.getHostPort());
        GridData portData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        portData.widthHint = UIUtils.getFontHeight(area) * 5;
        portText.setLayoutData(portData);
        UIUtils.createEmptyLabel(connection, 1, 1);
        Label endpointCaption = new Label(connection, SWT.WRAP);
        endpointCaption.setText(PostgreMessages.dialog_create_subscription_endpoint_tip);
        GridData captionData = new GridData(GridData.FILL_HORIZONTAL);
        captionData.widthHint = 0;
        endpointCaption.setLayoutData(captionData);
        Font captionFont = UIUtils.modifyFontSize(endpointCaption.getFont(), -1);
        endpointCaption.setFont(captionFont);
        endpointCaption.addDisposeListener(event -> captionFont.dispose());
        databaseText = UIUtils.createLabelText(connection, PostgreMessages.dialog_setting_connection_database,
            CommonUtils.notEmpty(workingConfiguration.getDatabaseName()));
        hostText.setToolTipText(PostgreMessages.dialog_create_subscription_endpoint_tip);
        portText.setToolTipText(PostgreMessages.dialog_create_subscription_endpoint_tip);
        credentials.createControl(connection, AuthModelDatabaseNative.INSTANCE, this::validateInput);
        credentials.loadSettings(publisher);
        for (Text text : new Text[]{hostText, portText, databaseText}) {
            text.addModifyListener(event -> validateInput());
        }

        ExpandableCompositeEx sslSection = UIUtils.createExpandableCompositeWithSeparator(connectionSettings, SWT.NONE,
            ExpandableCompositeEx.TWISTIE | ExpandableCompositeEx.CLIENT_INDENT);
        sslSection.setText(PostgreMessages.dialog_create_subscription_ssl_tab);
        sslSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Composite sslPage = UIUtils.createComposite(sslSection, 1);
        sslSection.setClient(sslPage);
        sslConfigurator.createControl(sslPage, null, this::validateInput);
        sslConfigurator.loadSettings(ssl);
        sslSection.addExpansionListener(IExpansionListener.expansionStateChangedAdapter(event -> onLayoutChange.run()));

        errorLabel = new Label(connectionSettings, SWT.WRAP);
        GridData errorData = new GridData(GridData.FILL_HORIZONTAL);
        errorData.widthHint = UIUtils.getFontHeight(area) * 40;
        errorLabel.setLayoutData(errorData);
    }

    private void saveInput() {
        DBPConnectionConfiguration workingConfiguration = publisher.getConnectionConfiguration();
        workingConfiguration.setHostName(hostText.getText().trim());
        workingConfiguration.setHostPort(portText.getText().trim());
        workingConfiguration.setDatabaseName(databaseText.getText().trim());
        credentials.saveSettings(publisher);
        sslConfigurator.saveSettings(ssl);
    }

    private boolean loadConnection(@NotNull DBPDataSourceContainer selected) {
        try {
            DBPConnectionConfiguration copy = UIUtils.runWithDialog(
                monitor -> PostgreSubscription.copyPublisherConnectionConfiguration(monitor, selected));
            if (copy == null) {
                return false;
            }
            loadingSettings = true;
            try {
                publisher.setConnectionInfo(copy);
                initializeSSL(copy);
                hostText.setText(CommonUtils.notEmpty(copy.getHostName()));
                portText.setText(CommonUtils.isEmpty(copy.getHostPort())
                    ? Integer.toString(PostgreConstants.DEFAULT_PORT) : copy.getHostPort());
                databaseText.setText(CommonUtils.notEmpty(copy.getDatabaseName()));
                credentials.loadSettings(publisher);
                sslConfigurator.loadSettings(ssl);
            } finally {
                loadingSettings = false;
            }
            validateInput();
            return true;
        } catch (DBException e) {
            errorLabel.setText(e.getMessage());
            UIUtils.setControlVisible(errorLabel, true);
            UIUtils.updateDialogSize(errorLabel);
            onLayoutChange.run();
            return false;
        }
    }

    private void validateInput() {
        if (errorLabel == null || loadingSettings) {
            return;
        }
        saveInput();
        try {
            connectionInfo = PostgreSubscription.buildConnectionInfo(publisher.getConnectionConfiguration());
            errorLabel.setText("");
        } catch (DBException e) {
            connectionInfo = "";
            errorLabel.setText(hostText.getText().isBlank() || databaseText.getText().isBlank() ? "" : e.getMessage());
        }
        UIUtils.setControlVisible(errorLabel, !errorLabel.getText().isEmpty());
        UIUtils.updateDialogSize(errorLabel);
        onLayoutChange.run();
        onChange.run();
    }

    @NotNull
    public String getConnectionInfo() {
        return connectionInfo;
    }

    private static class PublisherCredentialsConfigurator extends DatabaseNativeAuthModelConfigurator {
        @Override
        protected void createPasswordControls(@NotNull Composite parent, @NotNull Runnable propertyChangeListener) {
            passwordLabel = UIUtils.createLabel(parent, getPasswordFieldLabel());
            Text password = createPasswordText(parent, null);
            password.getParent().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            password.addModifyListener(event -> propertyChangeListener.run());
        }

        @NotNull
        @Override
        protected GridData makeAuthControlLayoutData(@NotNull Composite parent) {
            return new GridData(GridData.FILL_HORIZONTAL);
        }

        @Override
        public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
            super.loadSettings(dataSource);
            passwordText.setEnabled(canEditCredentialsPerPolicy);
        }

        @Override
        public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
            super.saveSettings(dataSource);
            if (canEditCredentialsPerPolicy) {
                // Whitespace is significant in libpq passwords; the standard datasource configurator trims it.
                dataSource.getConnectionConfiguration().setUserPassword(passwordText.getText());
            }
        }
    }

    private static class PublisherSSLConfigurator extends SSLConfiguratorTrustStoreUI {
        private Combo modeCombo;

        @Override
        public void createControl(@NotNull Composite parent, @Nullable Object object, @NotNull Runnable propertyChangeListener) {
            Composite mode = UIUtils.createComposite(parent, 2);
            mode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            modeCombo = UIUtils.createLabelCombo(mode, PostgreMessages.dialog_connection_network_postgres_ssl_advanced_ssl_mode,
                SWT.DROP_DOWN | SWT.READ_ONLY);
            modeCombo.setItems(Arrays.stream(PostgreSSLConfigurator.SSL_MODES).filter(value -> !value.isEmpty()).toArray(String[]::new));
            createTrustStoreConfigGroup(parent);
            // These paths refer to the subscriber server, not the desktop filesystem.
            for (TextWithOpen path : new TextWithOpen[]{caCertPath, clientCertPath, clientKeyPath}) {
                UIUtils.setControlVisible(path.getToolbar(), false);
                path.getTextControl().addModifyListener(event -> propertyChangeListener.run());
            }
            UIUtils.createInfoLabel(parent, PostgreMessages.dialog_create_subscription_ssl_paths_tip);
            modeCombo.addListener(SWT.Selection, event -> {
                updateCertificateControls();
                propertyChangeListener.run();
            });
        }

        @Override
        protected boolean useCACertificate() {
            return true;
        }

        @Override
        public void loadSettings(@NotNull DBWHandlerConfiguration configuration) {
            super.loadSettings(configuration);
            String mode = configuration.getStringProperty(PostgreConstants.PROP_SSL_MODE);
            modeCombo.setText(CommonUtils.isEmpty(mode) ? "prefer" : mode);
            updateCertificateControls();
        }

        @Override
        public void saveSettings(@NotNull DBWHandlerConfiguration configuration) {
            super.saveSettings(configuration);
            configuration.setProperty(PostgreConstants.PROP_SSL_MODE, modeCombo.getText());
        }

        private void updateCertificateControls() {
            boolean enabled = !"disable".equals(modeCombo.getText());
            for (TextWithOpen path : new TextWithOpen[]{caCertPath, clientCertPath, clientKeyPath}) {
                path.setEnabled(enabled);
            }
        }
    }
}
