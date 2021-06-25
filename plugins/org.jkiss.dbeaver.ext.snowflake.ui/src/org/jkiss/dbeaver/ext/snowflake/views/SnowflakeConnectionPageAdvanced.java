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
package org.jkiss.dbeaver.ext.snowflake.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ext.snowflake.SnowflakeConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;

public class SnowflakeConnectionPageAdvanced extends ConnectionPageAbstract {

    private Combo sqlDollarQuoteBehaviorCombo;

    public SnowflakeConnectionPageAdvanced() {
        setTitle("Snowflake");
    }

    @Override
    public void createControl(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group sqlGroup = new Group(group, SWT.NONE);
            sqlGroup.setText(SnowflakeMessages.dialog_setting_sql);
            sqlGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            sqlGroup.setLayout(new GridLayout(2, false));

            sqlDollarQuoteBehaviorCombo = UIUtils.createLabelCombo(sqlGroup, SnowflakeMessages.dialog_setting_sql_dd_label, SWT.DROP_DOWN | SWT.READ_ONLY);
            sqlDollarQuoteBehaviorCombo.add(SnowflakeMessages.dialog_setting_sql_dd_string);
            sqlDollarQuoteBehaviorCombo.add(SnowflakeMessages.dialog_setting_sql_dd_code_block);
        }

        setControl(group);
        loadSettings();
    }

    @Override
    public void loadSettings() {
        final DBPPreferenceStore store = getSite().getActiveDataSource().getPreferenceStore();
        sqlDollarQuoteBehaviorCombo.select(store.getBoolean(SnowflakeConstants.PROP_DD_STRING) ? 0 : 1);
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        final DBPPreferenceStore store = dataSource.getPreferenceStore();
        store.setValue(SnowflakeConstants.PROP_DD_STRING, sqlDollarQuoteBehaviorCombo.getSelectionIndex() == 0);
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
