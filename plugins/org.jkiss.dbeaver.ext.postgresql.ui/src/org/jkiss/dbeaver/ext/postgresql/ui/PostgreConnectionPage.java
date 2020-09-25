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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreExecutionContext;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerType;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.IDataSourceConnectionTester;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

/**
 * PostgreConnectionPage
 */
public class PostgreConnectionPage extends ConnectionPageWithAuth implements ICompositeDialogPage, IDataSourceConnectionTester {
    private static final Log log = Log.getLog(PostgreConnectionPage.class);

    private Text hostText;
    private Text portText;
    private Text dbText;
    private Combo roleCombo;
    private ClientHomesSelector homesSelector;
    private boolean activated = false;

    @Override
    public void dispose() {
        super.dispose();
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

        Composite mainGroup = new Composite(composite, SWT.NONE);
        mainGroup.setLayout(new GridLayout(1, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        mainGroup.setLayoutData(gd);

        Group addrGroup = UIUtils.createControlGroup(mainGroup, "Server", 4, GridData.FILL_HORIZONTAL, 0);

        hostText = UIUtils.createLabelText(addrGroup, PostgreMessages.dialog_setting_connection_host, null, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(textListener);

        portText = UIUtils.createLabelText(addrGroup, PostgreMessages.dialog_setting_connection_port, null, SWT.BORDER);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(portText) * 7;
        portText.setLayoutData(gd);
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        portText.addModifyListener(textListener);

        dbText = UIUtils.createLabelText(addrGroup, PostgreMessages.dialog_setting_connection_database, null, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 3;
        dbText.setLayoutData(gd);
        dbText.addModifyListener(textListener);

        createAuthPanel(mainGroup, 1);


        Group advancedGroup = UIUtils.createControlGroup(mainGroup, "Advanced", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

        roleCombo = UIUtils.createLabelCombo(advancedGroup, PostgreMessages.dialog_setting_use_role, SWT.DROP_DOWN);
        roleCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        homesSelector = new ClientHomesSelector(advancedGroup, PostgreMessages.dialog_setting_connection_localClient, false);
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
        homesSelector.getPanel().setLayoutData(gd);

        createDriverPanel(mainGroup);
        setControl(mainGroup);
    }

    @Override
    public boolean isComplete() {
        return hostText != null && portText != null && 
            !CommonUtils.isEmpty(hostText.getText()) &&
            !CommonUtils.isEmpty(portText.getText());
    }

    @Override
    public void loadSettings() {
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        final DBPDriver driver = site.getDriver();

        PostgreServerType serverType = PostgreUtils.getServerType(driver);

        super.loadSettings();

        setImageDescriptor(DBeaverIcons.getImageDescriptor(serverType.getIcon()));

        // Load values from new connection info
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText(PostgreConstants.DEFAULT_HOST);
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
        if (roleCombo != null) {
            roleCombo.setText(CommonUtils.notEmpty(connectionInfo.getProviderProperty(PostgreConstants.PROP_CHOSEN_ROLE)));
        }
        homesSelector.populateHomes(driver, connectionInfo.getClientHomeId(), site.isNew());

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
        if (roleCombo != null) {
            connectionInfo.setProviderProperty(PostgreConstants.PROP_CHOSEN_ROLE, roleCombo.getText().trim());
        }
        if (homesSelector != null) {
            connectionInfo.setClientHomeId(homesSelector.getSelectedHome());
        }

        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getSubPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[] {
            new PostgreConnectionPageAdvanced(),
            new DriverPropertiesDialogPage(this)
        };
    }

    @Override
    public void testConnection(final DBCSession session) {
        try {
            updateRolesCombo(session);
        } catch (DBCException e) {
            log.error(e);
        }
    }

    private void updateRolesCombo(final DBCSession session) throws DBCException {
        final Collection<String> result = new ArrayList<>();
        final String userName = ((PostgreExecutionContext)session.getExecutionContext()).getActiveUser();
        session.getProgressMonitor().subTask("Exec finding roles query");
        final String query = "WITH RECURSIVE cte AS (" +
                "   SELECT oid FROM pg_roles WHERE rolname = '" + userName + "'" +
                "   UNION ALL" +
                "   SELECT m.roleid" +
                "   FROM   cte" +
                "   JOIN   pg_auth_members m ON m.member = cte.oid" +
                "   )" +
                "SELECT oid::regrole::text AS rolenames FROM cte;";
        try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query, false, false, false)) {
            dbStat.executeStatement();
            try (DBCResultSet dbResult = dbStat.openResultSet()) {
                while (dbResult.nextRow()) {
                    result.add(CommonUtils.toString(dbResult.getAttributeValue("rolenames"))); //$NON-NLS-1$
                }
            }
        }
        UIUtils.asyncExec(() -> {
            String oldText = roleCombo.getText();
            if (!result.contains("")) {
                result.add("");
            }
            roleCombo.setItems(result.toArray(new String[0]));
            if (!result.contains(oldText)) {
                roleCombo.setText(userName);
            }
        });
    }
}
