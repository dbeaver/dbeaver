/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mysql.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mysql.Activator;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ClientHomesSelector;
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
    private Button useSslButton;
    private Text sslCertText;
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
                    saveSettings(site.getActiveDataSource());
                    site.updateButtons();
                }
            }
        };

        Composite addrGroup = UIUtils.createPlaceholder(composite, 4);
        GridLayout gl = new GridLayout(4, false);
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Label hostLabel = UIUtils.createControlLabel(addrGroup, MySQLMessages.dialog_connection_host);
        hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        hostText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(textListener);

        Label portLabel = UIUtils.createControlLabel(addrGroup, MySQLMessages.dialog_connection_port);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        portLabel.setLayoutData(gd);

        portText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = 40;
        portText.setLayoutData(gd);
        portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
        portText.addModifyListener(textListener);

        Label dbLabel = UIUtils.createControlLabel(addrGroup, MySQLMessages.dialog_connection_database);
        dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        dbText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 3;
        dbText.setLayoutData(gd);
        dbText.addModifyListener(textListener);

        Label usernameLabel = UIUtils.createControlLabel(addrGroup, MySQLMessages.dialog_connection_user_name);
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        usernameText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        usernameText.setLayoutData(gd);
        usernameText.addModifyListener(textListener);

        Label passwordLabel = UIUtils.createControlLabel(addrGroup, MySQLMessages.dialog_connection_password);
        passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        passwordText = new Text(addrGroup, SWT.BORDER | SWT.PASSWORD);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        passwordText.setLayoutData(gd);
        passwordText.addModifyListener(textListener);

        {
            Composite buttonsGroup = new Composite(addrGroup, SWT.NONE);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            buttonsGroup.setLayoutData(gd);
            buttonsGroup.setLayout(new GridLayout(2, false));
            homesSelector = new ClientHomesSelector(buttonsGroup, SWT.NONE, "Local Client");
            gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
            homesSelector.setLayoutData(gd);
        }

        {
            Group secureGroup = new Group(addrGroup, SWT.NONE);
            secureGroup.setText("Security");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 4;
            secureGroup.setLayoutData(gd);
            secureGroup.setLayout(new GridLayout(2, false));

            useSslButton = UIUtils.createLabelCheckbox(secureGroup, "Use SSL", true);
            useSslButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (sslCertText != null) {
                        sslCertText.setEnabled(useSslButton.getSelection());
                    }
                }
            });
            //sslCertText = UIUtils.createLabelText(secureGroup, "Certificate Path", "");
        }

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
        DBPDriver driver = getSite().getDriver();
        if (driver != null && driver.getId().equalsIgnoreCase(MySQLConstants.DRIVER_ID_MARIA_DB)) {
            setImageDescriptor(MARIADB_LOGO_IMG);
        } else {
            setImageDescriptor(MYSQL_LOGO_IMG);
        }

        // Load values from new connection info
        DBPConnectionInfo connectionInfo = site.getActiveDataSource().getConnectionInfo();
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
            } else {
                portText.setText(String.valueOf(MySQLConstants.DEFAULT_PORT));
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

        final boolean useSSL = CommonUtils.toBoolean(connectionInfo.getProperty(MySQLConstants.PROP_USE_SSL));
        useSslButton.setSelection(useSSL);
        if (sslCertText != null) {
            sslCertText.setText(CommonUtils.toString(connectionInfo.getProperty(MySQLConstants.PROP_SSL_CERT)));
            sslCertText.setEnabled(useSSL);
        }

        activated = true;
    }

    @Override
    public void saveSettings(DataSourceDescriptor dataSource)
    {
        DBPConnectionInfo connectionInfo = dataSource.getConnectionInfo();
        if (hostText != null) {
            connectionInfo.setHostName(hostText.getText());
        }
        if (portText != null) {
            connectionInfo.setHostPort(portText.getText());
        }
        if (dbText != null) {
            connectionInfo.setDatabaseName(dbText.getText());
        }
        if (usernameText != null) {
            connectionInfo.setUserName(usernameText.getText());
        }
        if (passwordText != null) {
            connectionInfo.setUserPassword(passwordText.getText());
        }
        if (homesSelector != null) {
            connectionInfo.setClientHomeId(homesSelector.getSelectedHome());
        }

        connectionInfo.setProperty(MySQLConstants.PROP_USE_SSL, useSslButton.getSelection());
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
