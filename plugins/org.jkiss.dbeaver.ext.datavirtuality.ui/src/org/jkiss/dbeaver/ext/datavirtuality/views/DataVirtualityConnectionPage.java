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
package org.jkiss.dbeaver.ext.datavirtuality.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.datavirtuality.DataVirtualityConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.ui.IDataSourceConnectionTester;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * DataVirtualityConnectionPage
 */
public class DataVirtualityConnectionPage extends ConnectionPageAbstract implements IDialogPageProvider, IDataSourceConnectionTester {

    private static final Log log = Log.getLog(DataVirtualityConnectionPage.class);

    private Text hostText;
    private Text portText;
    private Button sslCheckbox;
    private Text dbText;
    private Text usernameText;

    private final Image logoImage;

    public DataVirtualityConnectionPage() {
        logoImage = createImage("icons/datavirtuality_logo.png"); //$NON-NLS-1$
    }


    @Override
    public void dispose() {
        super.dispose();
        UIUtils.dispose(logoImage);
    }
    @Override
    public Image getImage() {
        return logoImage;
    }

    @Override
    public void createControl(Composite composite) {

        Composite control = new Composite(composite, SWT.NONE);
        control.setLayout(new GridLayout(1, false));
        control.setLayoutData(new GridData(GridData.FILL_BOTH));
        ModifyListener textListener = e -> site.updateButtons();

        {
            Composite addrGroup = UIUtils.createControlGroup(control, DataVirtualityMessages.label_connection, 4, 0, 0);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            addrGroup.setLayoutData(gd);

            UIUtils.createControlLabel(addrGroup, DataVirtualityMessages.label_host);

            hostText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);
            hostText.addModifyListener(textListener);

            UIUtils.createControlLabel(addrGroup, DataVirtualityMessages.label_port);

            portText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(portText) * 7;
            portText.setLayoutData(gd);
            portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
            portText.addModifyListener(textListener);

            UIUtils.createControlLabel(addrGroup, DataVirtualityMessages.label_database);

            dbText = new Text(addrGroup, SWT.BORDER | SWT.DROP_DOWN);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalSpan = 3;
            dbText.setLayoutData(gd);
            dbText.addModifyListener(textListener);

            UIUtils.createControlLabel(addrGroup, DataVirtualityMessages.label_ssl);

            sslCheckbox = new Button(addrGroup, SWT.CHECK);
            gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            sslCheckbox.setLayoutData(gd);
        }

        {
            Composite addrGroup = UIUtils.createControlGroup(control, DataVirtualityMessages.label_security, 4, 0, 0);
            addrGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(addrGroup, DataVirtualityMessages.label_user);

            usernameText = new Text(addrGroup, SWT.BORDER);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
            usernameText.setLayoutData(gd);
            usernameText.addModifyListener(textListener);

            UIUtils.createEmptyLabel(addrGroup, 2, 1);

            Text passwordText = createPasswordText(addrGroup, DataVirtualityMessages.label_password);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
            passwordText.setLayoutData(gd);
            passwordText.addModifyListener(textListener);

            createPasswordControls(addrGroup, 2);
        }

        createDriverPanel(control);
        setControl(control);
    }

    @Override
    public boolean isComplete() {
        return hostText != null &&
                !CommonUtils.isEmpty(hostText.getText());
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (hostText != null) {
            if (CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(DataVirtualityConstants.DEFAULT_HOST_PREFIX);
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
                portText.setText(""); //$NON-NLS-1$
            }
        }
        if (dbText != null) {
            String databaseName = connectionInfo.getDatabaseName();
            if (CommonUtils.isEmpty(databaseName)) {
                databaseName = DataVirtualityConstants.DEFAULT_DB_NAME;
            }
            dbText.setText(databaseName);
        }
        if (sslCheckbox != null) {
            sslCheckbox.setSelection(CommonUtils.notEmpty(connectionInfo.getProviderProperty(DataVirtualityConstants.PROP_SSL)).equals("mms") ? true : false);
        }
        if (usernameText != null) {
            usernameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
        }
        if (passwordText != null) {
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
        }
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
        if (usernameText != null) {
            connectionInfo.setUserName(usernameText.getText().trim());
        }

        if (sslCheckbox != null) {
            connectionInfo.setProviderProperty(DataVirtualityConstants.PROP_SSL, sslCheckbox.getSelection()  ? "mms" : "mm");
        }
        if (passwordText != null) {
            connectionInfo.setUserPassword(passwordText.getText());
        }
        super.saveSettings(dataSource);
    }

    @Override
    public void testConnection(DBCSession session) {

        try {
            session.getProgressMonitor().subTask("Execute 'SELECT 1'"); //$NON-NLS-1$
            try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, "SELECT 1", false, false, false)) {
                dbStat.executeStatement();
                try (DBCResultSet dbResult = dbStat.openResultSet()) {
                    while (dbResult.nextRow()) {
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate)
    {
        return new IDialogPage[] {
                new DriverPropertiesDialogPage(this)
        };
    }

}
