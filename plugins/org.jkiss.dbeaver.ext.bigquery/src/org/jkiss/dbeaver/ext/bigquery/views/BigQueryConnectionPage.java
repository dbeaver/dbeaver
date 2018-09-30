/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.bigquery.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.bigquery.BigQueryActivator;
import org.jkiss.dbeaver.ext.bigquery.model.BigQueryConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * BigQueryConnectionPage
 */
public class BigQueryConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage
{
    private static final Log log = Log.getLog(BigQueryConnectionPage.class);

    private Text projectText;
    private Text extraProjectsText;
    private Text usernameText;
    private Combo authTypeCombo;
    private TextWithOpenFile authCertFile;

    private Text hostText;
    private Text portText;

    private static ImageDescriptor logoImage = BigQueryActivator.getImageDescriptor("icons/google_bigquery_logo.png");

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite)
    {
        setImageDescriptor(logoImage);

        Composite settingsGroup = new Composite(composite, SWT.NONE);
        settingsGroup.setLayout(new GridLayout(1, false));
        settingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        ModifyListener textListener = e -> site.updateButtons();

        {
            Composite addrGroup = UIUtils.createControlGroup(settingsGroup, "Connection", 2, 0, 0);
            addrGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            projectText = UIUtils.createLabelText(addrGroup, "Project", "");
            projectText.addModifyListener(textListener);

            extraProjectsText = UIUtils.createLabelText(addrGroup, "Additional project(s)", "");
            extraProjectsText.setToolTipText("Coma-separated list of projects (optional)");
            extraProjectsText.addModifyListener(textListener);
        }

        {
            Composite addrGroup = UIUtils.createControlGroup(settingsGroup, "Security", 4, 0, 0);
            addrGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            usernameText = UIUtils.createLabelText(addrGroup, "Service account", "");
            usernameText.addModifyListener(textListener);

            UIUtils.createControlLabel(addrGroup, "OAuth type");
            authTypeCombo = new Combo(addrGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            authTypeCombo.add("Service-based");
            authTypeCombo.add("User-based");
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            authTypeCombo.setLayoutData(gd);
            authTypeCombo.addModifyListener(textListener);
            authTypeCombo.select(0);

            UIUtils.createControlLabel(addrGroup, "Key path");
            authCertFile = new TextWithOpenFile(addrGroup, "Private key path (p12 or JSON)", new String[] { "*", "*.p12", "*.json" } );
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 3;
            authCertFile.setLayoutData(gd);
        }


        {
            // Def host/port
            Composite addrGroup = UIUtils.createControlGroup(settingsGroup, "Server info", 4, 0, 0);
            addrGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            hostText = UIUtils.createLabelText(addrGroup, "Host", BigQueryConstants.DEFAULT_HOST_NAME);
            hostText.addModifyListener(textListener);

            portText = UIUtils.createLabelText(addrGroup, "Port", String.valueOf(BigQueryConstants.DEFAULT_PORT));
            GridData gd = (GridData) portText.getLayoutData();
            gd.widthHint = 40;
            portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
            portText.addModifyListener(textListener);
        }

        createDriverPanel(settingsGroup);
        setControl(settingsGroup);
    }

    @Override
    public boolean isComplete()
    {
        return hostText != null && !CommonUtils.isEmpty(hostText.getText()) &&
            portText != null && !CommonUtils.isEmpty(portText.getText()) &&
            projectText != null && !CommonUtils.isEmpty(projectText.getText()) &&
            usernameText != null && !CommonUtils.isEmpty(usernameText.getText());
    }

    @Override
    public void loadSettings()
    {
        super.loadSettings();

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (projectText != null) {
            String databaseName = connectionInfo.getDatabaseName();
            if (CommonUtils.isEmpty(databaseName)) {
                databaseName = "";
            }
            projectText.setText(databaseName);
        }
        if (usernameText != null) {
            usernameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
        }
        if (authTypeCombo != null) {
            authTypeCombo.select(CommonUtils.toInt(connectionInfo.getProviderProperty(BigQueryConstants.PROP_OAUTH_TYPE)));
        }

        if (hostText != null) {
            if (CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(BigQueryConstants.DEFAULT_HOST_NAME);
            } else {
                hostText.setText(connectionInfo.getHostName());
            }
        }
        if (portText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                portText.setText(String.valueOf(connectionInfo.getHostPort()));
            } else if (site.getDriver().getDefaultPort() != null) {
                portText.setText(site.getDriver().getDefaultPort());
            } else {
                portText.setText("");
            }
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (projectText != null) {
            connectionInfo.setDatabaseName(projectText.getText().trim());
        }
        if (usernameText != null) {
            connectionInfo.setUserName(usernameText.getText().trim());
        }
        if (authTypeCombo != null) {
            connectionInfo.setProviderProperty(BigQueryConstants.PROP_OAUTH_TYPE, String.valueOf(authTypeCombo.getSelectionIndex()));
        }
        if (hostText != null) {
            connectionInfo.setHostName(hostText.getText().trim());
        }
        if (portText != null) {
            connectionInfo.setHostPort(portText.getText().trim());
        }
        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getSubPages()
    {
        return new IDialogPage[] {
            new DriverPropertiesDialogPage(this)
        };
    }

}
