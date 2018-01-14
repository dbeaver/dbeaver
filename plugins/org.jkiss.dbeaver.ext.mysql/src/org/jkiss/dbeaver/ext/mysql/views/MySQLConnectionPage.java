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
package org.jkiss.dbeaver.ext.mysql.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.mysql.Activator;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * MySQLConnectionPage
 */
public class MySQLConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage
{
    private Text hostText;
    private Text portText;
    private Text dbText;
    private Text usernameText;
    private Text passwordText;
    private ClientHomesSelector homesSelector;
    private boolean activated = false;

    private static ImageDescriptor MYSQL_LOGO_IMG = Activator.getImageDescriptor("icons/mysql_logo.png");
    private static ImageDescriptor MARIADB_LOGO_IMG = Activator.getImageDescriptor("icons/mariadb_logo.png");


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
        ModifyListener textListener = new ModifyListener()
        {
            @Override
            public void modifyText(ModifyEvent e)
            {
                if (activated) {
                    site.updateButtons();
                }
            }
        };
        final int fontHeight = UIUtils.getFontHeight(composite);

        Composite addrGroup = UIUtils.createPlaceholder(composite, 2);
        GridLayout gl = new GridLayout(2, false);
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Label hostLabel = UIUtils.createControlLabel(addrGroup, MySQLMessages.dialog_connection_host);
        hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        hostText = new Text(addrGroup, SWT.BORDER);
        hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        hostText.addModifyListener(textListener);

        Label portLabel = UIUtils.createControlLabel(addrGroup, MySQLMessages.dialog_connection_port);
        portLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        portText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = fontHeight * 10;
        portText.setLayoutData(gd);
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        portText.addModifyListener(textListener);

        Label dbLabel = UIUtils.createControlLabel(addrGroup, MySQLMessages.dialog_connection_database);
        dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        dbText = new Text(addrGroup, SWT.BORDER);
        dbText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        dbText.addModifyListener(textListener);

        Label usernameLabel = UIUtils.createControlLabel(addrGroup, MySQLMessages.dialog_connection_user_name);
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        usernameText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = fontHeight * 20;
        usernameText.setLayoutData(gd);
        usernameText.addModifyListener(textListener);

        Label passwordLabel = UIUtils.createControlLabel(addrGroup, MySQLMessages.dialog_connection_password);
        passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        passwordText = new Text(addrGroup, SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = fontHeight * 20;
        passwordText.setLayoutData(gd);
        passwordText.addModifyListener(textListener);

        {
            Composite clientPanel = UIUtils.createPlaceholder(addrGroup, 1);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            clientPanel.setLayoutData(gd);

            UIUtils.createHorizontalLine(clientPanel);

            homesSelector = new ClientHomesSelector(clientPanel, SWT.NONE, "Local Client");
            gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
            homesSelector.getPanel().setLayoutData(gd);
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

        DBPDriver driver = getSite().getDriver();
        if (!activated) {
            // We set image only once at activation
            // There is a bug in Eclipse which leads to SWTException after wizard image change
            if (driver != null && driver.getId().equalsIgnoreCase(MySQLConstants.DRIVER_ID_MARIA_DB)) {
                setImageDescriptor(MARIADB_LOGO_IMG);
            } else {
                setImageDescriptor(MYSQL_LOGO_IMG);
            }
        }

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(connectionInfo.getHostName());
            } else {
                hostText.setText(MySQLConstants.DEFAULT_HOST);
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
            dbText.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
        }
        if (usernameText != null) {
            usernameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
        }
        if (passwordText != null) {
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
        }
        homesSelector.populateHomes(site.getDriver(), connectionInfo.getClientHomeId());

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
