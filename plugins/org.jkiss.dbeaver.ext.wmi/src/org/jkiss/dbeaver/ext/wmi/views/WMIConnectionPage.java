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

            UIUtils.createEmptyLabel(addrGroup, 2, 1);

            Label passwordLabel = UIUtils.createControlLabel(addrGroup, "Password");
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = new Text(addrGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            passwordText.setLayoutData(gd);
            passwordText.addModifyListener(textListener);

            createSavePasswordButton(addrGroup, 2);
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
        super.loadSettings();
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (hostText != null) {
            connectionInfo.setHostName(hostText.getText().trim());
        }
        if (domainText != null) {
            connectionInfo.setServerName(domainText.getText().trim());
        }
        if (namespaceCombo != null) {
            connectionInfo.setDatabaseName(namespaceCombo.getText().trim());
        }
        if (usernameText != null) {
            connectionInfo.setUserName(usernameText.getText().trim());
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
