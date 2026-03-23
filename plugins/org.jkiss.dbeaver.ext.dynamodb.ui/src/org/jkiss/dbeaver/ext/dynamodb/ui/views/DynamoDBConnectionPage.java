/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.dynamodb.ui.views;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.dynamodb.DynamoDBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

public class DynamoDBConnectionPage extends ConnectionPageAbstract implements IDialogPageProvider {

    private static final String[] AWS_REGIONS = {
            "us-east-1", "us-east-2", "us-west-1", "us-west-2",
            "ca-central-1", "ca-west-1",
            "eu-central-1", "eu-central-2", "eu-west-1", "eu-west-2", "eu-west-3",
            "eu-north-1", "eu-south-1", "eu-south-2",
            "ap-northeast-1", "ap-northeast-2", "ap-northeast-3",
            "ap-southeast-1", "ap-southeast-2", "ap-southeast-3", "ap-southeast-4",
            "ap-south-1", "ap-south-2", "ap-east-1",
            "sa-east-1",
            "me-south-1", "me-central-1",
            "af-south-1",
            "il-central-1"
    };

    private Text connectionNameText;
    private Combo regionCombo;
    private Text endpointText;
    private Text accessKeyText;
    private Text secretKeyText;
    private Text roleArnText;
    private Text externalIdText;

    @Override
    public void createControl(Composite composite) {
        Composite settingsGroup = new Composite(composite, SWT.NONE);
        settingsGroup.setLayout(new GridLayout(1, false));
        settingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        ModifyListener textListener = e -> {
            if (isActivated()) {
                site.updateButtons();
            }
        };

        // Connection settings group
        {
            Composite addrGroup = UIUtils.createControlGroup(
                    settingsGroup, "Connection", 2, GridData.FILL_HORIZONTAL, 0);

            UIUtils.createControlLabel(addrGroup, "Connection Name");
            connectionNameText = new Text(addrGroup, SWT.BORDER);
            connectionNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            connectionNameText.setToolTipText("Optional display name for this connection");
            connectionNameText.addModifyListener(textListener);

            regionCombo = UIUtils.createLabelCombo(addrGroup, "AWS Region", SWT.DROP_DOWN);
            regionCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            for (String region : AWS_REGIONS) {
                regionCombo.add(region);
            }
            regionCombo.addModifyListener(textListener);

            UIUtils.createControlLabel(addrGroup, "Custom Endpoint");
            endpointText = new Text(addrGroup, SWT.BORDER);
            endpointText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            endpointText.setToolTipText("Override endpoint for DynamoDB Local or LocalStack");
            endpointText.addModifyListener(textListener);
        }

        // AWS credentials group
        {
            Composite credGroup = UIUtils.createControlGroup(
                    settingsGroup, "AWS Credentials", 2, GridData.FILL_HORIZONTAL, 0);

            UIUtils.createControlLabel(credGroup, "Access Key");
            accessKeyText = new Text(credGroup, SWT.BORDER);
            accessKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            accessKeyText.addModifyListener(textListener);

            UIUtils.createControlLabel(credGroup, "Secret Key");
            secretKeyText = new Text(credGroup, SWT.BORDER | SWT.PASSWORD);
            secretKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            secretKeyText.addModifyListener(textListener);
        }

        // IAM Role assumption group
        {
            Composite roleGroup = UIUtils.createControlGroup(
                    settingsGroup, "IAM Role (optional)", 2, GridData.FILL_HORIZONTAL, 0);

            UIUtils.createControlLabel(roleGroup, "Role ARN");
            roleArnText = new Text(roleGroup, SWT.BORDER);
            roleArnText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            roleArnText.setToolTipText("IAM role ARN for cross-account access");
            roleArnText.addModifyListener(textListener);

            UIUtils.createControlLabel(roleGroup, "External ID");
            externalIdText = new Text(roleGroup, SWT.BORDER);
            externalIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            externalIdText.setToolTipText("External ID required by the trust policy");
            externalIdText.addModifyListener(textListener);
        }
        createDriverPanel(settingsGroup);
        setControl(settingsGroup);
    }

    private boolean isActivated() {
        return regionCombo != null && !regionCombo.isDisposed();
    }

