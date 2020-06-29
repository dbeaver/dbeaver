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
package org.jkiss.dbeaver.ext.cloudspanner.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cloudspanner.ui.CloudSpannerActivator;
import org.jkiss.dbeaver.ext.cloudspanner.ui.internal.CloudSpannerMessages;
import org.jkiss.dbeaver.ext.cloudspanner.model.CloudSpannerConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

/**
 * CloudSpannerConnectionPage
 */
public class CloudSpannerConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage
{
    private static final Log log = Log.getLog(CloudSpannerConnectionPage.class);

    private Text projectText;
    private Text instanceText;
    private Text databaseText;
    private TextWithOpenFile privateKeyFile;

    private static ImageDescriptor logoImage = CloudSpannerActivator.getImageDescriptor("icons/spanner_logo.png"); //$NON-NLS-1$
    private DriverPropertiesDialogPage driverPropsPage;

    public CloudSpannerConnectionPage() {
        driverPropsPage = new DriverPropertiesDialogPage(this);
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite)
    {
        setImageDescriptor(logoImage);

        Composite settingsGroup = new Composite(composite, SWT.NONE);
        settingsGroup.setLayout(new GridLayout(1, false));
        settingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        ModifyListener textListener = e -> site.updateButtons();

        {
            Composite addrGroup = UIUtils.createControlGroup(settingsGroup, CloudSpannerMessages.label_connection, 2, 0, 0);
            addrGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            projectText = UIUtils.createLabelText(addrGroup, CloudSpannerMessages.label_project, ""); //$NON-NLS-2$
            projectText.setToolTipText("Cloud Spanner Project ID"); //$NON-NLS-1$
            projectText.addModifyListener(textListener);

            instanceText = UIUtils.createLabelText(addrGroup, CloudSpannerMessages.label_instance, ""); //$NON-NLS-2$
            instanceText.setToolTipText("Cloud Spanner Instance ID"); //$NON-NLS-1$
            instanceText.addModifyListener(textListener);

            databaseText = UIUtils.createLabelText(addrGroup, CloudSpannerMessages.label_database, ""); //$NON-NLS-2$
            databaseText.setToolTipText("Cloud Spanner Database ID"); //$NON-NLS-1$
            databaseText.addModifyListener(textListener);

            UIUtils.createControlLabel(addrGroup, CloudSpannerMessages.label_private_key_path);
            privateKeyFile = new TextWithOpenFile(addrGroup, CloudSpannerMessages.label_private_key_path, new String[] { "*", "*.json" } ); //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
            //gd.horizontalSpan = 3;
            privateKeyFile.setLayoutData(gd);
        }

        createDriverPanel(settingsGroup);
        setControl(settingsGroup);
    }

    @Override
    public boolean isComplete()
    {
        return projectText != null && !CommonUtils.isEmpty(projectText.getText()) &&
            instanceText != null && !CommonUtils.isEmpty(instanceText.getText()) &&
            databaseText != null && !CommonUtils.isEmpty(databaseText.getText());
    }

    @Override
    public void loadSettings()
    {
        super.loadSettings();

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        if (projectText != null) {
            projectText.setText(CommonUtils.notEmpty(connectionInfo.getServerName()));
        }
        if (instanceText != null) {
            instanceText.setText(CommonUtils.notEmpty(connectionInfo.getHostName()));
        }
        if (databaseText != null) {
            databaseText.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
        }
        if (privateKeyFile != null) {
            privateKeyFile.setText(CommonUtils.notEmpty(connectionInfo.getProviderProperty(CloudSpannerConstants.DRIVER_PROP_CREDENTIALS_FILE)));
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (projectText != null) {
            connectionInfo.setServerName(projectText.getText().trim());
        }
        if (instanceText != null) {
            connectionInfo.setHostName(instanceText.getText().trim());
        }
        if (databaseText != null) {
            connectionInfo.setDatabaseName(databaseText.getText().trim());
        }
        if (privateKeyFile != null) {
            connectionInfo.setProviderProperty(CloudSpannerConstants.DRIVER_PROP_CREDENTIALS_FILE, privateKeyFile.getText());
        }
        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getSubPages(boolean extrasOnly, boolean forceCreate)
    {
        return new IDialogPage[] {
            driverPropsPage
        };
    }

}
