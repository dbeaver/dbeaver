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
package org.jkiss.dbeaver.ext.mysql.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.registry.DBConnectionConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

import java.util.TimeZone;

/**
 * MySQLPageAdvanced
 */
public class MySQLConnectionPageAdvanced extends ConnectionPageAbstract {

    // disable Server time zone manage - it confuses users and makes very little sense
    // as now we use server timestamp format by default
    private static final boolean MANAGE_SERVER_TIME_ZONE = true;

    private ClientHomesSelector homesSelector;
    private Combo serverTimezoneCombo;

    public MySQLConnectionPageAdvanced() {
        setTitle("Advanced");
        setDescription("MySQL advanced connection setting");
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void createControl(Composite parent) {
        Composite cfgGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        cfgGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        cfgGroup.setLayoutData(gd);

        Composite advancedGroup = UIUtils.createTitledComposite(
            cfgGroup,
            MySQLUIMessages.dialog_connection_group_advanced,
            2,
            GridData.HORIZONTAL_ALIGN_BEGINNING);

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

        boolean supportsClients = CommonUtils.getBoolean(getSite().getDriver().getDriverParameter(MySQLConstants.DRIVER_PARAM_CLIENTS), true);
        if (DBWorkbench.hasFeature(DBConnectionConstants.PRODUCT_FEATURE_ADVANCED_DATABASE_ADMINISTRATION) && supportsClients) {
            homesSelector = new ClientHomesSelector(advancedGroup, MySQLUIMessages.dialog_connection_local_client, false);
            gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
            homesSelector.getPanel().setLayoutData(gd);
        }

        setControl(cfgGroup);

        loadSettings();
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void loadSettings() {
        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();

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
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionCfg = dataSource.getConnectionConfiguration();

        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();

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

        saveConnectionURL(connectionCfg);
    }

}
