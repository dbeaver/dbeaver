/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

public class SQLServerConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {

    private Text hostText;
    private Text portText;
    private Text dbText;

    private Button showAllDatabases;
    private Button showAllSchemas;
    private Button encryptPassword;

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
    public void dispose()
    {
        super.dispose();
        UIUtils.dispose(LOGO_AZURE);
        UIUtils.dispose(LOGO_BABELFISH);
        UIUtils.dispose(LOGO_SQLSERVER);
        UIUtils.dispose(LOGO_SYBASE);
    }

    @Override
    public void createControl(Composite composite)
    {
        boolean isSqlServer = isSqlServer();
        boolean isDriverAzure = isSqlServer && isDriverAzure();

        Composite settingsGroup = new Composite(composite, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        settingsGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        settingsGroup.setLayoutData(gd);

        needsPort = CommonUtils.getBoolean(getSite().getDriver().getDriverParameter("needsPort"), true);
        {
            Label hostLabel = new Label(settingsGroup, SWT.NONE);
            hostLabel.setText(SQLServerUIMessages.dialog_connection_host_label);
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            hostText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);

            if (isDriverAzure || !needsPort) {
                // no port number for Azure
                gd.horizontalSpan = 3;
            } else {
                Label portLabel = new Label(settingsGroup, SWT.NONE);
                portLabel.setText(SQLServerUIMessages.dialog_connection_port_label);
                portLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

                portText = new Text(settingsGroup, SWT.BORDER);
                gd = new GridData(GridData.CENTER);
                gd.widthHint = UIUtils.getFontHeight(portText) * 7;
                portText.setLayoutData(gd);
            }
        }

        {
            Label dbLabel = new Label(settingsGroup, SWT.NONE);
            dbLabel.setText(SQLServerUIMessages.dialog_connection_database_schema_label);
            dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            dbText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            //gd.widthHint = 270;
            gd.horizontalSpan = 3;
            dbText.setLayoutData(gd);
        }

        {
            createAuthPanel(settingsGroup, 4);
        }

        {
            Group secureGroup = new Group(settingsGroup, SWT.NONE);
            secureGroup.setText(SQLServerUIMessages.dialog_setting_connection_settings);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 4;
            secureGroup.setLayoutData(gd);
            secureGroup.setLayout(new GridLayout(1, false));

            if (!isSqlServer) {
                encryptPassword = UIUtils.createCheckbox(secureGroup, SQLServerUIMessages.dialog_setting_encrypt_password, SQLServerUIMessages.dialog_setting_encrypt_password_tip, false, 2);
            }
            if (isDriverAzure || isDriverBabelfish()) {
                showAllDatabases = UIUtils.createCheckbox(
                    secureGroup,
                    SQLServerUIMessages.dialog_setting_show_all_databases,
                    SQLServerUIMessages.dialog_setting_show_all_databases_tip,
                    false,
                    2);
            }
            showAllSchemas = UIUtils.createCheckbox(secureGroup, SQLServerUIMessages.dialog_setting_show_all_schemas, SQLServerUIMessages.dialog_setting_show_all_schemas_tip, true, 2);
        }

        createDriverPanel(settingsGroup);
        setControl(settingsGroup);
    }

    @Override
    public boolean isComplete()
    {
        return hostText != null && !CommonUtils.isEmpty(hostText.getText());
    }

    @Override
    public Image getImage() {
        Image logo = LOGO_SYBASE;
        if (isSqlServer()) {
            if (isDriverAzure()) {
                logo = LOGO_AZURE;
            }
            else if (isDriverBabelfish()) {
                logo = LOGO_BABELFISH;
            }
            else {
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
    public void loadSettings()
    {
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

        if (!isSqlServer()) {
            encryptPassword.setSelection(CommonUtils.toBoolean(connectionInfo.getProviderProperty(SQLServerConstants.PROP_ENCRYPT_PASSWORD)));
        }

        activated = true;
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
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

        if (encryptPassword != null) {
            connectionInfo.setProviderProperty(SQLServerConstants.PROP_ENCRYPT_PASSWORD,
                String.valueOf(encryptPassword.getSelection()));
        }

        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate)
    {
        return new IDialogPage[] {
                new DriverPropertiesDialogPage(this)
        };
    }

}
