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
package org.jkiss.dbeaver.ext.mssql.ui;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;


public class SQLServerConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {

    private Label hostLabel;
    private Text hostText;
    private Label portLabel;
    private Text portText;
    private Label dbLabel;
    private Text dbText;
    private Text urlText;

    private Button showAllDatabases;
    private Button showAllSchemas;
    private Button encryptPassword;
    private Button trustServerCertificate;

    private boolean needsPort;

    private boolean activated;

    private final Image LOGO_AZURE;
    private final Image LOGO_BABELFISH;
    private final Image LOGO_SQLSERVER;
    private final Image LOGO_SYBASE;

    public SQLServerConnectionPage() {
        LOGO_AZURE = createImage("icons/azure_logo.png");
        LOGO_BABELFISH = createImage("icons/bbfsh_logo.png");
        LOGO_SQLSERVER = createImage("icons/mssql_logo.png");
        LOGO_SYBASE = createImage("icons/sybase_logo.png");
    }

    @Override
    public void dispose() {
        super.dispose();
        UIUtils.dispose(LOGO_AZURE);
        UIUtils.dispose(LOGO_BABELFISH);
        UIUtils.dispose(LOGO_SQLSERVER);
        UIUtils.dispose(LOGO_SYBASE);
    }

    @Override
    public void createControl(Composite composite) {
        ModifyListener textListener = e -> {
            if (activated) {
                updateUrl();
                site.updateButtons();
            }
        };
        
        boolean isSqlServer = isSqlServer();
        boolean isDriverAzure = isSqlServer && isDriverAzure();

        Composite settingsGroup = new Composite(composite, SWT.NONE);
        settingsGroup.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        settingsGroup.setLayoutData(gd);

        Group addrGroup = UIUtils.createControlGroup(
            settingsGroup,
            UIConnectionMessages.dialog_connection_server_label,
            4,
            GridData.FILL_HORIZONTAL,
            0
        );

        SelectionAdapter typeSwitcher = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setupConnectionModeSelection(urlText, typeURLRadio.getSelection(), GROUP_CONNECTION_ARR);
                updateUrl();
            }
        };
        createConnectionModeSwitcher(addrGroup, typeSwitcher);

        Label urlLabel = UIUtils.createControlLabel(addrGroup, UIConnectionMessages.dialog_connection_url_label);
        urlLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        urlText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        gd.grabExcessHorizontalSpace = true;
        gd.widthHint = 355;
        urlText.setLayoutData(gd);
        urlText.addModifyListener(e -> site.updateButtons());


        needsPort = CommonUtils.getBoolean(getSite().getDriver().getDriverParameter("needsPort"), true);
        {
            hostLabel = new Label(addrGroup, SWT.NONE);
            hostLabel.setText(SQLServerUIMessages.dialog_connection_host_label);
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            addControlToGroup(GROUP_CONNECTION, hostLabel);

            hostText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);
            hostText.addModifyListener(textListener);
            addControlToGroup(GROUP_CONNECTION, hostText);

            if (isDriverAzure || !needsPort) {
                // no port number for Azure
                gd.horizontalSpan = 3;
            } else {
                portLabel = new Label(addrGroup, SWT.NONE);
                portLabel.setText(SQLServerUIMessages.dialog_connection_port_label);
                portLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
                addControlToGroup(GROUP_CONNECTION, portLabel);

                portText = new Text(addrGroup, SWT.BORDER);
                gd = new GridData(GridData.CENTER);
                gd.widthHint = UIUtils.getFontHeight(portText) * 7;
                portText.setLayoutData(gd);
                portText.addModifyListener(textListener);
                addControlToGroup(GROUP_CONNECTION, portText);
            }
        }

        {
            dbLabel = new Label(addrGroup, SWT.NONE);
            dbLabel.setText(SQLServerUIMessages.dialog_connection_database_schema_label);
            dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            addControlToGroup(GROUP_CONNECTION, dbLabel);

            dbText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            //gd.widthHint = 270;
            gd.horizontalSpan = 3;
            dbText.setLayoutData(gd);
            dbText.addModifyListener(textListener);
            addControlToGroup(GROUP_CONNECTION, dbText);
        }

        {
            createAuthPanel(settingsGroup, 1);
        }

        {
            Group secureGroup = new Group(settingsGroup, SWT.NONE);
            secureGroup.setText(SQLServerUIMessages.dialog_setting_connection_settings);
            secureGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            secureGroup.setLayout(new GridLayout(1, false));

            if (!isSqlServer) {
                encryptPassword = UIUtils.createCheckbox(secureGroup, SQLServerUIMessages.dialog_setting_encrypt_password, SQLServerUIMessages.dialog_setting_encrypt_password_tip, false, 1);
            }
            if (isDriverAzure || isDriverBabelfish()) {
                showAllDatabases = UIUtils.createCheckbox(
                    secureGroup,
                    SQLServerUIMessages.dialog_setting_show_all_databases,
                    SQLServerUIMessages.dialog_setting_show_all_databases_tip,
                    false,
                    1);
            }
            showAllSchemas = UIUtils.createCheckbox(secureGroup, SQLServerUIMessages.dialog_setting_show_all_schemas, SQLServerUIMessages.dialog_setting_show_all_schemas_tip, true, 1);

            if (isSqlServer) {
                trustServerCertificate = UIUtils.createCheckbox(
                    secureGroup,
                    SQLServerUIMessages.dialog_setting_trust_server_certificate,
                    SQLServerUIMessages.dialog_setting_trust_server_certificate_tip,
                    false,
                    1);
            }
        }

        createDriverPanel(settingsGroup);
        setControl(settingsGroup);
    }

    @Override
    public boolean isComplete() {
        if (isCustomURL()) {
            return !CommonUtils.isEmpty(urlText.getText());
        } else {
            return hostText != null && !CommonUtils.isEmpty(hostText.getText());
        }
    }

    @Override
    public Image getImage() {
        DBPImage logoImage = site.getDriver().getLogoImage();
        if (logoImage != null) {
            return DBeaverIcons.getImage(logoImage);
        }
        Image logo = LOGO_SYBASE;
        if (isSqlServer()) {
            if (isDriverAzure()) {
                logo = LOGO_AZURE;
            } else if (isDriverBabelfish()) {
                logo = LOGO_BABELFISH;
            } else {
                logo = LOGO_SQLSERVER;
            }
        }
        return logo;
    }

    private boolean isDriverAzure() {
        return SQLServerUtils.isDriverAzure(getSite().getDriver());
    }

    private boolean isDriverBabelfish() {
        return SQLServerUtils.isDriverBabelfish(getSite().getDriver());
    }

    private boolean isSqlServer() {
        return SQLServerUtils.isDriverSqlServer(getSite().getDriver());
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        boolean isDriverAzure = isSqlServer() && isDriverAzure();

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText(
                    CommonUtils.toString(
                        site.getDriver().getDefaultHost(),
                        isDriverAzure ? SQLServerConstants.DEFAULT_HOST_AZURE : SQLServerConstants.DEFAULT_HOST)
                    );
            }
        }
        if (portText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                portText.setText(String.valueOf(connectionInfo.getHostPort()));
            } else if (site.getDriver().getDefaultPort() != null) {
                portText.setText(site.getDriver().getDefaultPort());
            } else {
                portText.setText("");
            }
        }
        if (dbText != null) {
            String databaseName = connectionInfo.getDatabaseName();
            if (getSite().isNew() && CommonUtils.isEmpty(databaseName)) {
                databaseName = CommonUtils.notEmpty(site.getDriver().getDefaultDatabase());
            }
            dbText.setText(CommonUtils.notEmpty(databaseName));
        }
        if (showAllDatabases != null) {
            showAllDatabases.setSelection(
                CommonUtils.toBoolean(connectionInfo.getProviderProperty(SQLServerConstants.PROP_SHOW_ALL_DATABASES)));
        }
        showAllSchemas.setSelection(CommonUtils.toBoolean(connectionInfo.getProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS)));
        if (trustServerCertificate != null) {
            trustServerCertificate.setSelection(CommonUtils.getBoolean(
                connectionInfo.getProviderProperty(SQLServerConstants.PROP_SSL_TRUST_SERVER_CERTIFICATE), false));
        }

        if (!isSqlServer() && encryptPassword != null) {
            encryptPassword.setSelection(CommonUtils.toBoolean(connectionInfo.getProviderProperty(SQLServerConstants.PROP_ENCRYPT_PASSWORD)));
        }
        boolean useURL = connectionInfo.getConfigurationType() == DBPDriverConfigurationType.URL;
        if (useURL) {
            urlText.setText(connectionInfo.getUrl());
        }
        setupConnectionModeSelection(urlText, useURL, GROUP_CONNECTION_ARR);
        updateUrl();

        activated = true;
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        
        connectionInfo.setConfigurationType(
            typeURLRadio != null && typeURLRadio.getSelection() ? DBPDriverConfigurationType.URL : DBPDriverConfigurationType.MANUAL);
        
        if (hostText != null) {
            connectionInfo.setHostName(hostText.getText().trim());
        }
        if (portText != null) {
            connectionInfo.setHostPort(portText.getText().trim());
        }
        if (dbText != null) {
            connectionInfo.setDatabaseName(dbText.getText().trim());
        }

        if (showAllDatabases != null) {
            connectionInfo.setProviderProperty(SQLServerConstants.PROP_SHOW_ALL_DATABASES,
                String.valueOf(showAllDatabases.getSelection()));
        }

        if (showAllSchemas != null) {
            connectionInfo.setProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS,
                String.valueOf(showAllSchemas.getSelection()));
        }
        if (trustServerCertificate != null) {
            connectionInfo.setProviderProperty(SQLServerConstants.PROP_SSL_TRUST_SERVER_CERTIFICATE,
                String.valueOf(trustServerCertificate.getSelection()));
        }

        if (encryptPassword != null) {
            connectionInfo.setProviderProperty(SQLServerConstants.PROP_ENCRYPT_PASSWORD,
                String.valueOf(encryptPassword.getSelection()));
        }
        if (typeURLRadio != null && typeURLRadio.getSelection()) {
            connectionInfo.setUrl(urlText.getText());
        }

        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[] { new DriverPropertiesDialogPage(this) };
    }

    private void updateUrl() {
        DBPDataSourceContainer dataSourceContainer = site.getActiveDataSource();
        saveSettings(dataSourceContainer);
        if (typeURLRadio != null && typeURLRadio.getSelection()) {
            urlText.setText(dataSourceContainer.getConnectionConfiguration().getUrl());
        } else {
            urlText.setText(dataSourceContainer.getDriver().getConnectionURL(site.getActiveDataSource().getConnectionConfiguration()));
        }
    }

}
