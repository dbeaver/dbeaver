/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.cubrid.ui.internal.CubridMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

public class CubridConnectionExtraPage extends ConnectionPageAbstract {

    private static final String PROP_SHARD_TYPE = "shardType";
    private static final String PROP_SHARD_VALUE = "shardValue";
    private static final String PROP_IS_SHARD = "isShard";
    private static final String SHARD_TYPE_ID = "SHARD ID";
    private static final String SHARD_TYPE_VAL = "SHARD VAL";
    private static final String DEFAULT_SHARD_VALUE = "0";

    private Combo shardTypeCombo;
    private Text shardVal;

    public CubridConnectionExtraPage() {
        setTitle(CubridMessages.dialog_connection_cubrid_properties);
        setDescription(CubridMessages.dialog_connection_cubrid_properties_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        DBPDataSourceContainer dataSource = site.getActiveDataSource();
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        boolean enableShardControls = dataSource.getName() == null || Boolean.parseBoolean(connectionInfo.getProperty(PROP_IS_SHARD));
        createShardGroup(container, enableShardControls);

        setControl(container);
        loadSettings();
    }

    private void createShardGroup(Composite parent, boolean enableControls) {
    	Group shardGroup = new Group(parent, SWT.NONE);
    	shardGroup.setText(CubridMessages.dialog_connection_cubrid_properties_shard_setting);
    	shardGroup.setToolTipText(CubridMessages.dialog_connection_cubrid_properties_shard_tooltip);
    	shardGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    	shardGroup.setLayout(new GridLayout(2, false));

        shardTypeCombo = UIUtils.createLabelCombo(shardGroup, "Shard Hint", SWT.DROP_DOWN | SWT.READ_ONLY);
        shardTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        shardTypeCombo.add(SHARD_TYPE_ID);
        shardTypeCombo.add(SHARD_TYPE_VAL);

        shardVal = UIUtils.createLabelText(shardGroup, "Value", "");
        shardVal.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        shardTypeCombo.setEnabled(enableControls);
        shardVal.setEnabled(enableControls);
    }

    @Override
    public void loadSettings() {
        DBPDataSourceContainer dataSource = site.getActiveDataSource();
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();

        String shardType = connectionInfo.getProperty(PROP_SHARD_TYPE);
        String shardValue = connectionInfo.getProperty(PROP_SHARD_VALUE);

        if (CommonUtils.isEmpty(shardType)) {
            shardType = SHARD_TYPE_ID;
        }
        if (CommonUtils.isEmpty(shardValue)) {
            shardValue = DEFAULT_SHARD_VALUE;
        }

        shardTypeCombo.setText(shardType);
        shardVal.setText(shardValue);
        super.loadSettings();
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (shardTypeCombo != null && shardVal != null) {
            String shardType = shardTypeCombo.getText().trim();
            String shardValueStr = shardVal.getText().trim();
            
            int shardValue;
            try {
            	shardValue = Integer.parseInt(shardValueStr);
            } catch (NumberFormatException e) {
            	UIUtils.showMessageBox(
                    getShell(),
                    CubridMessages.dialog_connection_cubrid_properties_invalid_input_title,
                    CubridMessages.dialog_connection_cubrid_properties_invalid_input_message,
                    SWT.ICON_ERROR
                );
                return;
            }

            if (SHARD_TYPE_ID.equals(shardType) && (shardValue < 0 || shardValue > 1)) {
            	UIUtils.showMessageBox(
                    getShell(),
                    CubridMessages.dialog_connection_cubrid_properties_invalid_shard_id_title,
                    CubridMessages.dialog_connection_cubrid_properties_invalid_shard_id_message,
                    SWT.ICON_ERROR
                );
            	return;
            }

            connectionInfo.setProperty(PROP_SHARD_TYPE, shardType);
            connectionInfo.setProperty(PROP_SHARD_VALUE, shardValueStr);
        }
        super.saveSettings(dataSource);
    }

    @Override
    public boolean isComplete() {
        return true;
    }

}
