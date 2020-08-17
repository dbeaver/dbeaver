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
package org.jkiss.dbeaver.ext.mysql.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.ui.MySQLUIActivator;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
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
public class MySQLConnectionPage extends ConnectionPageWithAuth implements ICompositeDialogPage
{
    // disable Server time zone manage - it confuses users and makes very little sense
    // as now we use server timestamp format by default
    private static final boolean MANAGE_SERVER_TIME_ZONE = true;

    private static final ImageDescriptor LOG_MYSQL = MySQLUIActivator.getImageDescriptor("icons/mysql_logo.png");
    private static final ImageDescriptor LOGO_MARIADB = MySQLUIActivator.getImageDescriptor("icons/mariadb_logo.png");

    private Text hostText;
    private Text portText;
    private Text dbText;
    private ClientHomesSelector homesSelector;
    private boolean activated = false;

    private Combo serverTimezoneCombo;


    @Override
    public void dispose()
    {
        super.dispose();
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

        Composite addrGroup = new Composite(composite, SWT.NONE);
        addrGroup.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Group serverGroup = UIUtils.createControlGroup(addrGroup, "Server", 2, GridData.FILL_HORIZONTAL, 0);

        Label hostLabel = UIUtils.createControlLabel(serverGroup, MySQLUIMessages.dialog_connection_host);
        Composite hostComposite = UIUtils.createComposite(serverGroup, 3);
        hostComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        hostText = new Text(hostComposite, SWT.BORDER);
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hostText.addModifyListener(textListener);

        portText = UIUtils.createLabelText(hostComposite, MySQLUIMessages.dialog_connection_port, null, SWT.BORDER, new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        ((GridData)portText.getLayoutData()).widthHint = fontHeight * 10;
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        portText.addModifyListener(textListener);

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
            !CommonUtils.isEmpty(portText.getText());
    }

    @Override
    public void loadSettings() {
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        DBPDriver driver = getSite().getDriver();

        super.loadSettings();

        {
            // We set image only once at activation
            // There is a bug in Eclipse which leads to SWTException after wizard image change
            if (driver.getId().equalsIgnoreCase(MySQLConstants.DRIVER_ID_MARIA_DB)) {
                setImageDescriptor(LOGO_MARIADB);
            } else {
                setImageDescriptor(LOG_MYSQL);
            }
        }

        // Load values from new connection info
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText(MySQLConstants.DEFAULT_HOST);
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

        homesSelector.populateHomes(site.getDriver(), connectionInfo.getClientHomeId(), site.isNew());

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
            if (serverTimezoneCombo.getSelectionIndex() == 0 || CommonUtils.isEmpty(serverTimezoneCombo.getText())) {
                connectionInfo.removeProviderProperty(MySQLConstants.PROP_SERVER_TIMEZONE);
            } else {
                connectionInfo.setProviderProperty(MySQLConstants.PROP_SERVER_TIMEZONE, serverTimezoneCombo.getText());
            }
        }
        if (homesSelector != null) {
            connectionInfo.setClientHomeId(homesSelector.getSelectedHome());
        }

        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getSubPages(boolean extrasOnly, boolean forceCreate)
    {
        return new IDialogPage[] {
            new DriverPropertiesDialogPage(this),

        };
    }

}