    @Override
    public boolean isComplete() {
        return regionCombo != null
                && !CommonUtils.isEmpty(regionCombo.getText());
    }

    @Override
    public void loadSettings() {
        super.loadSettings();
        DBPConnectionConfiguration cfg = site.getActiveDataSource().getConnectionConfiguration();

        if (connectionNameText != null) {
            String connName = cfg.getProviderProperty(DynamoDBConstants.PROP_CONNECTION_NAME);
            if (!CommonUtils.isEmpty(connName)) {
                connectionNameText.setText(connName);
            }
        }
        if (regionCombo != null) {
            String region = cfg.getProviderProperty(DynamoDBConstants.PROP_REGION);
            if (CommonUtils.isEmpty(region)) {
                region = cfg.getServerName();
            }
            if (CommonUtils.isEmpty(region)) {
                region = DynamoDBConstants.DEFAULT_REGION;
            }
            regionCombo.setText(region);
        }
        if (endpointText != null) {
            String endpoint = cfg.getProviderProperty(DynamoDBConstants.PROP_ENDPOINT);
            if (!CommonUtils.isEmpty(endpoint)) {
                endpointText.setText(endpoint);
            }
        }
        if (accessKeyText != null) {
            String accessKey = cfg.getUserName();
            if (!CommonUtils.isEmpty(accessKey)) {
                accessKeyText.setText(accessKey);
            }
        }
        if (secretKeyText != null) {
            String secretKey = cfg.getUserPassword();
            if (!CommonUtils.isEmpty(secretKey)) {
                secretKeyText.setText(secretKey);
            }
        }
        if (roleArnText != null) {
            String roleArn = cfg.getProviderProperty(DynamoDBConstants.PROP_ROLE_ARN);
            if (!CommonUtils.isEmpty(roleArn)) {
                roleArnText.setText(roleArn);
            }
        }
        if (externalIdText != null) {
            String externalId = cfg.getProviderProperty(DynamoDBConstants.PROP_EXTERNAL_ID);
            if (!CommonUtils.isEmpty(externalId)) {
                externalIdText.setText(externalId);
            }
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration cfg = dataSource.getConnectionConfiguration();

        if (connectionNameText != null) {
            String connName = connectionNameText.getText().trim();
            if (!CommonUtils.isEmpty(connName)) {
                cfg.setProviderProperty(DynamoDBConstants.PROP_CONNECTION_NAME, connName);
                dataSource.setName(connName);
            } else {
                cfg.removeProviderProperty(DynamoDBConstants.PROP_CONNECTION_NAME);
            }
        }
        if (regionCombo != null) {
            String region = regionCombo.getText().trim();
            cfg.setServerName(region);
            cfg.setProviderProperty(DynamoDBConstants.PROP_REGION, region);
        }
        if (endpointText != null) {
            String endpoint = endpointText.getText().trim();
            if (!CommonUtils.isEmpty(endpoint)) {
                cfg.setProviderProperty(DynamoDBConstants.PROP_ENDPOINT, endpoint);
            } else {
                cfg.removeProviderProperty(DynamoDBConstants.PROP_ENDPOINT);
            }
        }
        if (accessKeyText != null) {
            cfg.setUserName(accessKeyText.getText().trim());
        }
        if (secretKeyText != null) {
            cfg.setUserPassword(secretKeyText.getText().trim());
        }
        if (roleArnText != null) {
            String roleArn = roleArnText.getText().trim();
            if (!CommonUtils.isEmpty(roleArn)) {
                cfg.setProviderProperty(DynamoDBConstants.PROP_ROLE_ARN, roleArn);
            } else {
                cfg.removeProviderProperty(DynamoDBConstants.PROP_ROLE_ARN);
            }
        }
        if (externalIdText != null) {
            String externalId = externalIdText.getText().trim();
            if (!CommonUtils.isEmpty(externalId)) {
                cfg.setProviderProperty(DynamoDBConstants.PROP_EXTERNAL_ID, externalId);
            } else {
                cfg.removeProviderProperty(DynamoDBConstants.PROP_EXTERNAL_ID);
            }
        }
        super.saveSettings(dataSource);
    }

    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[0];
    }
}
