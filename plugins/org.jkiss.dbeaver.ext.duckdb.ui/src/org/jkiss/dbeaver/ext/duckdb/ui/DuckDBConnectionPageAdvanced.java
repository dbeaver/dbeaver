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
package org.jkiss.dbeaver.ext.duckdb.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ext.duckdb.model.DuckDBConstants;
import org.jkiss.dbeaver.ext.duckdb.ui.internal.DuckDBMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

public class DuckDBConnectionPageAdvanced extends ConnectionPageAbstract {

    private Combo sqlDollarQuotePlainCombo;
    private Combo sqlDollarQuoteTagCombo;

    public DuckDBConnectionPageAdvanced() {
        setTitle("DuckDB");
    }

    @Override
    public void createControl(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group sqlGroup = new Group(group, SWT.NONE);
            sqlGroup.setText(DuckDBMessages.dialog_setting_sql);
            sqlGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            sqlGroup.setLayout(new GridLayout(2, false));

            sqlDollarQuotePlainCombo = UIUtils.createLabelCombo(sqlGroup, DuckDBMessages.dialog_setting_sql_dd_plain_label, SWT.DROP_DOWN | SWT.READ_ONLY);
            sqlDollarQuotePlainCombo.add(DuckDBMessages.dialog_setting_sql_dd_string);
            sqlDollarQuotePlainCombo.add(DuckDBMessages.dialog_setting_sql_dd_code_block);

            sqlDollarQuoteTagCombo = UIUtils.createLabelCombo(sqlGroup, DuckDBMessages.dialog_setting_sql_dd_tag_label, SWT.DROP_DOWN | SWT.READ_ONLY);
            sqlDollarQuoteTagCombo.add(DuckDBMessages.dialog_setting_sql_dd_string);
            sqlDollarQuoteTagCombo.add(DuckDBMessages.dialog_setting_sql_dd_code_block);
        }

        setControl(group);
        loadSettings();
    }

    @Override
    public void loadSettings() {
        final DBPConnectionConfiguration config = getSite().getActiveDataSource().getConnectionConfiguration();
        sqlDollarQuotePlainCombo.select(CommonUtils.getBoolean(config.getProviderProperty(DuckDBConstants.PROP_DD_PLAIN_STRING), false) ? 0 : 1);
        sqlDollarQuoteTagCombo.select(CommonUtils.getBoolean(config.getProviderProperty(DuckDBConstants.PROP_DD_TAG_STRING), false) ? 0 : 1);
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        dataSource.getConnectionConfiguration().setProviderProperty(
            DuckDBConstants.PROP_DD_PLAIN_STRING,
            CommonUtils.toString(sqlDollarQuotePlainCombo.getSelectionIndex() == 0)
        );
        dataSource.getConnectionConfiguration().setProviderProperty(
            DuckDBConstants.PROP_DD_TAG_STRING,
            CommonUtils.toString(sqlDollarQuoteTagCombo.getSelectionIndex() == 0)
        );
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
