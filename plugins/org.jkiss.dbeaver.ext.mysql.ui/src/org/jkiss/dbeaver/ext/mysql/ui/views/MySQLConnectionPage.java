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
package org.jkiss.dbeaver.ext.mysql.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.registry.DBConnectionConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;
import java.util.TimeZone;

/**
 * MySQLConnectionPage
 */
public class MySQLConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider
{
    // disable Server time zone manage - it confuses users and makes very little sense
    // as now we use server timestamp format by default
    private static final boolean MANAGE_SERVER_TIME_ZONE = true;

    private Text urlText;
    private Text hostText;
    private Text portText;
    private Text dbText;
    private ClientHomesSelector homesSelector;
    private boolean activated = false;

    private Combo serverTimezoneCombo;

    private final Image LOGO_MYSQL;
    private final Image LOGO_MARIADB;
    private boolean needsPort;

    public MySQLConnectionPage() {
        LOGO_MYSQL = createImage("icons/mysql_logo.png");
        LOGO_MARIADB = createImage("icons/mariadb_logo.png");
    }

    @Override
    public void dispose()
    {
        super.dispose();
        UIUtils.dispose(LOGO_MYSQL);
        UIUtils.dispose(LOGO_MARIADB);
    }

    @Override
    public Image getImage() {
        // We set image only once at activation
        // There is a bug in Eclipse which leads to SWTException after wizard image change
        DBPDriver driver = getSite().getDriver();
        DBPImage logoImage = driver.getLogoImage();
        if (logoImage != null) {
            return DBeaverIcons.getImage(logoImage);
        }
        if (driver.getId().equalsIgnoreCase(MySQLConstants.DRIVER_ID_MARIA_DB)) {
            return LOGO_MARIADB;
        } else {
            return LOGO_MYSQL;
        }
    }

