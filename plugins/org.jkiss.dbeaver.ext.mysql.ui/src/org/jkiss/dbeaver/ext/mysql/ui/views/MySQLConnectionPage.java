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
package org.jkiss.dbeaver.ext.mysql.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
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

    private Text hostText;
    private Text portText;
    private Text dbText;
    private ClientHomesSelector homesSelector;
    private boolean activated = false;
    private boolean needsPort = true;

    private Combo serverTimezoneCombo;

    private final Image LOGO_MYSQL;
    private final Image LOGO_MARIADB;
    private final Image LOGO_GCLOUD;

    public MySQLConnectionPage() {
        LOGO_MYSQL = createImage("icons/mysql_logo.png");
        LOGO_MARIADB = createImage("icons/mariadb_logo.png");
        LOGO_GCLOUD = createImage("icons/google_cloud_sql_logo.png");
    }

    @Override
    public void dispose()
    {
        super.dispose();
        UIUtils.dispose(LOGO_MYSQL);
        UIUtils.dispose(LOGO_MARIADB);
        UIUtils.dispose(LOGO_GCLOUD);
    }

    @Override
    public Image getImage() {
        // We set image only once at activation
        // There is a bug in Eclipse which leads to SWTException after wizard image change
    	String id = getSite().getDriver().getId();
        if (id.equalsIgnoreCase(MySQLConstants.DRIVER_ID_MARIA_DB)) {
            return LOGO_MARIADB;
        } else if (id.equalsIgnoreCase(MySQLConstants.DRIVER_ID_GCLOUD_MYSQL8)
                || id.equalsIgnoreCase(MySQLConstants.DRIVER_ID_GCLOUD_MYSQL5)) {
            return LOGO_GCLOUD;
        } else {
            return LOGO_MYSQL;
        }
    }

    @Override
    public void createControl(Composite composite)
    {
        //Composite group = new Composite(composite, SWT.NONE);
        //group.setLayout(new GridLayout(1, true));
        ModifyListener textListener = e -> {
            if (activated) {
                site.updateButtons();
            }
        };
        final int fontHeight = UIUtils.getFontHeight(composite);
        DBPDriver driver = getSite().getDriver();
        boolean hostIsCloudInstance = CommonUtils.getBoolean(driver.getDriverParameter("hostIsCloudInstance"), false);
        needsPort = CommonUtils.getBoolean(driver.getDriverParameter("needsPort"), true);

        Composite addrGroup = new Composite(composite, SWT.NONE);
        addrGroup.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Group serverGroup = UIUtils.createControlGroup(addrGroup, "Server", 2, GridData.FILL_HORIZONTAL, 0);

        String hostOrCloudInstance;
        if (hostIsCloudInstance) {
            hostOrCloudInstance = MySQLUIMessages.dialog_connection_cloud_instance;
        } else {
            hostOrCloudInstance = MySQLUIMessages.dialog_connection_host;
        }
        UIUtils.createControlLabel(serverGroup, hostOrCloudInstance);
        Composite hostComposite = UIUtils.createComposite(serverGroup, needsPort ? 3 : 2);
        hostComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        hostText = new Text(hostComposite, SWT.BORDER);
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hostText.addModifyListener(textListener);
	    
        if (needsPort) {
            portText = UIUtils.createLabelText(hostComposite, MySQLUIMessages.dialog_connection_port, null, SWT.BORDER, new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            ((GridData)portText.getLayoutData()).widthHint = fontHeight * 10;
            portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
            portText.addModifyListener(textListener);
        }

        dbText = UIUtils.createLabelText(serverGroup, MySQLUIMessages.dialog_connection_database, null, SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
        dbText.addModifyListener(textListener);

        createAuthPanel(addrGroup, 1);

        Group advancedGroup = UIUtils.createControlGroup(addrGroup, "Advanced", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

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

        homesSelector = new ClientHomesSelector(advancedGroup, MySQLUIMessages.dialog_connection_local_client, false);
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
        homesSelector.getPanel().setLayoutData(gd);

        createDriverPanel(addrGroup);
        setControl(addrGroup);
    }

    @Override
    public boolean isComplete() {
        return super.isComplete() &&
            hostText != null && portText != null &&
            !CommonUtils.isEmpty(hostText.getText()) &&
            !(needsPort && CommonUtils.isEmpty(portText.getText()));
    }

    @Override
    public void loadSettings() {
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        DBPDriver driver = getSite().getDriver();

        super.loadSettings();

        // Load values from new connection info
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else if (driver.getDriverParameter("defaultHost") != null) {
                hostText.setText(driver.getDriverParameter("defaultHost").toString());
            } else {
                hostText.setText(MySQLConstants.DEFAULT_HOST);
            }
        }
        if (portText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                portText.setText(connectionInfo.getHostPort());
            } else if (driver.getDefaultPort() != null) {
                portText.setText(driver.getDefaultPort());
            } else {
                portText.setText("");
            }
        }

        if (dbText != null) {
            dbText.setText(CommonUtils.toString(connectionInfo.getDatabaseName(), CommonUtils.notEmpty(driver.getDefaultDatabase())));
        }
        if (serverTimezoneCombo != null) {
            String tzProp = connectionInfo.getProviderProperty(MySQLConstants.PROP_SERVER_TIMEZONE);
            if (CommonUtils.isEmpty(tzProp)) {
                serverTimezoneCombo.select(0);
            } else {
                serverTimezoneCombo.setText(tzProp);
            }
        }

        homesSelector.populateHomes(driver, connectionInfo.getClientHomeId(), site.isNew());

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

        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate)
    {
        return new IDialogPage[] {
            new DriverPropertiesDialogPage(this),

        };
    }

}
