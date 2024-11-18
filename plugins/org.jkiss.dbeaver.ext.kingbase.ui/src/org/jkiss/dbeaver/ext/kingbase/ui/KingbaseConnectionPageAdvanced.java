/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.kingbase.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.KingbaseMessages;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.ext.kingbase.model.impls.KingbaseServerType;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseConnectionPageAdvanced
 */
public class KingbaseConnectionPageAdvanced extends ConnectionPageAbstract
{
    private Button showTemplates;
    private Button showUnavailable;
    private Button showDatabaseStatistics;
    private Button readAllDataTypes;
    private Button readKeysWithColumns;
    private Button usePreparedStatements;
    private Combo ddPlainBehaviorCombo;
    private Combo ddTagBehaviorCombo;

    public KingbaseConnectionPageAdvanced()
    {
        setTitle("Kingbase");
        setDescription("Kingbase - " + KingbaseMessages.dialog_setting_connection_settings);
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite cfgGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        cfgGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        cfgGroup.setLayoutData(gd);

        {
            Group secureGroup = new Group(cfgGroup, SWT.NONE);
            secureGroup.setText(KingbaseMessages.dialog_setting_connection_settings);
            secureGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            secureGroup.setLayout(new GridLayout(2, false));

            showTemplates = UIUtils.createCheckbox(secureGroup, KingbaseMessages.dialog_setting_connection_show_templates, KingbaseMessages.dialog_setting_connection_show_templates_tip, false, 2);
            showUnavailable = UIUtils.createCheckbox(secureGroup, KingbaseMessages.dialog_setting_connection_show_not_available_for_conn, KingbaseMessages.dialog_setting_connection_show_not_available_for_conn_tip, false, 2);
            showDatabaseStatistics = UIUtils.createCheckbox(secureGroup, KingbaseMessages.dialog_setting_connection_database_statistics, KingbaseMessages.dialog_setting_connection_database_statistics_tip, false, 2);
            readAllDataTypes = UIUtils.createCheckbox(secureGroup, KingbaseMessages.dialog_setting_connection_read_all_data_types, KingbaseMessages.dialog_setting_connection_read_all_data_types_tip, false, 2);

            readKeysWithColumns = UIUtils.createCheckbox(
                secureGroup,
                KingbaseMessages.dialog_setting_connection_read_keys_with_columns,
                KingbaseMessages.dialog_setting_connection_read_keys_with_columns_tip,
                false,
                2);
        }

        {
            Group secureGroup = new Group(cfgGroup, SWT.NONE);
            secureGroup.setText(KingbaseMessages.dialog_setting_group_sql);
            secureGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            secureGroup.setLayout(new GridLayout(2, false));

            ddPlainBehaviorCombo = UIUtils.createLabelCombo(secureGroup, KingbaseMessages.dialog_setting_sql_dd_plain_label, KingbaseMessages.dialog_setting_sql_dd_plain_tip, SWT.DROP_DOWN | SWT.READ_ONLY);
            ddPlainBehaviorCombo.add(KingbaseMessages.dialog_setting_sql_dd_string);
            ddPlainBehaviorCombo.add(KingbaseMessages.dialog_setting_sql_dd_code_block);
            ddTagBehaviorCombo = UIUtils.createLabelCombo(secureGroup, KingbaseMessages.dialog_setting_sql_dd_tag_label, KingbaseMessages.dialog_setting_sql_dd_tag_tip, SWT.DROP_DOWN | SWT.READ_ONLY);
            ddTagBehaviorCombo.add(KingbaseMessages.dialog_setting_sql_dd_string);
            ddTagBehaviorCombo.add(KingbaseMessages.dialog_setting_sql_dd_code_block);
        }

        final DBPDriver driver = site.getDriver();
        KingbaseServerType serverType = KingbaseUtils.getServerType(driver);

        if (serverType.turnOffPreparedStatements())
        {
            Group performanceGroup = new Group(cfgGroup, SWT.NONE);
            performanceGroup.setText(KingbaseMessages.dialog_setting_group_performance);
            performanceGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            performanceGroup.setLayout(new GridLayout(2, false));
            usePreparedStatements = UIUtils.createCheckbox(performanceGroup, KingbaseMessages.dialog_setting_connection_use_prepared_statements, KingbaseMessages.dialog_setting_connection_use_prepared_statements_tip, false, 2);
        }

        setControl(cfgGroup);

        loadSettings();
    }

    @Override
    public boolean isComplete()
    {
        return true;
    }

