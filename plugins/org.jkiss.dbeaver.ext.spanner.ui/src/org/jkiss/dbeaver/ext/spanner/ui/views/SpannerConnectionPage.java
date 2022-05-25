/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.spanner.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.spanner.auth.SpannerAuthModel;
import org.jkiss.dbeaver.ext.spanner.ui.SpannerActivator;
import org.jkiss.dbeaver.ext.spanner.ui.internal.SpannerMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

/**
 * SpannerConnectionPage
 */
public class SpannerConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider
{
    private static final Log log = Log.getLog(SpannerConnectionPage.class);

    private Text projectText;
    private Text instanceText;
    private Text databaseText;

    private static ImageDescriptor logoImage = SpannerActivator.getImageDescriptor("icons/spanner_logo.png"); //$NON-NLS-1$
    private DriverPropertiesDialogPage driverPropsPage;

    public SpannerConnectionPage() {
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
            Composite addrGroup = UIUtils.createControlGroup(settingsGroup, SpannerMessages.label_connection, 2, 0, 0);
            addrGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            projectText = UIUtils.createLabelText(addrGroup, SpannerMessages.label_project, ""); //$NON-NLS-2$
            projectText.addModifyListener(textListener);

            instanceText = UIUtils.createLabelText(addrGroup, SpannerMessages.label_instance, ""); //$NON-NLS-2$
            instanceText.setToolTipText(SpannerMessages.label_instance_tip);
            instanceText.addModifyListener(textListener);

            databaseText = UIUtils.createLabelText(addrGroup, SpannerMessages.label_database, ""); //$NON-NLS-2$
            databaseText.setToolTipText(SpannerMessages.label_database_tip);
            databaseText.addModifyListener(textListener);
        }

        createAuthPanel(settingsGroup, 1);
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
        super.saveSettings(dataSource);
    }

    @NotNull
    @Override
    protected String getDefaultAuthModelId(DBPDataSourceContainer dataSource) {
        return SpannerAuthModel.ID;
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate)
    {
        return new IDialogPage[] {
            driverPropsPage
        };
    }

}
