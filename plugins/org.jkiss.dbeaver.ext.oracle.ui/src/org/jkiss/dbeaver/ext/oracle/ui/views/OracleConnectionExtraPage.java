/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleLanguage;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleTerritory;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * OracleConnectionPage
 */
public class OracleConnectionExtraPage extends ConnectionPageAbstract
{

    private Combo languageCombo;
    private Combo territoryCombo;
    private Text nlsDateFormat;
    private Button hideEmptySchemasCheckbox;
    private Button showDBAAlwaysCheckbox;
    private Button useDBAViewsCheckbox;
    private Button useSysSchemaCheckbox;
    private Button useRuleHint;
    private Button useOptimizerHint;
    private Button useSimpleConstraints;

    public OracleConnectionExtraPage()
    {
        setTitle(OracleUIMessages.dialog_connection_oracle_properties);
        setDescription(OracleUIMessages.dialog_connection_oracle_properties_discription);
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
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        cfgGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        cfgGroup.setLayoutData(gd);

        {
            final Group sessionGroup = UIUtils.createControlGroup(cfgGroup, OracleUIMessages.dialog_controlgroup_session_settings, 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            languageCombo = UIUtils.createLabelCombo(sessionGroup, OracleUIMessages.edit_label_combo_language, SWT.DROP_DOWN);
            languageCombo.setToolTipText(OracleUIMessages.edit_label_combo_language_tool_tip_text);
            languageCombo.add(OracleConstants.NLS_DEFAULT_VALUE);
            for (OracleLanguage language : OracleLanguage.values()) {
                languageCombo.add(language.getLanguage());
            }
            languageCombo.setText(OracleConstants.NLS_DEFAULT_VALUE);

            territoryCombo = UIUtils.createLabelCombo(sessionGroup, OracleUIMessages.edit_label_combo_territory, SWT.DROP_DOWN);
            territoryCombo.setToolTipText(OracleUIMessages.edit_label_combo_territory_tool_tip_text);
            territoryCombo.add(OracleConstants.NLS_DEFAULT_VALUE);
            for (OracleTerritory territory : OracleTerritory.values()) {
                territoryCombo.add(territory.getTerritory());
            }
            territoryCombo.setText(OracleConstants.NLS_DEFAULT_VALUE);

            nlsDateFormat = UIUtils.createLabelText(sessionGroup, OracleUIMessages.edit_label_text_date_format, "");
        }

        {
            final Group contentGroup = UIUtils.createControlGroup(cfgGroup, OracleUIMessages.dialog_controlgroup_content, 1, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            hideEmptySchemasCheckbox = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_hide_empty_schemas, false);
            hideEmptySchemasCheckbox.setToolTipText(
            		OracleUIMessages.edit_create_checkbox_hide_empty_schemas_tool_tip_text);

            showDBAAlwaysCheckbox = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_content_group_show, OracleUIMessages.edit_create_checkbox_content_group_show_discription, false, 1);
            useDBAViewsCheckbox = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_content_group_use,  OracleUIMessages.edit_create_checkbox_content_group_use_discription, false, 1);
            useSysSchemaCheckbox = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_content_group_use_sys_schema,  OracleUIMessages.edit_create_checkbox_content_group_use_sys_schema_description, false, 1);
        }

        {
            final Group contentGroup = UIUtils.createControlGroup(cfgGroup, OracleUIMessages.dialog_controlgroup_performance, 1, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            useRuleHint = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_group_use_rule, true);
            useRuleHint.setToolTipText(OracleUIMessages.edit_create_checkbox_adds_rule_tool_tip_text);

            useOptimizerHint = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_group_use_metadata_optimizer, true);
            useOptimizerHint.setToolTipText(OracleUIMessages.edit_create_checkbox_group_use_metadata_optimizer_tip);

            useSimpleConstraints = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_content_group_use_simple_constraints,  OracleUIMessages.edit_create_checkbox_content_group_use_simple_constraints_description, false, 1);
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
    public void loadSettings()
    {
        //oraHomeSelector.setVisible(isOCI);

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        Map<String, String> providerProperties = connectionInfo.getProviderProperties();

        // Settings
        final Object nlsLanguage = providerProperties.get(OracleConstants.PROP_SESSION_LANGUAGE);
        if (nlsLanguage != null) {
            languageCombo.setText(nlsLanguage.toString());
        }

        final Object nlsTerritory = providerProperties.get(OracleConstants.PROP_SESSION_TERRITORY);
        if (nlsTerritory != null) {
            territoryCombo.setText(nlsTerritory.toString());
        }

        final Object dateFormat = providerProperties.get(OracleConstants.PROP_SESSION_NLS_DATE_FORMAT);
        if (dateFormat != null) {
            nlsDateFormat.setText(dateFormat.toString());
        }

        final Object checkSchemaContent = providerProperties.get(OracleConstants.PROP_CHECK_SCHEMA_CONTENT);
        if (checkSchemaContent != null) {
            hideEmptySchemasCheckbox.setSelection(CommonUtils.getBoolean(checkSchemaContent, false));
        }

        showDBAAlwaysCheckbox.setSelection(CommonUtils.getBoolean(providerProperties.get(OracleConstants.PROP_ALWAYS_SHOW_DBA), false));
        useDBAViewsCheckbox.setSelection(CommonUtils.getBoolean(providerProperties.get(OracleConstants.PROP_ALWAYS_USE_DBA_VIEWS), false));
        useSysSchemaCheckbox.setSelection(CommonUtils.getBoolean(providerProperties.get(OracleConstants.PROP_METADATA_USE_SYS_SCHEMA), false));
        useSimpleConstraints.setSelection(CommonUtils.getBoolean(providerProperties.get(OracleConstants.PROP_METADATA_USE_SIMPLE_CONSTRAINTS), false));
        useRuleHint.setSelection(CommonUtils.getBoolean(providerProperties.get(OracleConstants.PROP_USE_RULE_HINT), false));
        useOptimizerHint.setSelection(CommonUtils.getBoolean(providerProperties.get(OracleConstants.PROP_USE_META_OPTIMIZER), false));
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        Map<String, String> providerProperties = dataSource.getConnectionConfiguration().getProviderProperties();

        {
            // Settings
            if (!OracleConstants.NLS_DEFAULT_VALUE.equals(languageCombo.getText())) {
                providerProperties.put(OracleConstants.PROP_SESSION_LANGUAGE, languageCombo.getText());
            } else {
                providerProperties.remove(OracleConstants.PROP_SESSION_LANGUAGE);
            }

            if (!OracleConstants.NLS_DEFAULT_VALUE.equals(territoryCombo.getText())) {
                providerProperties.put(OracleConstants.PROP_SESSION_TERRITORY, territoryCombo.getText());
            } else {
                providerProperties.remove(OracleConstants.PROP_SESSION_TERRITORY);
            }

            String dateFormat = nlsDateFormat.getText().trim();
            if (!dateFormat.isEmpty()) {
                providerProperties.put(OracleConstants.PROP_SESSION_NLS_DATE_FORMAT, dateFormat);
            } else {
                providerProperties.remove(OracleConstants.PROP_SESSION_NLS_DATE_FORMAT);
            }

            providerProperties.put(
                OracleConstants.PROP_CHECK_SCHEMA_CONTENT,
                String.valueOf(hideEmptySchemasCheckbox.getSelection()));
            providerProperties.put(
                OracleConstants.PROP_ALWAYS_SHOW_DBA,
                String.valueOf(showDBAAlwaysCheckbox.getSelection()));
            providerProperties.put(
                OracleConstants.PROP_ALWAYS_USE_DBA_VIEWS,
                String.valueOf(useDBAViewsCheckbox.getSelection()));
            providerProperties.put(
                OracleConstants.PROP_METADATA_USE_SYS_SCHEMA,
                String.valueOf(useSysSchemaCheckbox.getSelection()));
            providerProperties.put(
                    OracleConstants.PROP_METADATA_USE_SYS_SCHEMA,
                    String.valueOf(useSysSchemaCheckbox.getSelection()));
            providerProperties.put(
                    OracleConstants.PROP_METADATA_USE_SIMPLE_CONSTRAINTS,
                    String.valueOf(useSimpleConstraints.getSelection()));

            providerProperties.put(
                OracleConstants.PROP_USE_RULE_HINT,
                String.valueOf(useRuleHint.getSelection()));
            providerProperties.put(
                OracleConstants.PROP_USE_META_OPTIMIZER,
                String.valueOf(useOptimizerHint.getSelection()));

        }
        saveConnectionURL(dataSource.getConnectionConfiguration());
    }

}
