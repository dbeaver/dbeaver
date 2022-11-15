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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerType;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
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

import java.util.List;
import java.util.Locale;

/**
 * PostgreConnectionPage
 */
public class PostgreConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {
    
    protected static final String GROUP_CONNECTION = "connection"; //$NON-NLS-1$
    protected static final List<String> GROUP_CONNECTION_ARR = List.of(GROUP_CONNECTION);
    
    private Text urlText;
    private Label hostLabel;
    private Text hostText;
    private Label portLabel;
    private Text portText;
    private Label dbLabel;
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
        final ModifyListener textListener = e -> {
            if (activated) {
                updateUrl();
                site.updateButtons();
            }
        };

        Composite mainGroup = new Composite(composite, SWT.NONE);
        mainGroup.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        mainGroup.setLayoutData(gd);

        Group addrGroup = UIUtils.createControlGroup(
            mainGroup,
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

        UIUtils.createControlLabel(addrGroup, UIConnectionMessages.dialog_connection_url_label);
        urlText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        gd.grabExcessHorizontalSpace = true;
        gd.widthHint = 355;
        urlText.setLayoutData(gd);
        urlText.addModifyListener(e -> site.updateButtons());

        final DBPDriver driver = site.getDriver();
        PostgreServerType serverType = PostgreUtils.getServerType(driver);

        hostLabel = UIUtils.createControlLabel(
            addrGroup,
            serverType.isCloudServer()
                ? PostgreMessages.dialog_setting_connection_cloud_instance
                : PostgreMessages.dialog_setting_connection_host
        );
        addControlToGroup(GROUP_CONNECTION, hostLabel);
        hostText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(textListener);
        addControlToGroup(GROUP_CONNECTION, hostText);

        if (serverType.needsPort()) {
            portLabel = UIUtils.createControlLabel(addrGroup, PostgreMessages.dialog_setting_connection_port);
            addControlToGroup(GROUP_CONNECTION, portLabel);
            portText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(portText) * 7;
            portText.setLayoutData(gd);
            portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
            portText.addModifyListener(textListener);
            addControlToGroup(GROUP_CONNECTION, portText);
        } else {
            gd.horizontalSpan = 3;
        }

        dbLabel = UIUtils.createControlLabel(addrGroup, PostgreMessages.dialog_setting_connection_database);
        addControlToGroup(GROUP_CONNECTION, dbLabel);
        dbText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 3;
        dbText.setLayoutData(gd);
        dbText.addModifyListener(textListener);
        dbText.setMessage(PostgreMessages.dialog_database_name_hint);
        addControlToGroup(GROUP_CONNECTION, dbText);

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

            if (DBWorkbench.hasFeature(DBConnectionConstants.PRODUCT_FEATURE_ADVANCED_DATABASE_ADMINISTRATION) && serverType.supportsClient()) {
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
        if (isCustomURL()) {
            return !CommonUtils.isEmpty(urlText.getText());
        } else {
            return super.isComplete() &&
                hostText != null &&
                !CommonUtils.isEmpty(hostText.getText()) &&
                (portText == null || !CommonUtils.isEmpty(portText.getText()));
        }
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
                hostText.setText(
                    CommonUtils.toString(site.getDriver().getDefaultHost(), PostgreConstants.DEFAULT_HOST));
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
        
        final boolean useURL = connectionInfo.getConfigurationType() == DBPDriverConfigurationType.URL;
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
        if (roleText != null) {
            connectionInfo.setProviderProperty(PostgreConstants.PROP_CHOSEN_ROLE, roleText.getText().trim());
        }
        if (homesSelector != null) {
            connectionInfo.setClientHomeId(homesSelector.getSelectedHome());
        }
        if (typeURLRadio.getSelection()) {
            connectionInfo.setUrl(urlText.getText());
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
    
    @Override
    protected boolean isCustomURL() {
        return typeURLRadio != null && typeURLRadio.getSelection();
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
