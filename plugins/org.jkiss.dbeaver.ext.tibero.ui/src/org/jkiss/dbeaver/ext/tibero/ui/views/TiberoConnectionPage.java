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

package org.jkiss.dbeaver.ext.tibero.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class TiberoConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {

    private Text hostText;
    private Text portText;
    private Text databaseText;
    private Text urlText;
    private boolean activated;

    @Override
    public void createControl(Composite parent) {
        ModifyListener textListener = e -> {
            if (activated) {
                saveAndUpdate();
            }
        };

        Composite page = new Composite(parent, SWT.NONE);
        page.setLayout(new GridLayout(1, false));
        page.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite settingsGroup = UIUtils.createTitledComposite(page, "Tibero", 4, GridData.FILL_HORIZONTAL);
        SelectionAdapter modeSwitcher = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setupConnectionModeSelection(urlText, typeURLRadio.getSelection(), GROUP_CONNECTION_ARR);
                saveAndUpdate();
            }
        };
        createConnectionModeSwitcher(settingsGroup, modeSwitcher);

        Label urlLabel = UIUtils.createControlLabel(settingsGroup, "JDBC URL");
        urlText = new Text(settingsGroup, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        gd.grabExcessHorizontalSpace = true;
        gd.widthHint = 300;
        urlText.setLayoutData(gd);
        urlText.addModifyListener(e -> site.updateButtons());
        addControlToGroup(GROUP_URL, urlLabel, urlText);

        Label hostLabel = UIUtils.createControlLabel(settingsGroup, "Host");
        hostText = new Text(settingsGroup, SWT.BORDER);
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hostText.addModifyListener(textListener);

        Label portLabel = UIUtils.createControlLabel(settingsGroup, "Port");
        portText = new Text(settingsGroup, SWT.BORDER);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(portText) * 7;
        portText.setLayoutData(gd);
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        portText.addModifyListener(textListener);

        Label databaseLabel = UIUtils.createControlLabel(settingsGroup, "Database");
        databaseText = new Text(settingsGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        databaseText.setLayoutData(gd);
        databaseText.addModifyListener(textListener);

        addControlToGroup(GROUP_CONNECTION, hostLabel, hostText, portLabel, portText, databaseLabel, databaseText);

        createAuthPanel(page, 1);
        createDriverPanel(page);
        setControl(page);
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        DBPDataSourceContainer dataSource = site.getActiveDataSource();
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        boolean useURL = connectionInfo.getConfigurationType() == DBPDriverConfigurationType.URL;

        hostText.setText(CommonUtils.toString(
            connectionInfo.getHostName(),
            CommonUtils.toString(site.getDriver().getDefaultHost(), DBConstants.HOST_LOCALHOST)));
        portText.setText(CommonUtils.toString(connectionInfo.getHostPort(), site.getDriver().getDefaultPort()));
        databaseText.setText(CommonUtils.toString(connectionInfo.getDatabaseName(), site.getDriver().getDefaultDatabase()));

        if (CommonUtils.isEmpty(connectionInfo.getUrl())) {
            saveSettings(dataSource);
        }
        urlText.setText(CommonUtils.toString(connectionInfo.getUrl(), site.getDriver().getSampleURL()));

        setupConnectionModeSelection(urlText, useURL, GROUP_CONNECTION_ARR);
        activated = true;
        site.updateButtons();
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        connectionInfo.setConfigurationType(
            typeURLRadio != null && typeURLRadio.getSelection() ? DBPDriverConfigurationType.URL : DBPDriverConfigurationType.MANUAL);
        connectionInfo.setHostName(hostText.getText().trim());
        connectionInfo.setHostPort(portText.getText().trim());
        connectionInfo.setDatabaseName(databaseText.getText().trim());

        super.saveSettings(dataSource);

        if (isCustomURL()) {
            connectionInfo.setUrl(urlText.getText().trim());
        } else if (connectionInfo.getUrl() != null && !urlText.isDisposed()) {
            urlText.setText(connectionInfo.getUrl());
        }
    }

    @Override
    public boolean isComplete() {
        if (isCustomURL()) {
            return !CommonUtils.isEmptyTrimmed(urlText.getText()) && super.isComplete();
        }
        return !CommonUtils.isEmptyTrimmed(hostText.getText()) &&
            !CommonUtils.isEmptyTrimmed(portText.getText()) &&
            !CommonUtils.isEmptyTrimmed(databaseText.getText()) &&
            super.isComplete();
    }

    @Nullable
    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[] {
            new TiberoConnectionExtraPage(),
            new DriverPropertiesDialogPage(this)
        };
    }

    private void saveAndUpdate() {
        saveSettings(site.getActiveDataSource());
        site.updateButtons();
    }
}