    @Override
    public void setSite(IDataSourceConnectionEditorSite site) {
        super.setSite(site);
        if (site != null && site.getDriver() != null) {
            setTitle(site.getDriver().getName());
        }
    }

    @Override
    public void loadSettings()
    {
        // Load values from new connection info
        DBPPreferenceStore globalPrefs = DBWorkbench.getPlatform().getPreferenceStore();
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        setTitle(site.getActiveDataSource().getDriver().getName());

        showTemplates.setSelection(
            CommonUtils.getBoolean(connectionInfo.getProviderProperty(KingbaseConstants.PROP_SHOW_TEMPLATES_DB),
            globalPrefs.getBoolean(KingbaseConstants.PROP_SHOW_TEMPLATES_DB)));
        showUnavailable.setSelection(
            CommonUtils.getBoolean(connectionInfo.getProviderProperty(KingbaseConstants.PROP_SHOW_UNAVAILABLE_DB),
            globalPrefs.getBoolean(KingbaseConstants.PROP_SHOW_UNAVAILABLE_DB)));
        showDatabaseStatistics.setSelection(
            CommonUtils.getBoolean(connectionInfo.getProviderProperty(KingbaseConstants.PROP_SHOW_DATABASE_STATISTICS),
                globalPrefs.getBoolean(KingbaseConstants.PROP_SHOW_DATABASE_STATISTICS)));
        readAllDataTypes.setSelection(
                CommonUtils.getBoolean(connectionInfo.getProviderProperty(KingbaseConstants.PROP_READ_ALL_DATA_TYPES),
                        globalPrefs.getBoolean(KingbaseConstants.PROP_READ_ALL_DATA_TYPES)));
        readKeysWithColumns.setSelection(
            CommonUtils.getBoolean(connectionInfo.getProviderProperty(KingbaseConstants.PROP_READ_KEYS_WITH_COLUMNS),
                globalPrefs.getBoolean(KingbaseConstants.PROP_READ_KEYS_WITH_COLUMNS)));
        if (usePreparedStatements != null) {
            usePreparedStatements.setSelection(
                    CommonUtils.getBoolean(connectionInfo.getProviderProperty(KingbaseConstants.PROP_USE_PREPARED_STATEMENTS), false));
        }

        ddPlainBehaviorCombo.select(CommonUtils.getBoolean(
            connectionInfo.getProviderProperty(KingbaseConstants.PROP_DD_PLAIN_STRING),
            globalPrefs.getBoolean(KingbaseConstants.PROP_DD_PLAIN_STRING)) ? 0 : 1);
        ddTagBehaviorCombo.select(CommonUtils.getBoolean(
            connectionInfo.getProviderProperty(KingbaseConstants.PROP_DD_TAG_STRING),
            globalPrefs.getBoolean(KingbaseConstants.PROP_DD_TAG_STRING)) ? 0 : 1);
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        DBPConnectionConfiguration connectionCfg = dataSource.getConnectionConfiguration();

        connectionCfg.setProviderProperty(KingbaseConstants.PROP_SHOW_TEMPLATES_DB, String.valueOf(showTemplates.getSelection()));
        connectionCfg.setProviderProperty(KingbaseConstants.PROP_SHOW_UNAVAILABLE_DB, String.valueOf(showUnavailable.getSelection()));
        connectionCfg.setProviderProperty(KingbaseConstants.PROP_SHOW_DATABASE_STATISTICS, String.valueOf(showDatabaseStatistics.getSelection()));
        connectionCfg.setProviderProperty(KingbaseConstants.PROP_READ_ALL_DATA_TYPES, String.valueOf(readAllDataTypes.getSelection()));
        connectionCfg.setProviderProperty(KingbaseConstants.PROP_READ_KEYS_WITH_COLUMNS, String.valueOf(readKeysWithColumns.getSelection()));
        if (usePreparedStatements != null) {
            connectionCfg.setProviderProperty(KingbaseConstants.PROP_USE_PREPARED_STATEMENTS, String.valueOf(usePreparedStatements.getSelection()));
        }

        connectionCfg.setProviderProperty(KingbaseConstants.PROP_DD_PLAIN_STRING, String.valueOf(ddPlainBehaviorCombo.getSelectionIndex() == 0));
        connectionCfg.setProviderProperty(KingbaseConstants.PROP_DD_TAG_STRING, String.valueOf(ddTagBehaviorCombo.getSelectionIndex() == 0));

        saveConnectionURL(connectionCfg);
    }

}
