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
package org.jkiss.dbeaver.ext.nosql.cassandra.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.nosql.cassandra.Activator;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

/**
 * CassandraConnectionPage
 */
public class CassandraConnectionPage extends ConnectionPageAbstract
{
    private static ImageDescriptor logoImage = Activator.getImageDescriptor("icons/cassandra_logo.png");

    // Host/port
    private Text hostText;
    private Text portText;
    // Keyspace
    private Text keyspaceText;
    // Login
    private Text userNameText;
    private Text passwordText;

    private Composite settingsGroup;

    private boolean activated;

    @Override
    public void createControl(Composite composite)
    {
        setImageDescriptor(logoImage);

        createGeneralTab(composite);
        setControl(settingsGroup);
    }

    private Composite createGeneralTab(Composite parent)
    {
        ModifyListener textListener = new ModifyListener()
        {
            @Override
            public void modifyText(ModifyEvent e)
            {
                if (activated) {
                    saveAndUpdate();
                }
            }
        };

        settingsGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        settingsGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        settingsGroup.setLayoutData(gd);

        {
            Label hostLabel = new Label(settingsGroup, SWT.NONE);
            hostLabel.setText("Host");
            hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            hostText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            hostText.setLayoutData(gd);
            hostText.addModifyListener(textListener);

            Label portLabel = new Label(settingsGroup, SWT.NONE);
            portLabel.setText("Port");
            portLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            portText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.CENTER);
            gd.widthHint = 60;
            portText.setLayoutData(gd);
            //portText.addVerifyListener(UIUtils.INTEGER_VERIFY_LISTENER);
            portText.addModifyListener(textListener);
        }

        {
            Label dbLabel = new Label(settingsGroup, SWT.NONE);
            dbLabel.setText("Keyspace");
            dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            keyspaceText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            gd.widthHint = 270;
            gd.horizontalSpan = 3;
            keyspaceText.setLayoutData(gd);
            keyspaceText.addModifyListener(textListener);
        }

        {
            Label userNameLabel = new Label(settingsGroup, SWT.NONE);
            userNameLabel.setText("User");
            userNameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            userNameText = new Text(settingsGroup, SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            //gd.horizontalSpan = 3;
            userNameText.setLayoutData(gd);
            userNameText.addModifyListener(textListener);

            Control emptyLabel = createEmptyLabel(settingsGroup, 2);

            Label passwordLabel = new Label(settingsGroup, SWT.NONE);
            passwordLabel.setText("Password");
            passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

            passwordText = new Text(settingsGroup, SWT.BORDER | SWT.PASSWORD);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.grabExcessHorizontalSpace = true;
            passwordText.setLayoutData(gd);
            passwordText.addModifyListener(textListener);
        }

        return settingsGroup;
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
        return !hostText.getText().isEmpty() &&
            !portText.getText().isEmpty() &&
            !keyspaceText.getText().isEmpty();
    }

    @Override
    public void loadSettings()
    {
        // Load values from new connection info
        DBPConnectionInfo connectionInfo = site.getActiveDataSource().getConnectionInfo();
        if (hostText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                hostText.setText(CommonUtils.notEmpty(connectionInfo.getHostName()));
            } else {
                hostText.setText("localhost"); //$NON-NLS-1$
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
        if (keyspaceText != null) {
            if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                keyspaceText.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
            } else {
                keyspaceText.setText("system");
            }
        }
        if (userNameText != null) {
            userNameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
        }
        if (passwordText != null) {
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
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
        if (keyspaceText != null) {
            connectionInfo.setDatabaseName(keyspaceText.getText());
        }
        if (userNameText != null) {
            connectionInfo.setUserName(userNameText.getText());
        }
        if (passwordText != null) {
            connectionInfo.setUserPassword(passwordText.getText());
        }
        super.saveSettings(dataSource);
    }

    private void saveAndUpdate()
    {
        saveSettings(site.getActiveDataSource());
        site.updateButtons();
    }

}
