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

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ConfigurationFileSelector;
import org.jkiss.dbeaver.ui.controls.DatabaseLabelProviders;
import org.jkiss.dbeaver.ui.controls.ExpandableCompositeEx;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.dialogs.connection.DatabaseNativeAuthModelConfigurator;
import org.jkiss.dbeaver.ui.dialogs.net.SSLConfiguratorTrustStoreUI;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class PostgreSubscriptionConnectionPanel extends Composite {
    private final Runnable onChange;
    private final Runnable onLayoutChange;
    private final DataSourceDescriptor publisher;
    private DBWHandlerConfiguration ssl;
    private final PublisherCredentialsConfigurator credentials = new PublisherCredentialsConfigurator();
    private final PublisherSSLConfigurator sslConfigurator = new PublisherSSLConfigurator();
    private Button hostMode;
    private Button urlMode;
    private Text urlText;
    private Composite endpoint;
    private Text hostText;
    private Text portText;
    private Text databaseText;
    private Label errorLabel;
    private String connectionInfo = "";
    private boolean loadingSettings;
    private Composite connectionSettings;
    private Composite testArea;
    private Composite connection;
    private Composite authentication;
    private DBPConnectionConfiguration desktopConfiguration;
    private String copiedConnectionName;
    private String copiedConnectionId;
    private boolean useCopiedConnection;

    public PostgreSubscriptionConnectionPanel(
        @NotNull Composite parent,
        @NotNull PostgreDatabase subscriberDatabase,
        @NotNull DBPConnectionConfiguration initialConfiguration,
        @NotNull Runnable onChange,
        @NotNull Runnable onLayoutChange
    ) {
        super(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = PostgreSubscriptionDialogLayout.SECTION_SPACING;
        setLayout(layout);
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.onChange = onChange;
        this.onLayoutChange = onLayoutChange;
        DBPDataSourceContainer subscriber = subscriberDatabase.getDataSource().getContainer();
        DBPConnectionConfiguration workingConfiguration =
            DBPConnectionConfiguration.copyWithIndependentRuntimeAttributes(initialConfiguration);
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
        ((GridLayout) connectionSettings.getLayout()).marginLeft = 0;
        ((GridLayout) connectionSettings.getLayout()).verticalSpacing = PostgreSubscriptionDialogLayout.COMPACT_SPACING;
        connection = UIUtils.createComposite(connectionSettings, 2);
        GridLayout connectionLayout = (GridLayout) connection.getLayout();
        connectionLayout.marginLeft = PostgreSubscriptionDialogLayout.MARGIN;
        connectionLayout.verticalSpacing = PostgreSubscriptionDialogLayout.ROW_SPACING;
        connection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createControlLabel(connection, PostgreMessages.dialog_create_subscription_connect_by);
        Composite modes = UIUtils.createComposite(connection, 3);
        modes.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hostMode = UIUtils.createRadioButton(modes, PostgreMessages.dialog_create_subscription_host_mode, null, null);
        hostMode.setSelection(true);
        urlMode = UIUtils.createRadioButton(modes, UIConnectionMessages.dialog_connection_url_label, null, null);
        Button copy = UIUtils.createDialogButton(modes, PostgreMessages.dialog_create_subscription_existing_connection,
            SelectionListener.widgetSelectedAdapter(event -> showConnectionsMenu((Button) event.widget)));
        copy.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        copy.setToolTipText(PostgreMessages.dialog_create_subscription_existing_connection_tip);
        urlText = UIUtils.createLabelText(connection, UIConnectionMessages.dialog_connection_url_label, "");
        urlText.setMessage(PostgreMessages.dialog_create_subscription_url_placeholder);
        urlText.setToolTipText(PostgreMessages.dialog_create_subscription_url_tip);
        urlText.setEnabled(false);
        hostMode.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> updateConnectionMode()));
        urlMode.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> updateConnectionMode()));
        DBPConnectionConfiguration workingConfiguration = publisher.getConnectionConfiguration();
        UIUtils.createControlLabel(connection, PostgreMessages.dialog_setting_connection_host);
        endpoint = UIUtils.createComposite(connection, 3);
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
        databaseText = UIUtils.createLabelText(connection, PostgreMessages.dialog_setting_connection_database,
            CommonUtils.notEmpty(workingConfiguration.getDatabaseName()));
        hostText.setToolTipText(PostgreMessages.dialog_create_subscription_endpoint_tip);
        portText.setToolTipText(PostgreMessages.dialog_create_subscription_endpoint_tip);
        authentication = UIUtils.createComposite(connectionSettings, 2);
        GridLayout authenticationLayout = (GridLayout) authentication.getLayout();
        authenticationLayout.marginLeft = PostgreSubscriptionDialogLayout.MARGIN;
        authenticationLayout.marginTop = PostgreSubscriptionDialogLayout.ROW_SPACING;
        authenticationLayout.verticalSpacing = PostgreSubscriptionDialogLayout.ROW_SPACING;
        authentication.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        credentials.createControl(authentication, AuthModelDatabaseNative.INSTANCE, this::validateInput);
        credentials.loadSettings(publisher);
        for (Text text : new Text[]{hostText, portText, databaseText, urlText}) {
            text.addModifyListener(event -> validateInput());
        }

        testArea = UIUtils.createComposite(connectionSettings, 2);
        testArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ExpandableCompositeEx sslSection = UIUtils.createExpandableCompositeWithSeparator(area, SWT.NONE,
            ExpandableCompositeEx.TWISTIE);
        sslSection.setShowTextAsTitle(true);
        sslSection.clientVerticalSpacing = PostgreSubscriptionDialogLayout.ROW_SPACING;
        sslSection.setText(PostgreMessages.dialog_create_subscription_ssl_tab);
        sslSection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Composite sslPage = UIUtils.createComposite(sslSection, 1);
        ((GridLayout) sslPage.getLayout()).marginLeft = PostgreSubscriptionDialogLayout.MARGIN;
        sslSection.setClient(sslPage);
        sslConfigurator.createControl(sslPage, null, this::validateInput);
        sslConfigurator.loadSettings(ssl);
        sslSection.addExpansionListener(IExpansionListener.expansionStateChangedAdapter(event -> onLayoutChange.run()));

        errorLabel = new Label(connectionSettings, SWT.WRAP);
        GridData errorData = new GridData(GridData.FILL_HORIZONTAL);
        errorData.widthHint = UIUtils.getFontHeight(area) * 40;
        errorLabel.setLayoutData(errorData);
    }

    void alignLabels(@NotNull Composite general) {
        int width = PostgreSubscriptionDialogLayout.alignLabels(PostgreSubscriptionDialogLayout.LABEL_WIDTH,
            connection, authentication);
        int generalWidth = PostgreSubscriptionDialogLayout.alignLabels(width + PostgreSubscriptionDialogLayout.MARGIN, general);
        width = PostgreSubscriptionDialogLayout.alignLabels(generalWidth - PostgreSubscriptionDialogLayout.MARGIN,
            connection, authentication);
        ((GridData) testArea.getLayoutData()).horizontalIndent = PostgreSubscriptionDialogLayout.MARGIN + width
            + ((GridLayout) connection.getLayout()).horizontalSpacing;
    }

    @NotNull
    public Composite getTestArea() {
        return testArea;
    }

    private void updateConnectionMode() {
        urlText.setEnabled(urlMode.getSelection());
        UIUtils.enableWithChildren(endpoint, hostMode.getSelection());
        databaseText.setEnabled(hostMode.getSelection());
        if (urlMode.getSelection()) {
            useCopiedConnection = false;
        }
        validateInput();
    }

    private void showConnectionsMenu(@NotNull Button button) {
        Menu menu = new Menu(button);
        for (DBPDataSourceContainer candidate : publisher.getRegistry().getDataSources()) {
            if (!candidate.isHidden() && !candidate.isTemporary() && PostgreSubscription.supportsPublisherConnection(candidate)) {
                MenuItem item = new MenuItem(menu, SWT.PUSH);
                item.setText(candidate.getName());
                item.setImage(new DatabaseLabelProviders.ConnectionLabelProvider().getImage(candidate));
                item.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> loadConnection(candidate)));
            }
        }
        menu.addListener(SWT.Hide, event -> getDisplay().asyncExec(() -> {
            if (!menu.isDisposed()) {
                menu.dispose();
            }
        }));
        Point position = button.toDisplay(0, button.getSize().y);
        menu.setLocation(position);
        menu.setVisible(true);
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
            desktopConfiguration = DBPConnectionConfiguration.copyWithIndependentRuntimeAttributes(selected.getConnectionConfiguration());
            desktopConfiguration.setUserName(copy.getUserName());
            desktopConfiguration.setUserPassword(copy.getUserPassword());
            copiedConnectionName = selected.getName();
            copiedConnectionId = selected.getId();
            loadingSettings = true;
            try {
                publisher.setConnectionInfo(copy);
                initializeSSL(copy);
                hostText.setText(CommonUtils.notEmpty(copy.getHostName()));
                portText.setText(CommonUtils.isEmpty(copy.getHostPort())
                    ? Integer.toString(PostgreConstants.DEFAULT_PORT) : copy.getHostPort());
                databaseText.setText(CommonUtils.notEmpty(copy.getDatabaseName()));
                hostMode.setSelection(true);
                urlMode.setSelection(false);
                urlText.setEnabled(false);
                UIUtils.enableWithChildren(endpoint, true);
                databaseText.setEnabled(true);
                credentials.loadSettings(publisher);
                sslConfigurator.loadSettings(ssl);
                useCopiedConnection = true;
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
        } finally {
            if (!isDisposed()) {
                getShell().forceActive();
            }
        }
    }

    private void validateInput() {
        if (errorLabel == null || loadingSettings) {
            return;
        }
        saveInput();
        try {
            if (urlMode.getSelection()) {
                connectionInfo = PostgreSubscription.buildConnectionUri(urlText.getText(), publisher.getConnectionConfiguration());
            } else {
                connectionInfo = PostgreSubscription.buildConnectionInfo(publisher.getConnectionConfiguration());
                boolean wasLoading = loadingSettings;
                loadingSettings = true;
                try {
                    String host = hostText.getText().trim();
                    if (host.contains(":") && !host.startsWith("[") && !host.contains(",")) {
                        host = "[" + host + "]";
                    } else if (host.contains("/")) {
                        host = URLEncoder.encode(host, StandardCharsets.UTF_8).replace("+", "%20");
                    }
                    String port = portText.getText().isBlank()
                        ? Integer.toString(PostgreConstants.DEFAULT_PORT) : portText.getText().trim();
                    String database = URLEncoder.encode(databaseText.getText().trim(), StandardCharsets.UTF_8).replace("+", "%20");
                    urlText.setText("postgresql://" + host + ":" + port + "/" + database);
                } finally {
                    loadingSettings = wasLoading;
                }
            }
            errorLabel.setText("");
        } catch (DBException e) {
            connectionInfo = "";
            errorLabel.setText(urlMode.getSelection() ? urlText.getText().isBlank() ? "" : e.getMessage()
                : hostText.getText().isBlank() || databaseText.getText().isBlank() ? "" : e.getMessage());
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

    private boolean hasCopiedConnection() {
        return desktopConfiguration != null && hostMode.getSelection();
    }

    @NotNull
    public String[] getPublicationSources() {
        String configured = PostgreMessages.dialog_create_subscription_configured_publisher;
        return hasCopiedConnection() ? new String[]{configured, getCopiedPublicationSourceName()} : new String[]{configured};
    }

    public int getPublicationSourceIndex() {
        return useCopiedConnection ? 1 : 0;
    }

    public void selectPublicationSource(int index) {
        boolean copied = index == 1 && hasCopiedConnection();
        if (useCopiedConnection != copied) {
            useCopiedConnection = copied;
            validateInput();
        }
    }

    @NotNull
    private String getCopiedPublicationSourceName() {
        return NLS.bind(PostgreMessages.dialog_create_subscription_copied_publisher,
            copiedConnectionName + " / " + publisher.getConnectionConfiguration().getDatabaseName());
    }

    @NotNull
    public String getPublicationSourceName() {
        if (useCopiedConnection) {
            return getCopiedPublicationSourceName();
        }
        return PostgreMessages.dialog_create_subscription_configured_publisher;
    }

    @NotNull
    public DataSourceDescriptor createPublicationConnection() throws DBException {
        DBPConnectionConfiguration currentConfiguration = publisher.getConnectionConfiguration();
        DBPConnectionConfiguration configuration = DBPConnectionConfiguration.copyWithIndependentRuntimeAttributes(
            useCopiedConnection ? desktopConfiguration : currentConfiguration);
        configuration.setDatabaseName(currentConfiguration.getDatabaseName());
        configuration.setUserName(currentConfiguration.getUserName());
        configuration.setUserPassword(currentConfiguration.getUserPassword());
        if (urlMode.getSelection()) {
            configuration = PostgreSubscription.createPublicationConnectionConfiguration(connectionInfo, configuration);
        } else {
            configuration.setConfigurationType(DBPDriverConfigurationType.MANUAL);
            configuration.setUrl(publisher.getDriver().getDataSourceProvider().getConnectionURL(publisher.getDriver(), configuration));
        }
        DataSourceDescriptor discovery = (DataSourceDescriptor) publisher.getRegistry().createDataSource(
            DataSourceDescriptor.generateNewId(publisher.getDriver()), publisher.getDriver(), configuration);
        discovery.setName(getPublicationSourceName());
        discovery.setTemporary(true);
        discovery.setSavePassword(true);
        return discovery;
    }

    @Nullable
    public String getPublicationSourceKey() {
        if (connectionInfo.isBlank()) {
            return null;
        }
        try {
            String url;
            if (useCopiedConnection) {
                DBPConnectionConfiguration configuration =
                    DBPConnectionConfiguration.copyWithIndependentRuntimeAttributes(desktopConfiguration);
                configuration.setDatabaseName(publisher.getConnectionConfiguration().getDatabaseName());
                url = publisher.getDriver().getDataSourceProvider().getConnectionURL(publisher.getDriver(), configuration);
            } else {
                String uri = urlMode.getSelection() ? connectionInfo
                    : PostgreSubscription.buildConnectionUri(urlText.getText(), publisher.getConnectionConfiguration());
                url = PostgreSubscription.createPublicationConnectionConfiguration(uri, publisher.getConnectionConfiguration()).getUrl();
            }
            String endpoint = url.split("\\?", 2)[0];
            int databaseStart = endpoint.indexOf('/', "jdbc:postgresql://".length());
            if (databaseStart < 0) {
                return endpoint;
            }
            return (useCopiedConnection ? copiedConnectionId + ":" : "")
                + endpoint.substring(0, databaseStart).toLowerCase(Locale.ROOT).replaceAll(":5432(?=,|$)", "")
                + endpoint.substring(databaseStart);
        } catch (DBException e) {
            return null;
        }
    }

    private static class PublisherCredentialsConfigurator extends DatabaseNativeAuthModelConfigurator {
        @Override
        protected void createPasswordControls(@NotNull Composite parent, @NotNull Runnable propertyChangeListener) {
            super.createPasswordControls(parent, propertyChangeListener);
            GridData passwordData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
            passwordData.widthHint = UIUtils.getFontHeight(parent) * 18;
            passwordText.getParent().setLayoutData(passwordData);
            if (savePasswordCheck != null) {
                savePasswordCheck.dispose();
                savePasswordCheck = null;
            }
        }

        @NotNull
        @Override
        protected GridData makeAuthControlLayoutData(@NotNull Composite parent) {
            GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
            data.widthHint = UIUtils.getFontHeight(parent) * 18;
            return data;
        }

        @Override
        public void loadSettings(@NotNull DBPDataSourceContainer dataSource) {
            super.loadSettings(dataSource);
            passwordText.setEnabled(canEditCredentialsPerPolicy);
            if (savePasswordCheck != null) {
                savePasswordCheck.setEnabled(canEditCredentialsPerPolicy);
                savePasswordCheck.setSelection(dataSource.isSavePassword());
            }
            if (showPasswordButton != null) {
                showPasswordButton.setEnabled(canEditCredentialsPerPolicy);
            }
        }

        @Override
        public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
            super.saveSettings(dataSource);
            if (canEditCredentialsPerPolicy) {
                // whitespace is significant in libpq passwords; the standard datasource configurator trims it.
                dataSource.getConnectionConfiguration().setUserPassword(passwordText.getText());
            }
        }
    }

    private static class PublisherSSLConfigurator extends SSLConfiguratorTrustStoreUI {
        private Combo modeCombo;

        @Override
        public void createControl(@NotNull Composite parent, @Nullable Object object, @NotNull Runnable propertyChangeListener) {
            sslCertComposite = UIUtils.createComposite(parent, 2);
            sslCertComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            ((GridLayout) sslCertComposite.getLayout()).verticalSpacing = PostgreSubscriptionDialogLayout.ROW_SPACING;
            modeCombo = UIUtils.createLabelCombo(sslCertComposite,
                PostgreMessages.dialog_connection_network_postgres_ssl_advanced_ssl_mode, SWT.DROP_DOWN | SWT.READ_ONLY);
            modeCombo.setItems(Arrays.stream(PostgreSSLConfigurator.SSL_MODES).filter(value -> !value.isEmpty()).toArray(String[]::new));
            caCertPath = createCertificateControl(sslCertComposite, UIConnectionMessages.dialog_setting_ssl_configurator_certs_ca_name);
            clientCertPath = createCertificateControl(sslCertComposite,
                UIConnectionMessages.dialog_setting_ssl_configurator_certs_client_name);
            clientKeyPath = createCertificateControl(sslCertComposite,
                UIConnectionMessages.dialog_setting_ssl_configurator_certs_client_key_name);
            sslKeyStoreComposite = UIUtils.createComposite(parent, 1);
            // paths refer to the subscriber server
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

        @NotNull
        private TextWithOpen createCertificateControl(@NotNull Composite parent, @NotNull String label) {
            UIUtils.createControlLabel(parent, label);
            TextWithOpen field = new ConfigurationFileSelector(parent, label, new String[]{"*.*"});
            field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            return field;
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
