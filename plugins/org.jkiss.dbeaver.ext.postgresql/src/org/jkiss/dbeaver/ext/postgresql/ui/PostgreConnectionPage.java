/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.postgresql.PostgreActivator;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerType;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * PostgreConnectionPage
 */
public class PostgreConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage
{
    private Text hostText;
    private Text portText;
    private Text dbText;
    private Text usernameText;
    private Text passwordText;
    private ClientHomesSelector homesSelector;
    private Button showNonDefault;
    private Button showTemplates;
    private boolean activated = false;

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite)
    {
        //Composite group = new Composite(composite, SWT.NONE);
        //group.setLayout(new GridLayout(1, true));
        ModifyListener textListener = e -> {
            if (activated) {
                site.updateButtons();
            }
        };

        Composite addrGroup = UIUtils.createPlaceholder(composite, 4);
        GridLayout gl = new GridLayout(4, false);
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Label hostLabel = UIUtils.createControlLabel(addrGroup, PostgreMessages.dialog_setting_connection_host);
        hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        hostText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(textListener);

        Label portLabel = UIUtils.createControlLabel(addrGroup, PostgreMessages.dialog_setting_connection_port);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        portLabel.setLayoutData(gd);

        portText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = 40;
        portText.setLayoutData(gd);
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        portText.addModifyListener(textListener);

        Label dbLabel = UIUtils.createControlLabel(addrGroup, PostgreMessages.dialog_setting_connection_database);
        dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        dbText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 3;
        dbText.setLayoutData(gd);
        dbText.addModifyListener(textListener);

        Label usernameLabel = UIUtils.createControlLabel(addrGroup, PostgreMessages.dialog_setting_connection_user);
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        usernameText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        usernameText.setLayoutData(gd);
        usernameText.addModifyListener(textListener);

        UIUtils.createEmptyLabel(addrGroup, 2, 1);

        Label passwordLabel = UIUtils.createControlLabel(addrGroup, PostgreMessages.dialog_setting_connection_password);
        passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        passwordText = new Text(addrGroup, SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        passwordText.setLayoutData(gd);
        passwordText.addModifyListener(textListener);

        createSavePasswordButton(addrGroup, 2);

        {
            Composite buttonsGroup = new Composite(addrGroup, SWT.NONE);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            buttonsGroup.setLayoutData(gd);
            buttonsGroup.setLayout(new GridLayout(2, false));
            homesSelector = new ClientHomesSelector(buttonsGroup, SWT.NONE, PostgreMessages.dialog_setting_connection_localClient);
            gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
            homesSelector.getPanel().setLayoutData(gd);
        }

        {
            Group secureGroup = new Group(addrGroup, SWT.NONE);
            secureGroup.setText(PostgreMessages.dialog_setting_connection_settings);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 4;
            secureGroup.setLayoutData(gd);
            secureGroup.setLayout(new GridLayout(2, false));

            showNonDefault = UIUtils.createCheckbox(secureGroup, PostgreMessages.dialog_setting_connection_nondefaultDatabase, PostgreMessages.dialog_setting_connection_nondefaultDatabase_tip, false, 2);
            showNonDefault.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    showTemplates.setEnabled(showNonDefault.getSelection());
                }
            });
            showTemplates = UIUtils.createCheckbox(secureGroup, PostgreMessages.dialog_setting_connection_show_templates, PostgreMessages.dialog_setting_connection_show_templates_tip, false, 2);
        }

        createDriverPanel(addrGroup);
        setControl(addrGroup);
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

        final DBPDriver driver = site.getDriver();

        PostgreServerType serverType = PostgreUtils.getServerType(driver);
        {
            ImageDescriptor logo = DBeaverIcons.getImageDescriptor(serverType.getIcon());
            if (logo != null) {
                setImageDescriptor(logo);
            }
        }

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText(PostgreConstants.DEFAULT_HOST);
            }
        }
        if (portText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                portText.setText(String.valueOf(connectionInfo.getHostPort()));
            } else if (getSite().isNew()) {
                if (driver.getDefaultPort() != null) {
                    portText.setText(driver.getDefaultPort());
                } else {
                    portText.setText("");
                }
            }
        }
        if (dbText != null) {
            String databaseName = connectionInfo.getDatabaseName();
            if (CommonUtils.isEmpty(databaseName)) {
                if (getSite().isNew()) {
                    databaseName = serverType.getDefaultDatabase();
                    if (CommonUtils.isEmpty(databaseName)) {
                        databaseName = PostgreConstants.DEFAULT_DATABASE;
                    }
                } else {
                    databaseName = "";
                }
            }
            dbText.setText(databaseName);
        }
        if (usernameText != null) {
            String userName = CommonUtils.notEmpty(connectionInfo.getUserName());
            if (site.isNew() && CommonUtils.isEmpty(userName)) {
                userName = serverType.getDefaultUser();
                if (CommonUtils.isEmpty(userName)) {
                    userName = PostgreConstants.DEFAULT_DATABASE;
                }
            }
            usernameText.setText(userName);
        }
        if (passwordText != null) {
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
        }
        homesSelector.populateHomes(driver, connectionInfo.getClientHomeId(), site.isNew());

        showNonDefault.setSelection(CommonUtils.getBoolean(connectionInfo.getProviderProperty(PostgreConstants.PROP_SHOW_NON_DEFAULT_DB), true));
        showTemplates.setSelection(CommonUtils.getBoolean(connectionInfo.getProviderProperty(PostgreConstants.PROP_SHOW_TEMPLATES_DB), false));
        showTemplates.setEnabled(showNonDefault.getSelection());

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
        if (usernameText != null) {
            connectionInfo.setUserName(usernameText.getText().trim());
        }
        if (passwordText != null) {
            connectionInfo.setUserPassword(passwordText.getText());
        }
        if (homesSelector != null) {
            connectionInfo.setClientHomeId(homesSelector.getSelectedHome());
        }

        connectionInfo.setProviderProperty(PostgreConstants.PROP_SHOW_NON_DEFAULT_DB, String.valueOf(showNonDefault.getSelection()));
        connectionInfo.setProviderProperty(PostgreConstants.PROP_SHOW_TEMPLATES_DB, String.valueOf(showTemplates.getSelection()));
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
