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
package org.jkiss.dbeaver.ext.kingbase.ui;

import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.KingbaseMessages;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.ext.kingbase.model.impls.KingbaseServerType;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseConnectionPage
 */
public class KingbaseConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {
    
    private Text urlText;
    private Text hostText;
    private Text portText;
    private Text dbText;
    private Button showNonDefault;
    private Text roleText; 
    private ClientHomesSelector homesSelector;
    private boolean activated = false;

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public Image getImage() {
        final DBPDriver driver = site.getDriver();

        DBPImage logoImage = driver.getLogoImage();
        if (logoImage != null) {
            return DBeaverIcons.getImage(logoImage);
        }
        KingbaseServerType serverType = getServerType(driver);
        return DBeaverIcons.getImage(serverType.getIcon());
    }

    @Override
    public void createControl(Composite composite) {
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
        KingbaseServerType serverType = getServerType(driver);

        Label hostLabel = UIUtils.createControlLabel(
            addrGroup,
            serverType.isCloudServer()
                ? KingbaseMessages.dialog_setting_connection_cloud_instance
                : KingbaseMessages.dialog_setting_connection_host
        );
        addControlToGroup(GROUP_CONNECTION, hostLabel);
        hostText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(textListener);
        addControlToGroup(GROUP_CONNECTION, hostText);

        if (serverType.needsPort()) {
            Label portLabel = UIUtils.createControlLabel(addrGroup, KingbaseMessages.dialog_setting_connection_port);
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

        Label dbLabel = UIUtils.createControlLabel(addrGroup, KingbaseMessages.dialog_setting_connection_database);
        addControlToGroup(GROUP_CONNECTION, dbLabel);
        dbText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        dbText.setLayoutData(gd);
        dbText.addModifyListener(textListener);
        dbText.setMessage(KingbaseMessages.dialog_database_name_hint);
        addControlToGroup(GROUP_CONNECTION, dbText);

        showNonDefault = UIUtils.createCheckbox(
            addrGroup,
            KingbaseMessages.dialog_setting_connection_nondefaultDatabase,
            KingbaseMessages.dialog_setting_connection_nondefaultDatabase_tip,
            false,
            2);
        addControlToGroup(GROUP_CONNECTION, showNonDefault);

        createAuthPanel(mainGroup, 1);

        createDriverPanel(mainGroup);
        setControl(mainGroup);
    }

    /**
     * Returns server type for correct classes initialization
     *
     * @param driver to read server type from custom properties
     */
    public KingbaseServerType getServerType(DBPDriver driver) {
        return KingbaseUtils.getServerType(driver);
    }

    protected boolean isSessionRoleSupported() {
        return false;
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
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText(
                    CommonUtils.toString(site.getDriver().getDefaultHost(), KingbaseConstants.DEFAULT_HOST));
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
                        databaseName = KingbaseConstants.DEFAULT_DATABASE;
                    }
                } else {
                    databaseName = "";
                }
            }
            dbText.setText(databaseName);
        }
        if (showNonDefault != null) {
            showNonDefault.setSelection(CommonUtils.getBoolean(
                connectionInfo.getProviderProperty(KingbaseConstants.PROP_SHOW_NON_DEFAULT_DB),
                DBWorkbench.getPlatform().getPreferenceStore().getBoolean(KingbaseConstants.PROP_SHOW_NON_DEFAULT_DB)));
        }
        if (roleText != null) {
            roleText.setText(CommonUtils.notEmpty(connectionInfo.getProviderProperty(KingbaseConstants.PROP_CHOSEN_ROLE)));
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
        if (showNonDefault != null) {
            connectionInfo.setProviderProperty(KingbaseConstants.PROP_SHOW_NON_DEFAULT_DB, String.valueOf(showNonDefault.getSelection()));
        }
        if (roleText != null) {
            connectionInfo.setProviderProperty(KingbaseConstants.PROP_CHOSEN_ROLE, roleText.getText().trim());
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
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[] {
            new KingbaseConnectionPageAdvanced(),
            new DriverPropertiesDialogPage(this)
        };
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
