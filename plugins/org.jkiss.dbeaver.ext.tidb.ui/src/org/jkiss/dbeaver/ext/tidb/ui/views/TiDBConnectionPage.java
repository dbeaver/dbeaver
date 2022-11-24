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

package org.jkiss.dbeaver.ext.tidb.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.tidb.ui.internal.Activator;
import org.jkiss.dbeaver.ext.tidb.ui.internal.TiDBMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * TiDBConnectionPage
 */
public class TiDBConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {
    private static final Log log = Log.getLog(TiDBConnectionPage.class);
    private static final ImageDescriptor logoImage = Activator.getImageDescriptor("icons/tidb_logo.png");

    private Text portText;
    private Text hostText;
    private Text databaseText;

    @Override
    public void createControl(Composite composite) {
        // Composite group = new Composite(composite, SWT.NONE);
        // group.setLayout(new GridLayout(1, true));
        setImageDescriptor(logoImage);

        ModifyListener textListener = e -> evaluateURL();

        Composite addrGroup = new Composite(composite, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        {
            Group hostGroup = UIUtils.createControlGroup(addrGroup,
                    TiDBMessages.tidb_connection_page_label_connection, 4, GridData.FILL_HORIZONTAL, 0);

            Label hostLabel = UIUtils.createControlLabel(hostGroup,
                    TiDBMessages.tidb_connection_page_label_host);
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            hostText = new Text(hostGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);
            hostText.addModifyListener(textListener);

            Label portLabel = UIUtils.createControlLabel(hostGroup,
                    TiDBMessages.tidb_connection_page_label_port);
            portLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            portText = new Text(hostGroup, SWT.BORDER);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(portText) * 5;
            portText.setLayoutData(gd);
            portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
            portText.addModifyListener(textListener);

            Label databaseLabel = UIUtils.createControlLabel(hostGroup,
                    TiDBMessages.tidb_connection_page_label_database);
            databaseLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            databaseText = new Text(hostGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalSpan = 3;
            databaseText.setLayoutData(gd);
            databaseText.addModifyListener(textListener);
        }

        createAuthPanel(addrGroup, 1);
        createDriverPanel(addrGroup);
        setControl(addrGroup);
    }

    @Override
    public boolean isComplete() {
        return super.isComplete() && hostText != null && databaseText != null && portText != null &&
                !CommonUtils.isEmpty(portText.getText()) && !CommonUtils.isEmpty(hostText.getText());
    }

    @Override
    public void loadSettings() {
        // Load values from new connection info
        DBPDataSourceContainer activeDataSource = site.getActiveDataSource();
        DBPConnectionConfiguration connectionInfo = activeDataSource.getConnectionConfiguration();
        if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
            hostText.setText(connectionInfo.getHostName());
        } else {
            hostText.setText(MySQLConstants.DEFAULT_HOST);
        }
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            portText.setText(connectionInfo.getHostPort());
        } else if (site.getDriver().getDefaultPort() != null) {
            portText.setText(site.getDriver().getDefaultPort());
        } else {
            portText.setText("");
        }
        if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            databaseText.setText(connectionInfo.getDatabaseName());
        }
        super.loadSettings();
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
        if (databaseText != null) {
            connectionInfo.setDatabaseName(databaseText.getText().trim());
        }
        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[] { new DriverPropertiesDialogPage(this) };
    }

    private void evaluateURL() {
        site.updateButtons();
    }

}
