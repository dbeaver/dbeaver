/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mssql.ui;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mssql.SQLServerActivator;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLServerConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage
{
    private Text hostText;
    private Text portText;
    private Text dbText;
    private Label userNameLabel;
    private Text userNameText;
    private Label passwordLabel;
    private Text passwordText;
    private Button windowsAuthetticationButton;

    private Composite settingsGroup;

    private Map<String, List<Control>> propGroupMap = new HashMap<>();

    private static final String GROUP_HOST = "host"; //$NON-NLS-1$
    private static final String GROUP_DB = "db"; //$NON-NLS-1$
    private static final String GROUP_LOGIN = "login"; //$NON-NLS-1$
    private boolean activated;

    private static ImageDescriptor LOGO_IMG = SQLServerActivator.getImageDescriptor("icons/mssql_logo.png");


    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite)
    {
        settingsGroup = new Composite(composite, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        settingsGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        settingsGroup.setLayoutData(gd);

        {
            Label hostLabel = new Label(settingsGroup, SWT.NONE);
            hostLabel.setText(SQLServerMessages.dialog_connection_host_label);
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            hostText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);

            Label portLabel = new Label(settingsGroup, SWT.NONE);
            portLabel.setText(SQLServerMessages.dialog_connection_port_label);
            portLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            portText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.CENTER);
            gd.widthHint = 60;
            portText.setLayoutData(gd);

            addControlToGroup(GROUP_HOST, hostLabel);
            addControlToGroup(GROUP_HOST, hostText);
            addControlToGroup(GROUP_HOST, portLabel);
            addControlToGroup(GROUP_HOST, portText);
        }

        {
            Label dbLabel = new Label(settingsGroup, SWT.NONE);
            dbLabel.setText(SQLServerMessages.dialog_connection_database_schema_label);
            dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            dbText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            //gd.widthHint = 270;
            //gd.horizontalSpan = 3;
            dbText.setLayoutData(gd);

            Control emptyLabel = createEmptyLabel(settingsGroup, 1);

            addControlToGroup(GROUP_DB, dbLabel);
            addControlToGroup(GROUP_DB, dbText);
            addControlToGroup(GROUP_DB, emptyLabel);
        }

        {
            windowsAuthetticationButton = UIUtils.createLabelCheckbox(settingsGroup, SQLServerMessages.dialog_connection_windows_authentication_button, false);
            addControlToGroup(GROUP_DB, windowsAuthetticationButton);

            Control emptyLabel = new Label(settingsGroup, SWT.NONE);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            gd.horizontalSpan = 2;
            gd.widthHint = 0;
            emptyLabel.setLayoutData(gd);

            userNameLabel = new Label(settingsGroup, SWT.NONE);
            userNameLabel.setText(SQLServerMessages.dialog_connection_user_name_label);
            userNameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            userNameText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            userNameText.setLayoutData(gd);

            emptyLabel = createEmptyLabel(settingsGroup, 2);

            passwordLabel = new Label(settingsGroup, SWT.NONE);
            passwordLabel.setText(SQLServerMessages.dialog_connection_password_label);
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = new Text(settingsGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            passwordText.setLayoutData(gd);

            windowsAuthetticationButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    enableTexts();
                }
            });

            addControlToGroup(GROUP_LOGIN, userNameLabel);
            addControlToGroup(GROUP_LOGIN, userNameText);
            addControlToGroup(GROUP_LOGIN, emptyLabel);
            addControlToGroup(GROUP_LOGIN, passwordLabel);
            addControlToGroup(GROUP_LOGIN, passwordText);
        }

        createDriverPanel(settingsGroup);
        setControl(settingsGroup);
    }

    private void enableTexts() {
        boolean selection = windowsAuthetticationButton.getSelection();
        userNameLabel.setEnabled(!selection);
        userNameText.setEnabled(!selection);
        passwordLabel.setEnabled(!selection);
        passwordText.setEnabled(!selection);
    }

    private void addControlToGroup(String group, Control control)
    {
        List<Control> controlList = propGroupMap.get(group);
        if (controlList == null) {
            controlList = new ArrayList<>();
            propGroupMap.put(group, controlList);
        }
        controlList.add(control);
    }

    private Control createEmptyLabel(Composite parent, int verticalSpan)
    {
        Label emptyLabel = new Label(parent, SWT.NONE);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        gd.horizontalSpan = 2;
        gd.verticalSpan = verticalSpan;
        gd.widthHint = 0;
        emptyLabel.setLayoutData(gd);
        return emptyLabel;
    }

    @Override
    public boolean isComplete()
    {
        return hostText != null && portText != null &&
                !CommonUtils.isEmpty(hostText.getText()) &&
                !CommonUtils.isEmpty(portText.getText());
    }

    @Override
    public void loadSettings()
    {
        super.loadSettings();

        if (!activated) {
            setImageDescriptor(LOGO_IMG);
        }

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText(SQLServerConstants.DEFAULT_HOST);
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
        if (dbText != null) {
            String databaseName = connectionInfo.getDatabaseName();
            if (CommonUtils.isEmpty(databaseName) && getSite().isNew()) {
                databaseName = SQLServerConstants.DEFAULT_DATABASE;
            }
            dbText.setText(databaseName);
        }
        if (userNameText != null) {
            userNameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
        }
        if (passwordText != null) {
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
        }
        if (windowsAuthetticationButton != null) {
            String winAuthProperty = connectionInfo.getProviderProperty(SQLServerConstants.PROP_CONNECTION_WINDOWS_AUTH);
            if (winAuthProperty != null) {
                windowsAuthetticationButton.setSelection(Boolean.parseBoolean(winAuthProperty));
            }
            enableTexts();
        }

        activated = true;
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
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
        if (userNameText != null) {
            connectionInfo.setUserName(userNameText.getText().trim());
        }
        if (passwordText != null) {
            connectionInfo.setUserPassword(passwordText.getText());
        }
        if (windowsAuthetticationButton != null) {
            connectionInfo.setProviderProperty(SQLServerConstants.PROP_CONNECTION_WINDOWS_AUTH,
                    String.valueOf(windowsAuthetticationButton.getSelection()));
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
