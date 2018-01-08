/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.jkiss.dbeaver.ext.mssql.SQLServerActivator;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerMessages;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

public class SQLServerConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage
{
    private Text hostText;
    private Text portText;
    private Text dbText;
    private Label userNameLabel;
    private Text userNameText;
    private Label passwordLabel;
    private Text passwordText;
    private Button windowsAuthenticationButton;
    private Button showAllSchemas;

    private boolean activated;

    private static ImageDescriptor MSSQL_LOGO_IMG = SQLServerActivator.getImageDescriptor("icons/mssql_logo.png");
    private static ImageDescriptor SYBASE_LOGO_IMG = SQLServerActivator.getImageDescriptor("icons/sybase_logo.png");

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite)
    {
        Composite settingsGroup = new Composite(composite, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        settingsGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        settingsGroup.setLayoutData(gd);

        {
            Label hostLabel = new Label(settingsGroup, SWT.NONE);
            hostLabel.setText(SQLServerMessages.dialog_connection_host_label);
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            hostText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);

            Label portLabel = new Label(settingsGroup, SWT.NONE);
            portLabel.setText(SQLServerMessages.dialog_connection_port_label);
            portLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            portText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.CENTER);
            gd.widthHint = 60;
            portText.setLayoutData(gd);
        }

        {
            Label dbLabel = new Label(settingsGroup, SWT.NONE);
            dbLabel.setText(SQLServerMessages.dialog_connection_database_schema_label);
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
                windowsAuthenticationButton = UIUtils.createLabelCheckbox(settingsGroup, SQLServerMessages.dialog_connection_windows_authentication_button, false);
                windowsAuthenticationButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        enableTexts();
                    }
                });
                createEmptyLabel(settingsGroup, 1);
            }

            userNameLabel = new Label(settingsGroup, SWT.NONE);
            userNameLabel.setText(SQLServerMessages.dialog_connection_user_name_label);
            userNameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            userNameText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            userNameText.setLayoutData(gd);

            createEmptyLabel(settingsGroup, 1);

            passwordLabel = new Label(settingsGroup, SWT.NONE);
            passwordLabel.setText(SQLServerMessages.dialog_connection_password_label);
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = new Text(settingsGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            passwordText.setLayoutData(gd);

            createEmptyLabel(settingsGroup, 1);
        }

        {
            Group secureGroup = new Group(settingsGroup, SWT.NONE);
            secureGroup.setText(SQLServerMessages.dialog_setting_connection_settings);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 4;
            secureGroup.setLayoutData(gd);
            secureGroup.setLayout(new GridLayout(2, false));

            showAllSchemas = UIUtils.createCheckbox(secureGroup, SQLServerMessages.dialog_setting_show_all_schemas, SQLServerMessages.dialog_setting_show_all_schemas_tip, true, 2);
        }

        createDriverPanel(settingsGroup);
        setControl(settingsGroup);
    }

    private void enableTexts() {
        boolean isWindowsAuth = windowsAuthenticationButton != null && windowsAuthenticationButton.getSelection();
        userNameLabel.setEnabled(!isWindowsAuth);
        userNameText.setEnabled(!isWindowsAuth);
        passwordLabel.setEnabled(!isWindowsAuth);
        passwordText.setEnabled(!isWindowsAuth);
    }

    private void createEmptyLabel(Composite parent, int verticalSpan)
    {
        Label emptyLabel = new Label(parent, SWT.NONE);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gd.horizontalSpan = 2;
        gd.verticalSpan = verticalSpan;
        gd.widthHint = 0;
        emptyLabel.setLayoutData(gd);
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

        if (!activated) {
            setImageDescriptor(isSqlServer ? MSSQL_LOGO_IMG : SYBASE_LOGO_IMG);
        }

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText(SQLServerConstants.DEFAULT_HOST);
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
            if (CommonUtils.isEmpty(databaseName) && getSite().isNew()) {
                databaseName = SQLServerConstants.DEFAULT_DATABASE;
            }
            dbText.setText(databaseName);
        }
        if (userNameText != null) {
            userNameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
        }
        if (passwordText != null) {
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
        }
        if (windowsAuthenticationButton != null) {
            String winAuthProperty = connectionInfo.getProviderProperty(SQLServerConstants.PROP_CONNECTION_WINDOWS_AUTH);
            if (winAuthProperty != null) {
                windowsAuthenticationButton.setSelection(Boolean.parseBoolean(winAuthProperty));
            }
            enableTexts();
        }
        showAllSchemas.setSelection(CommonUtils.toBoolean(connectionInfo.getProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS)));

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
        if (userNameText != null) {
            connectionInfo.setUserName(userNameText.getText().trim());
        }
        if (passwordText != null) {
            connectionInfo.setUserPassword(passwordText.getText());
        }
        if (windowsAuthenticationButton != null) {
            connectionInfo.setProviderProperty(SQLServerConstants.PROP_CONNECTION_WINDOWS_AUTH,
                    String.valueOf(windowsAuthenticationButton.getSelection()));
        }
        if (showAllSchemas != null) {
            connectionInfo.setProviderProperty(SQLServerConstants.PROP_SHOW_ALL_SCHEMAS,
                String.valueOf(showAllSchemas.getSelection()));
        }

        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getSubPages()
    {
        return new IDialogPage[] {
                new DriverPropertiesDialogPage(this)
        };
    }

}
