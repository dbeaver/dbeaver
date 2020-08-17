/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerAuthentication;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class SQLServerConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage
{

    private static final ImageDescriptor LOG_AZURE = SQLServerUIActivator.getImageDescriptor("icons/azure_logo.png");
    private static final ImageDescriptor LOGO_SQLSERVER = SQLServerUIActivator.getImageDescriptor("icons/mssql_logo.png");
    private static final ImageDescriptor LOGO_SYBASE = SQLServerUIActivator.getImageDescriptor("icons/sybase_logo.png");

    private Text hostText;
    private Text portText;
    private Text dbText;
    private Label userNameLabel;
    private Text userNameText;
    private Label passwordLabel;

    private SQLServerAuthentication[] authSchemas;
    private Combo authCombo;
//    private Button windowsAuthenticationButton;
//    private Button adpAuthenticationButton;
    private Button trustServerCertificate;
    private Button showAllSchemas;

    private boolean activated;

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite)
    {
        boolean isSqlServer = SQLServerUtils.isDriverSqlServer(getSite().getDriver());
        boolean isDriverAzure = isSqlServer && SQLServerUtils.isDriverAzure(getSite().getDriver());

        Composite settingsGroup = new Composite(composite, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        settingsGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        settingsGroup.setLayoutData(gd);

        {
            Label hostLabel = new Label(settingsGroup, SWT.NONE);
            hostLabel.setText(SQLServerUIMessages.dialog_connection_host_label);
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            hostText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);

            if (isDriverAzure) {
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
            if (SQLServerUtils.isDriverSqlServer(getSite().getDriver())) {
                boolean isJtds = SQLServerUtils.isDriverJtds(getSite().getDriver());

                List<SQLServerAuthentication> supportedSchemas = new ArrayList<>();
                for (SQLServerAuthentication auth : SQLServerAuthentication.values()) {
                    if (!isJtds || auth.isSupportsJTDS()) {
                        supportedSchemas.add(auth);
                    }
                }
                authSchemas = supportedSchemas.toArray(new SQLServerAuthentication[0]);

                authCombo = UIUtils.createLabelCombo(settingsGroup, SQLServerUIMessages.dialog_connection_authentication_combo,
                    SQLServerUIMessages.dialog_connection_authentication_combo_tip, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.BORDER);
                for (SQLServerAuthentication authentication : authSchemas) {
                    authCombo.add(authentication.getTitle());
                }
                authCombo.select(0);
                authCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        updateSecurityControls();
                    }
                });
                authCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

                UIUtils.createEmptyLabel(settingsGroup, 2, 1);

/*
                if (!isDriverAzure) {
                    windowsAuthenticationButton = UIUtils.createLabelCheckbox(settingsGroup, SQLServerUIMessages.dialog_connection_windows_authentication_button, false);
                    windowsAuthenticationButton.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            enableTexts();
                        }
                    });
                    UIUtils.createEmptyLabel(settingsGroup, 2, 1);
                } else {
                    adpAuthenticationButton = UIUtils.createLabelCheckbox(settingsGroup, SQLServerUIMessages.dialog_connection_adp_authentication_button, false);
                    UIUtils.createEmptyLabel(settingsGroup, 2, 1);
                }
*/
            }

            userNameLabel = new Label(settingsGroup, SWT.NONE);
            userNameLabel.setText(SQLServerUIMessages.dialog_connection_user_name_label);
            userNameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            userNameText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            userNameText.setLayoutData(gd);

            UIUtils.createEmptyLabel(settingsGroup, 2, 1);

            passwordLabel = new Label(settingsGroup, SWT.NONE);
            passwordLabel.setText(SQLServerUIMessages.dialog_connection_password_label);
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = createPasswordText(settingsGroup, null);

            UIUtils.createEmptyLabel(settingsGroup, 2, 1);
        }

        {
            Group secureGroup = new Group(settingsGroup, SWT.NONE);
            secureGroup.setText(SQLServerUIMessages.dialog_setting_connection_settings);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 4;
            secureGroup.setLayoutData(gd);
            secureGroup.setLayout(new GridLayout(1, false));

            createPasswordControls(secureGroup);
            trustServerCertificate = UIUtils.createCheckbox(secureGroup, SQLServerUIMessages.dialog_setting_trust_server_certificate, SQLServerUIMessages.dialog_setting_trust_server_certificate_tip, true, 2);
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
    public void loadSettings()
    {
        super.loadSettings();

        boolean isSqlServer = SQLServerUtils.isDriverSqlServer(getSite().getDriver());
        boolean isDriverAzure = isSqlServer && SQLServerUtils.isDriverAzure(getSite().getDriver());

        {
            setImageDescriptor(isSqlServer ?
                (isDriverAzure ?
                    LOG_AZURE :
                    LOGO_SQLSERVER) :
                LOGO_SYBASE);
        }

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText(isDriverAzure ? SQLServerConstants.DEFAULT_HOST_AZURE : SQLServerConstants.DEFAULT_HOST);
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
            dbText.setText(databaseName);
        }
        if (userNameText != null) {
            if (site.isNew() && CommonUtils.isEmpty(connectionInfo.getUserName())) {
                userNameText.setText(CommonUtils.notEmpty(site.getDriver().getDefaultUser()));
            } else {
                userNameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
            }
        }
        if (passwordText != null) {
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
        }
        if (authCombo != null) {
            authCombo.select(ArrayUtils.indexOf(authSchemas, SQLServerUtils.detectAuthSchema(connectionInfo)));
            updateSecurityControls();
        }
/*
        if (windowsAuthenticationButton != null) {
            windowsAuthenticationButton.setSelection(SQLServerUtils.isWindowsAuth(connectionInfo));
            enableTexts();
        }
        if (adpAuthenticationButton != null) {
            adpAuthenticationButton.setSelection(SQLServerUtils.isActiveDirectoryAuth(connectionInfo));
        }
*/
        trustServerCertificate.setSelection(CommonUtils.getBoolean(connectionInfo.getProperty(SQLServerConstants.PROP_TRUST_SERVER_CERTIFICATE), true));
        showAllSchemas.setSelection(CommonUtils.toBoolean(connectionInfo.getProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS)));

        activated = true;
    }

    private void updateSecurityControls() {
        SQLServerAuthentication authSchema = authSchemas[authCombo.getSelectionIndex()];

        boolean supportsUserName = authSchema.isAllowsUserName();
        boolean supportsPassword = authSchema.isAllowsPassword();
        userNameLabel.setEnabled(supportsUserName);
        userNameText.setEnabled(supportsUserName);
        passwordLabel.setEnabled(supportsPassword);
        passwordText.setEnabled(supportsPassword);
        savePasswordCheck.setEnabled(supportsPassword);
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
        if (userNameText != null) {
            connectionInfo.setUserName(userNameText.getText().trim());
        }
        if (passwordText != null) {
            connectionInfo.setUserPassword(passwordText.getText());
        }
        if (authCombo != null) {
            SQLServerAuthentication authSchema = authSchemas[authCombo.getSelectionIndex()];
            connectionInfo.setProviderProperty(SQLServerConstants.PROP_AUTHENTICATION,
                authSchema.name());

            if (SQLServerConstants.PROVIDER_GENERIC.equals(getSite().getDriver().getProviderId())) {
                if (authSchema == SQLServerAuthentication.WINDOWS_INTEGRATED) {
                    connectionInfo.getProperties().put(SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY,
                        String.valueOf(true));
                } else {
                    connectionInfo.getProperties().remove(SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY);
                }
            }
        }

/*
        if (windowsAuthenticationButton != null) {
            connectionInfo.getProperties().put(SQLServerConstants.PROP_CONNECTION_INTEGRATED_SECURITY,
                    String.valueOf(windowsAuthenticationButton.getSelection()));
        }
        if (adpAuthenticationButton != null) {
            if (adpAuthenticationButton.getSelection()) {
                connectionInfo.getProperties().put(SQLServerConstants.PROP_CONNECTION_AUTHENTICATION, SQLServerConstants.AUTH_ACTIVE_DIRECTORY_PASSWORD);
            } else {
                connectionInfo.getProperties().remove(SQLServerConstants.PROP_CONNECTION_AUTHENTICATION);
            }
        }
*/
        if (trustServerCertificate != null) {
            connectionInfo.setProperty(SQLServerConstants.PROP_TRUST_SERVER_CERTIFICATE,
                String.valueOf(trustServerCertificate.getSelection()));
        }
        if (showAllSchemas != null) {
            connectionInfo.setProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS,
                String.valueOf(showAllSchemas.getSelection()));
        }

        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getSubPages(boolean extrasOnly, boolean forceCreate)
    {
        return new IDialogPage[] {
                new DriverPropertiesDialogPage(this)
        };
    }

}
