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
package org.jkiss.dbeaver.ext.vertica.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ext.vertica.VerticaConstants;
import org.jkiss.dbeaver.ext.vertica.ui.internal.VerticaUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

public class VerticaConnectionPageAdvanced extends ConnectionPageAbstract {

    private Button disableCommentsReading;
    private Combo sqlDollarQuoteBehaviorCombo;

    public VerticaConnectionPageAdvanced() {
        setTitle("Vertica");
    }

    @Override
    public void createControl(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group advancedSettings = new Group(group, SWT.NONE);
            advancedSettings.setText(VerticaUIMessages.connection_page_group_performance);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.horizontalSpan = 4;
            advancedSettings.setLayoutData(gridData);
            advancedSettings.setLayout(new GridLayout(1, false));

            disableCommentsReading = UIUtils.createCheckbox(
                advancedSettings,
                VerticaUIMessages.connection_page_group_checkbox_disable_comments,
                VerticaUIMessages.connection_page_group_checkbox_disable_comments_tip,
                false,
                1);
        }

        {
            Group sqlGroup = new Group(group, SWT.NONE);
            sqlGroup.setText(VerticaUIMessages.connection_page_setting_sql);
            sqlGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            sqlGroup.setLayout(new GridLayout(2, false));

            sqlDollarQuoteBehaviorCombo = UIUtils.createLabelCombo(
                sqlGroup,
                VerticaUIMessages.connection_page_sql_dd_label,
                SWT.DROP_DOWN | SWT.READ_ONLY);
            sqlDollarQuoteBehaviorCombo.add(VerticaUIMessages.connection_page_sql_dd_string);
            sqlDollarQuoteBehaviorCombo.add(VerticaUIMessages.connection_page_sql_dd_code_block);
        }

        setControl(group);
        loadSettings();

    }

    @Override
    public void loadSettings() {
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        disableCommentsReading.setSelection(CommonUtils.toBoolean(
            connectionInfo.getProviderProperty(VerticaConstants.PROP_DISABLE_COMMENTS_READING)));
        sqlDollarQuoteBehaviorCombo.select(CommonUtils.toBoolean(
            connectionInfo.getProviderProperty(VerticaConstants.PROP_DOLLAR_QUOTES_AS_STRING)) ? 0 : 1);
        super.loadSettings();
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
        if (disableCommentsReading != null) {
            connectionInfo.setProviderProperty(
                VerticaConstants.PROP_DISABLE_COMMENTS_READING,
                String.valueOf(disableCommentsReading.getSelection()));
        }
        if (sqlDollarQuoteBehaviorCombo != null) {
            connectionInfo.setProviderProperty(
                VerticaConstants.PROP_DOLLAR_QUOTES_AS_STRING,
                String.valueOf(sqlDollarQuoteBehaviorCombo.getSelectionIndex() == 0));
        }
        super.saveSettings(dataSource);
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
