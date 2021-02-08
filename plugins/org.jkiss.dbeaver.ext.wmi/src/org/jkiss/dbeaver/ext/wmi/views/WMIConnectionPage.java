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
package org.jkiss.dbeaver.ext.wmi.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.wmi.Activator;
import org.jkiss.dbeaver.ext.wmi.WMIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.utils.CommonUtils;

/**
 * WMIConnectionPage
 */
public class WMIConnectionPage extends ConnectionPageWithAuth
{
    private static final String DEFAULT_HOST = "localhost"; //$NON-NLS-1$
    private static final String DEFAULT_NAMESPACE = "root/cimv2"; //$NON-NLS-1$

    private Text domainText;
    private Text hostText;
    private Combo namespaceCombo;
    private Combo localeCombo;

    private static ImageDescriptor logoImage = Activator.getImageDescriptor("icons/wmi_icon_big.png"); //$NON-NLS-1$

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

        ModifyListener textListener = e -> evaluateURL();

        Composite addrGroup = new Composite(composite, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        addrGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        addrGroup.setLayoutData(gd);

        {
            Group hostGroup = UIUtils.createControlGroup(addrGroup, "Server", 4, GridData.FILL_HORIZONTAL, 0);
            Label hostLabel = UIUtils.createControlLabel(hostGroup, WMIMessages.wmi_connection_page_label_host);
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            hostText = new Text(hostGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);
            hostText.addModifyListener(textListener);

            Label domainLabel = UIUtils.createControlLabel(hostGroup, WMIMessages.wmi_connection_page_label_domain);
            domainLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            domainText = new Text(hostGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            domainText.setLayoutData(gd);
            domainText.addModifyListener(textListener);

            Label namespaceLabel = UIUtils.createControlLabel(hostGroup, WMIMessages.wmi_connection_page_label_namespace);
            namespaceLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            namespaceCombo = new Combo(hostGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalSpan = 3;
            namespaceCombo.setLayoutData(gd);
            namespaceCombo.addModifyListener(textListener);
        }

        createAuthPanel(addrGroup, 1);
        createDriverPanel(addrGroup);

        setControl(addrGroup);
    }

    @Override
    public boolean isComplete()
    {
        return super.isComplete() &&
            hostText != null && namespaceCombo != null &&
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
        super.saveSettings(dataSource);
    }

    private void evaluateURL()
    {
        site.updateButtons();
    }


}
