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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerType;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * PostgreConnectionPage
 */
public class PostgreConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {
    private static final Log log = Log.getLog(PostgreConnectionPage.class);

    private Text hostText;
    private Text portText;
    private Text dbText;
    private Text roleText; //TODO: make it a combo and fill it with appropriate roles
    private ClientHomesSelector homesSelector;
    private boolean activated = false;

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public Image getImage() {
        final DBPDriver driver = site.getDriver();

        PostgreServerType serverType = PostgreUtils.getServerType(driver);
        return DBeaverIcons.getImage(serverType.getIcon());
    }

    @Override
    public void createControl(Composite composite) {
        //Composite group = new Composite(composite, SWT.NONE);
        //group.setLayout(new GridLayout(1, true));
        ModifyListener textListener = e -> {
            if (activated) {
                site.updateButtons();
            }
        };
        
        final DBPDriver driver = site.getDriver();
        PostgreServerType serverType = PostgreUtils.getServerType(driver);

        Composite mainGroup = new Composite(composite, SWT.NONE);
        mainGroup.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        mainGroup.setLayoutData(gd);

        Group addrGroup = UIUtils.createControlGroup(mainGroup, "Server", 4, GridData.FILL_HORIZONTAL, 0);

        hostText = UIUtils.createLabelText(
            addrGroup,
            serverType.isCloudServer() ? PostgreMessages.dialog_setting_connection_cloud_instance : PostgreMessages.dialog_setting_connection_host,
            null,
            SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(textListener);

        if (serverType.needsPort()) {
	        portText = UIUtils.createLabelText(addrGroup, PostgreMessages.dialog_setting_connection_port, null, SWT.BORDER);
	        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
	        gd.widthHint = UIUtils.getFontHeight(portText) * 7;
	        portText.setLayoutData(gd);
	        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
	        portText.addModifyListener(textListener);
        } else {
        	gd.horizontalSpan = 3;
        }

        dbText = UIUtils.createLabelText(addrGroup, PostgreMessages.dialog_setting_connection_database, null, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 3;
        dbText.setLayoutData(gd);
        dbText.addModifyListener(textListener);

        createAuthPanel(mainGroup, 1);

        if (isSessionRoleSupported() || serverType.supportsClient()) {
            Group advancedGroup = UIUtils.createControlGroup(mainGroup, "Advanced", 4, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            if (isSessionRoleSupported()) {
                roleText = UIUtils.createLabelText(advancedGroup, PostgreMessages.dialog_setting_session_role, null, SWT.BORDER);
                roleText.setToolTipText(PostgreMessages.dialog_setting_session_role_tip);
                gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.widthHint = UIUtils.getFontHeight(roleText) * 15;
                roleText.setLayoutData(gd);
            }

            if (!DBWorkbench.getPlatform().getApplication().hasProductFeature(DBPApplication.PRODUCT_FEATURE_SIMPLE_DATABASE_ADMINISTRATION) &&
                serverType.supportsClient())
            {
                homesSelector = new ClientHomesSelector(advancedGroup, PostgreMessages.dialog_setting_connection_localClient, false);
                gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
                homesSelector.getPanel().setLayoutData(gd);
            }
        }

        createDriverPanel(mainGroup);
        setControl(mainGroup);
    }

    protected boolean isSessionRoleSupported() {
        return true;
    }

    @Override
    public boolean isComplete() {
        return super.isComplete() &&
            hostText != null &&
            !CommonUtils.isEmpty(hostText.getText()) &&
            (portText == null || !CommonUtils.isEmpty(portText.getText()));
    }

    @Override
    public void loadSettings() {
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        final DBPDriver driver = site.getDriver();

        super.loadSettings();

        // Load values from new connection info
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                Object defaultHost = driver.getDriverParameter("defaultHost");
                hostText.setText(defaultHost != null ? defaultHost.toString() : PostgreConstants.DEFAULT_HOST);
            }
        }
        if (portText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                portText.setText(connectionInfo.getHostPort());
            } else if (getSite().isNew()) {
                portText.setText(CommonUtils.notEmpty(driver.getDefaultPort()));
            }
        }
        if (dbText != null) {
            String databaseName = connectionInfo.getDatabaseName();
            if (CommonUtils.isEmpty(databaseName)) {
                if (getSite().isNew()) {
                    databaseName = driver.getDefaultDatabase();
                    if (CommonUtils.isEmpty(databaseName)) {
                        databaseName = PostgreConstants.DEFAULT_DATABASE;
                    }
                } else {
                    databaseName = "";
                }
            }
            dbText.setText(databaseName);
        }
        if (roleText != null) {
            roleText.setText(CommonUtils.notEmpty(connectionInfo.getProviderProperty(PostgreConstants.PROP_CHOSEN_ROLE)));
        }
        if (homesSelector != null) {
            homesSelector.populateHomes(driver, connectionInfo.getClientHomeId(), site.isNew());
        }

        activated = true;
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
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
        if (roleText != null) {
            connectionInfo.setProviderProperty(PostgreConstants.PROP_CHOSEN_ROLE, roleText.getText().trim());
        }
        if (homesSelector != null) {
            connectionInfo.setClientHomeId(homesSelector.getSelectedHome());
        }

        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[] {
            new PostgreConnectionPageAdvanced(),
            new DriverPropertiesDialogPage(this)
        };
    }
}
