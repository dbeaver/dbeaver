/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.db2.views;

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
import org.jkiss.dbeaver.ext.db2.Activator;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * DB2ConnectionPage
 */
public class DB2ConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage {
    private Text hostText;
    private Text portText;
    private Text dbText;
    private Text usernameText;
    private Text passwordText;

    private static ImageDescriptor logoImage = Activator.getImageDescriptor("icons/db2_logo.png");

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
        setImageDescriptor(logoImage);

        Composite control = new Composite(composite, SWT.NONE);
        control.setLayout(new GridLayout(1, false));
        control.setLayoutData(new GridData(GridData.FILL_BOTH));
        ModifyListener textListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                evaluateURL();
            }
        };

        {
            Composite addrGroup = UIUtils.createControlGroup(control, "Database", 4, 0, 0);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            addrGroup.setLayoutData(gd);

            Label hostLabel = UIUtils.createControlLabel(addrGroup, DB2Messages.dialog_connection_host);
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            hostText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);
            hostText.addModifyListener(textListener);

            Label portLabel = UIUtils.createControlLabel(addrGroup, DB2Messages.dialog_connection_port);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            portLabel.setLayoutData(gd);

            portText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            gd.widthHint = 40;
            portText.setLayoutData(gd);
            portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
            portText.addModifyListener(textListener);

            Label dbLabel = UIUtils.createControlLabel(addrGroup, DB2Messages.dialog_connection_database);
            dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            dbText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalSpan = 3;
            dbText.setLayoutData(gd);
            dbText.addModifyListener(textListener);
        }

        {
            Composite addrGroup = UIUtils.createControlGroup(control, "Security", 2, 0, 0);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            addrGroup.setLayoutData(gd);
                Label usernameLabel = UIUtils.createControlLabel(addrGroup, DB2Messages.dialog_connection_user_name);
            usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            usernameText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = 200;
            usernameText.setLayoutData(gd);
            usernameText.addModifyListener(textListener);

            Label passwordLabel = UIUtils.createControlLabel(addrGroup, DB2Messages.dialog_connection_password);
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = new Text(addrGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = 200;
            passwordText.setLayoutData(gd);
            passwordText.addModifyListener(textListener);
        }

        createDriverPanel(control);
        setControl(control);
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

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (hostText != null) {
            hostText.setText(CommonUtils.notEmpty(connectionInfo.getHostName()));
        }
        if (portText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                portText.setText(String.valueOf(connectionInfo.getHostPort()));
            } else {
                portText.setText(String.valueOf(DB2Constants.DEFAULT_PORT));
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
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
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
        super.saveSettings(dataSource);
    }

    private void evaluateURL()
    {
        site.updateButtons();
    }

    @Override
    public IDialogPage[] getSubPages()
    {
        return new IDialogPage[]{
            new DB2ConnectionTracePage(),
            new DriverPropertiesDialogPage(this),
        };
    }

}
