/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

public class MySQLConnectionPageAdvanced extends ConnectionPageAbstract {

    private Button readKeysWithColumns;

    MySQLConnectionPageAdvanced() {
        setTitle("MySQL Settings"); //$NON-NLS-1$
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void createControl(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group advancedSettings = new Group(group, SWT.NONE);
            advancedSettings.setText(MySQLUIMessages.connection_settings_advanced_group_performance);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.horizontalSpan = 4;
            advancedSettings.setLayoutData(gridData);
            advancedSettings.setLayout(new GridLayout(1, false));

            readKeysWithColumns = UIUtils.createCheckbox(
                advancedSettings,
                MySQLUIMessages.connection_settings_advanced_checkbox_read_keys,
                MySQLUIMessages.connection_settings_advanced_checkbox_read_keys_tip,
                false,
                1);
        }

        setControl(group);
        loadSettings();
    }

    @Override
    public void setSite(IDataSourceConnectionEditorSite site) {
        super.setSite(site);
        if (site != null && site.getDriver() != null) {
            setTitle(site.getDriver().getName());
        }
    }

    @Override
    public void loadSettings() {
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        readKeysWithColumns.setSelection(CommonUtils.toBoolean(
            connectionInfo.getProviderProperty(MySQLConstants.PROP_READ_KEYS_CACHE_WITH_COLUMNS)));
        super.loadSettings();
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (readKeysWithColumns != null) {
            connectionInfo.setProviderProperty(
                MySQLConstants.PROP_READ_KEYS_CACHE_WITH_COLUMNS,
                String.valueOf(readKeysWithColumns.getSelection()));
        }
        super.saveSettings(dataSource);
    }
}
