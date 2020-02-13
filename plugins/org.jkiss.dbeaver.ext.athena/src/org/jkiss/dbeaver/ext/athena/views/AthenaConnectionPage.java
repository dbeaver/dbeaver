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
package org.jkiss.dbeaver.ext.athena.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.athena.AthenaActivator;
import org.jkiss.dbeaver.ext.athena.internal.AthenaMessages;
import org.jkiss.dbeaver.ext.athena.model.AWSRegion;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

/**
 * AthenaConnectionPage
 */
public class AthenaConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage {

    private Combo awsRegionCombo;
    private Text s3LocationText;
    private Text accessKeyText;

    private static ImageDescriptor logoImage = AthenaActivator.getImageDescriptor("icons/aws_athena_logo.png"); //$NON-NLS-1$
    private DriverPropertiesDialogPage driverPropsPage;

    public AthenaConnectionPage() {
        driverPropsPage = new DriverPropertiesDialogPage(this);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void createControl(Composite composite) {
        setImageDescriptor(logoImage);

        Composite settingsGroup = new Composite(composite, SWT.NONE);
        settingsGroup.setLayout(new GridLayout(1, false));
        settingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        ModifyListener textListener = e -> site.updateButtons();

        {
            Composite addrGroup = UIUtils.createControlGroup(settingsGroup, AthenaMessages.label_connection, 2, 0, 0);
            addrGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            awsRegionCombo = UIUtils.createLabelCombo(addrGroup, AthenaMessages.label_region, SWT.DROP_DOWN);
            awsRegionCombo.addModifyListener(textListener);

            s3LocationText = UIUtils.createLabelText(addrGroup, AthenaMessages.label_s3_location, ""); //$NON-NLS-2$ //$NON-NLS-1$ //$NON-NLS-1$ //$NON-NLS-1$
            s3LocationText.setToolTipText(AthenaMessages.label_s3_output_location);
            s3LocationText.addModifyListener(textListener);
        }

        {
            Composite addrGroup = UIUtils.createControlGroup(settingsGroup, AthenaMessages.label_security, 2, 0, 0);
            addrGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            accessKeyText = UIUtils.createLabelText(addrGroup, AthenaMessages.label_aws_access_key, ""); //$NON-NLS-1$
            accessKeyText.setToolTipText(AthenaMessages.label_access_key);
            accessKeyText.addModifyListener(textListener);

            Text secretAccessKeyText = createPasswordText(addrGroup, AthenaMessages.label_secret_key);
            secretAccessKeyText.setToolTipText(AthenaMessages.label_access_key_id);
            secretAccessKeyText.addModifyListener(textListener);

            createPasswordControls(addrGroup, 2);
        }


        createDriverPanel(settingsGroup);
        setControl(settingsGroup);
    }

    @Override
    public boolean isComplete() {
        return awsRegionCombo != null && !CommonUtils.isEmpty(awsRegionCombo.getText()) &&
            s3LocationText != null && !CommonUtils.isEmpty(s3LocationText.getText()) &&
            accessKeyText != null && !CommonUtils.isEmpty(accessKeyText.getText()) &&
            passwordText != null && !CommonUtils.isEmpty(passwordText.getText());
    }

    @Override
    public void loadSettings() {
        super.loadSettings();

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();

        if (awsRegionCombo != null) {
            awsRegionCombo.removeAll();
            for (AWSRegion region : AWSRegion.values()) {
                awsRegionCombo.add(region.getId());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getServerName())) {
                awsRegionCombo.setText(connectionInfo.getServerName());
            }
            if (awsRegionCombo.getText().isEmpty()) {
                awsRegionCombo.setText(AWSRegion.us_west_1.getId());
            }
        }

        if (s3LocationText != null) {
            String databaseName = connectionInfo.getDatabaseName();
            if (CommonUtils.isEmpty(databaseName)) {
                databaseName = "s3://"; //$NON-NLS-1$
            }
            s3LocationText.setText(databaseName);
        }

        if (accessKeyText != null) {
            accessKeyText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
        }
        if (passwordText != null) {
            passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (awsRegionCombo != null) {
            connectionInfo.setServerName(awsRegionCombo.getText().trim());
        }
        if (s3LocationText != null) {
            connectionInfo.setDatabaseName(s3LocationText.getText().trim());
        }
        if (accessKeyText != null) {
            connectionInfo.setUserName(accessKeyText.getText().trim());
        }
        if (passwordText != null && savePasswordCheck.getSelection()) {
            connectionInfo.setUserPassword(passwordText.getText().trim());
        }
        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getSubPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[]{
            driverPropsPage
        };
    }

}
