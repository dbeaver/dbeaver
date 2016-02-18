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
package org.jkiss.dbeaver.ext.wmi.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.wmi.Activator;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

/**
 * WMIConnectionPage
 */
public class WMIConnectionPage extends ConnectionPageAbstract
{
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_NAMESPACE = "root/cimv2";

    private Text domainText;
    private Text hostText;
    private Combo namespaceCombo;
    private Combo localeCombo;
    private Text usernameText;
    private Text passwordText;

    private static ImageDescriptor logoImage = Activator.getImageDescriptor("icons/wmi_logo.png");

    public WMIConnectionPage()
    {
    }

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

        ModifyListener textListener = new ModifyListener()
        {
            @Override
            public void modifyText(ModifyEvent e)
            {
                evaluateURL();
            }
        };

        Composite addrGroup = new Composite(composite, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        Label hostLabel = UIUtils.createControlLabel(addrGroup, "Host");
        hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        hostText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        hostText.setLayoutData(gd);
        hostText.addModifyListener(textListener);

        Label domainLabel = UIUtils.createControlLabel(addrGroup, "Domain");
        domainLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        domainText = new Text(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        domainText.setLayoutData(gd);
        domainText.addModifyListener(textListener);

        Label namespaceLabel = UIUtils.createControlLabel(addrGroup, "Namespace");
        namespaceLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

        namespaceCombo = new Combo(addrGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 3;
        namespaceCombo.setLayoutData(gd);
        namespaceCombo.addModifyListener(textListener);

        Label divLabel = new Label(addrGroup, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 4;
        divLabel.setLayoutData(gd);

        {
            Label usernameLabel = UIUtils.createControlLabel(addrGroup, "User");
            usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            usernameText = new Text(addrGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            usernameText.setLayoutData(gd);
            usernameText.addModifyListener(textListener);


            Label passwordLabel = UIUtils.createControlLabel(addrGroup, "Password");
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = new Text(addrGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            passwordText.setLayoutData(gd);
            passwordText.addModifyListener(textListener);
        }

        setControl(addrGroup);
    }

    @Override
    public boolean isComplete()
    {
        return hostText != null && namespaceCombo != null &&
            !CommonUtils.isEmpty(hostText.getText()) &&
            !CommonUtils.isEmpty(namespaceCombo.getText());
    }

    @Override
    public void loadSettings()
    {
        // Load values from new connection info
        DBPDataSourceContainer activeDataSource = site.getActiveDataSource();
        DBPConnectionConfiguration connectionInfo = activeDataSource.getConnectionConfiguration();
        if (connectionInfo.getHostName() == null) {
            connectionInfo.setHostName(DEFAULT_HOST);
        }
        if (connectionInfo.getDatabaseName() == null) {
            connectionInfo.setDatabaseName(DEFAULT_NAMESPACE);
        }
        if (hostText != null) {
            hostText.setText(CommonUtils.notEmpty(connectionInfo.getHostName()));
        }
        if (domainText != null) {
            domainText.setText(CommonUtils.notEmpty(connectionInfo.getServerName()));
        }
        if (usernameText != null) {
            usernameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
        }
        if (passwordText != null) {
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
        }
        if (namespaceCombo != null) {
            namespaceCombo.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (hostText != null) {
            connectionInfo.setHostName(hostText.getText());
        }
        if (domainText != null) {
            connectionInfo.setServerName(domainText.getText());
        }
        if (namespaceCombo != null) {
            connectionInfo.setDatabaseName(namespaceCombo.getText());
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


}
