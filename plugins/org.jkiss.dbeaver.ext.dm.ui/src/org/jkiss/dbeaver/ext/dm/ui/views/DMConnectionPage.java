/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.dm.ui.views;

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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * @author Shengkai Bai
 */
public class DMConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {

    private Text urlText;

    private Text hostText;

    private Text portText;

    private boolean activated = false;

    @Override
    public Image getImage() {
        return createImage("icons/dm_logo.png");
    }

    @Override
    public void createControl(Composite parent) {

        final ModifyListener listener = e -> {
            if (activated) {
                updateUrl();
                site.updateButtons();
            }
        };

        Composite mainGroup = new Composite(parent, SWT.NONE);
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

        Label hostLabel = UIUtils.createControlLabel(
                addrGroup,
                UIConnectionMessages.dialog_connection_host_label);

        addControlToGroup(GROUP_CONNECTION, hostLabel);

        hostText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.GRAB_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        hostText.setLayoutData(gd);
        addControlToGroup(GROUP_CONNECTION, hostText);

        Label portLabel = UIUtils.createControlLabel(addrGroup,
                UIConnectionMessages.dialog_connection_network_socket_label_port);
        addControlToGroup(GROUP_CONNECTION, portLabel);
        portText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(portText) * 7;
        portText.setLayoutData(gd);
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        addControlToGroup(GROUP_CONNECTION, portText);

        hostText.addModifyListener(listener);
        portText.addModifyListener(listener);

        createAuthPanel(mainGroup, 1);

        createDriverPanel(mainGroup);
        setControl(mainGroup);
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
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[]{
                new DriverPropertiesDialogPage(this)
        };
    }

    @Override
    public boolean isComplete() {
        if (isCustomURL()) {
            return !CommonUtils.isEmpty(urlText.getText());
        }
        return super.isComplete() &&
                hostText != null && portText != null &&
                !CommonUtils.isEmpty(hostText.getText()) &&
                !CommonUtils.isEmpty(portText.getText());
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();

        if (typeURLRadio != null) {
            connectionInfo.setConfigurationType(
                    typeURLRadio.getSelection() ? DBPDriverConfigurationType.URL : DBPDriverConfigurationType.MANUAL
            );
        }

        if (hostText != null) {
            connectionInfo.setHostName(hostText.getText().trim());
        }

        if (portText != null) {
            connectionInfo.setHostPort(portText.getText().trim());
        }
        if (typeURLRadio != null && typeURLRadio.getSelection()) {
            connectionInfo.setUrl(urlText.getText());
        }
        super.saveSettings(dataSource);
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
                        CommonUtils.toString(site.getDriver().getDefaultHost(), "localhost"));
            }
        }
        if (portText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                portText.setText(connectionInfo.getHostPort());
            } else if (getSite().isNew()) {
                portText.setText(CommonUtils.notEmpty(driver.getDefaultPort()));
            }
        }
        final boolean useURL = connectionInfo.getConfigurationType() == DBPDriverConfigurationType.URL;
        if (useURL) {
            urlText.setText(connectionInfo.getUrl());
        }
        setupConnectionModeSelection(urlText, useURL, GROUP_CONNECTION_ARR);
        updateUrl();
        activated = true;
    }
}