    @Override
    public void createControl(Composite composite) {
        ModifyListener textListener = e -> {
            if (activated) {
                updateUrl();
                site.updateButtons();
            }
        };

        Composite addrGroup = new Composite(composite, SWT.NONE);
        addrGroup.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Group serverGroup = UIUtils.createControlGroup(
            addrGroup,
            UIConnectionMessages.dialog_connection_server_label,
            4,
            GridData.FILL_HORIZONTAL,
            0);

        SelectionAdapter typeSwitcher = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setupConnectionModeSelection(urlText, typeURLRadio.getSelection(), GROUP_CONNECTION_ARR);
                updateUrl();
            }
        };
        createConnectionModeSwitcher(serverGroup, typeSwitcher);

        UIUtils.createControlLabel(serverGroup, UIConnectionMessages.dialog_connection_url_label);
        urlText = new Text(serverGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        gd.grabExcessHorizontalSpace = true;
        gd.widthHint = 355;
        urlText.setLayoutData(gd);
        urlText.addModifyListener(e -> site.updateButtons());

        DBPDriver driver = getSite().getDriver();
        needsPort = CommonUtils.getBoolean(driver.getDriverParameter("needsPort"), true);

        Label hostLabel = UIUtils.createControlLabel(serverGroup,
            needsPort ? MySQLUIMessages.dialog_connection_host : MySQLUIMessages.dialog_connection_instance);

        hostText = new Text(serverGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(textListener);
        addControlToGroup(GROUP_CONNECTION, hostLabel, hostText);

        if (needsPort) {
            Label portLabel = UIUtils.createControlLabel(serverGroup, MySQLUIMessages.dialog_connection_port);
            portText = new Text(serverGroup, SWT.BORDER);
            gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(portText) * 10;
            portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
            portText.addModifyListener(textListener);
            addControlToGroup(GROUP_CONNECTION, portLabel, portText);
        } else {
            gd.horizontalSpan = 3;
        }

        Label dbLabel = UIUtils.createControlLabel(serverGroup, MySQLUIMessages.dialog_connection_database);
        dbText = new Text(serverGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 3;
        dbText.setLayoutData(gd);
        dbText.addModifyListener(textListener);
        addControlToGroup(GROUP_CONNECTION, dbLabel, dbText);

        createAuthPanel(addrGroup, 1);

        Group advancedGroup = UIUtils.createControlGroup(
            addrGroup,
            MySQLUIMessages.dialog_connection_group_advanced,
            2,
            GridData.HORIZONTAL_ALIGN_BEGINNING,
            0);

        if (MANAGE_SERVER_TIME_ZONE) {
            serverTimezoneCombo = UIUtils.createLabelCombo(advancedGroup, MySQLUIMessages.dialog_connection_server_timezone, SWT.DROP_DOWN);
            serverTimezoneCombo.add(MySQLUIMessages.dialog_connection_auto_detect);
            {
                String[] tzList = TimeZone.getAvailableIDs();
                for (String tzID : tzList) {
                    //TimeZone timeZone = TimeZone.getTimeZone(tzID);
                    serverTimezoneCombo.add(tzID);
                }
            }
            serverTimezoneCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        }

        boolean supportsClients = CommonUtils.getBoolean(driver.getDriverParameter(MySQLConstants.DRIVER_PARAM_CLIENTS), true);
        if (DBWorkbench.hasFeature(DBConnectionConstants.PRODUCT_FEATURE_ADVANCED_DATABASE_ADMINISTRATION) && supportsClients) {
            homesSelector = new ClientHomesSelector(advancedGroup, MySQLUIMessages.dialog_connection_local_client, false);
            gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
            homesSelector.getPanel().setLayoutData(gd);
        }

        createDriverPanel(addrGroup);
        setControl(addrGroup);
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

    @Override
    public boolean isComplete() {
        if (isCustomURL()) {
            return !CommonUtils.isEmpty(urlText.getText());
        }
        return super.isComplete() &&
            hostText != null &&
            !CommonUtils.isEmpty(hostText.getText()) &&
            (!needsPort || !CommonUtils.isEmpty(portText.getText()));
    }

    @Override
    public void loadSettings() {
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        super.loadSettings();

        // Load values from new connection info
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText(
                    CommonUtils.toString(site.getDriver().getDefaultHost(), MySQLConstants.DEFAULT_HOST));
            }
        }
        if (portText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                portText.setText(connectionInfo.getHostPort());
            } else if (site.getDriver().getDefaultPort() != null) {
                portText.setText(site.getDriver().getDefaultPort());
            } else {
                portText.setText("");
            }
        }
        if (dbText != null) {
            dbText.setText(CommonUtils.toString(connectionInfo.getDatabaseName(), CommonUtils.notEmpty(site.getDriver().getDefaultDatabase())));
        }
        if (serverTimezoneCombo != null) {
            String tzProp = connectionInfo.getProviderProperty(MySQLConstants.PROP_SERVER_TIMEZONE);
            if (CommonUtils.isEmpty(tzProp)) {
                serverTimezoneCombo.select(0);
            } else {
                serverTimezoneCombo.setText(tzProp);
            }
        }

        if (homesSelector != null) {
            homesSelector.populateHomes(site.getDriver(), connectionInfo.getClientHomeId(), site.isNew());
        }

        final boolean useURL = connectionInfo.getConfigurationType() == DBPDriverConfigurationType.URL;
        if (useURL) {
            urlText.setText(connectionInfo.getUrl());
        }
        setupConnectionModeSelection(urlText, useURL, GROUP_CONNECTION_ARR);
        updateUrl();

        activated = true;
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (typeURLRadio != null) {
            connectionInfo.setConfigurationType(
                typeURLRadio.getSelection() ? DBPDriverConfigurationType.URL : DBPDriverConfigurationType.MANUAL);
        }
        if (hostText != null) {
            connectionInfo.setHostName(hostText.getText().trim());
        }
        if (portText != null) {
            connectionInfo.setHostPort(portText.getText().trim());
        }
        if (dbText != null) {
            connectionInfo.setDatabaseName(dbText.getText().trim());
        }
        if (serverTimezoneCombo != null) {
            String serverTimeZone = serverTimezoneCombo.getText();
            if (CommonUtils.isEmpty(serverTimeZone) || serverTimeZone.equals(MySQLUIMessages.dialog_connection_auto_detect)) {
                connectionInfo.removeProviderProperty(MySQLConstants.PROP_SERVER_TIMEZONE);
            } else {
                connectionInfo.setProviderProperty(MySQLConstants.PROP_SERVER_TIMEZONE, serverTimeZone);
            }
        }
        if (homesSelector != null) {
            connectionInfo.setClientHomeId(homesSelector.getSelectedHome());
        }
        if (typeURLRadio != null && typeURLRadio.getSelection()) {
            connectionInfo.setUrl(urlText.getText());
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
