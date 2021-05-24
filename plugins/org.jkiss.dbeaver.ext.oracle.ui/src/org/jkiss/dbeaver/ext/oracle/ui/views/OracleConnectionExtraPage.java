/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
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
    private Text nlsTimestampFormat;
    private Text nlsLengthFormat;
    private Text nlsCurrencyFormat;
    private Button hideEmptySchemasCheckbox;
    private Button showDBAAlwaysCheckbox;
    private Button useDBAViewsCheckbox;
    private Button useSysSchemaCheckbox;
    private Button useRuleHint;
    private Button useOptimizerHint;
    private Button useSimpleConstraints;
    private Button useAlternativeTableMetadataQuery;
    private Button searchInSynonyms;

    public OracleConnectionExtraPage()
    {
        setTitle(OracleUIMessages.dialog_connection_oracle_properties);
        setDescription(OracleUIMessages.dialog_connection_oracle_properties_description);
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
        GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        cfgGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        cfgGroup.setLayoutData(gd);

        {
            final Group sessionGroup = UIUtils.createControlGroup(cfgGroup, OracleUIMessages.dialog_controlgroup_session_settings, 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);
            ((GridData)sessionGroup.getLayoutData()).horizontalSpan = 2;

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
            nlsTimestampFormat = UIUtils.createLabelText(sessionGroup, OracleUIMessages.edit_label_text_timestamp_format, "");
            nlsLengthFormat = UIUtils.createLabelText(sessionGroup, OracleUIMessages.edit_label_text_length_format, "");
            nlsCurrencyFormat = UIUtils.createLabelText(sessionGroup, OracleUIMessages.edit_label_text_currency_format, "");
        }

        {
            final Group contentGroup = UIUtils.createControlGroup(cfgGroup, OracleUIMessages.dialog_controlgroup_performance, 1, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            useRuleHint = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_group_use_rule, true);
            useRuleHint.setToolTipText(OracleUIMessages.edit_create_checkbox_adds_rule_tool_tip_text);

            useOptimizerHint = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_group_use_metadata_optimizer, true);
            useOptimizerHint.setToolTipText(OracleUIMessages.edit_create_checkbox_group_use_metadata_optimizer_tip);

            useSimpleConstraints = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_content_group_use_simple_constraints,  OracleUIMessages.edit_create_checkbox_content_group_use_simple_constraints_description, false, 1);

            useAlternativeTableMetadataQuery = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_content_group_use_another_table_query, false);
            useAlternativeTableMetadataQuery.setToolTipText(OracleUIMessages.edit_create_checkbox_content_group_use_another_table_query_description);

            searchInSynonyms = UIUtils.createCheckbox(
                contentGroup,
                OracleUIMessages.edit_create_checkbox_content_group_search_metadata_in_synonyms,
                false
            );
            searchInSynonyms.setToolTipText(OracleUIMessages.edit_create_checkbox_content_group_search_metadata_in_synonyms_tooltip);
        }

        {
            final Group contentGroup = UIUtils.createControlGroup(
                cfgGroup,
                OracleUIMessages.dialog_controlgroup_content,
                1,
                GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING,
                0
            );

            hideEmptySchemasCheckbox = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_hide_empty_schemas, false);
            hideEmptySchemasCheckbox.setToolTipText(OracleUIMessages.edit_create_checkbox_hide_empty_schemas_tool_tip_text);

            showDBAAlwaysCheckbox = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_content_group_show, OracleUIMessages.edit_create_checkbox_content_group_show_description, false, 1);
            useDBAViewsCheckbox = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_content_group_use,  OracleUIMessages.edit_create_checkbox_content_group_use_description, false, 1);
            useSysSchemaCheckbox = UIUtils.createCheckbox(contentGroup, OracleUIMessages.edit_create_checkbox_content_group_use_sys_schema,  OracleUIMessages.edit_create_checkbox_content_group_use_sys_schema_description, false, 1);
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
        DBPPreferenceStore globalPreferences = DBWorkbench.getPlatform().getPreferenceStore();
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

        nlsDateFormat.setText(CommonUtils.toString(providerProperties.get(OracleConstants.PROP_SESSION_NLS_DATE_FORMAT)));
        nlsTimestampFormat.setText(CommonUtils.toString(providerProperties.get(OracleConstants.PROP_SESSION_NLS_TIMESTAMP_FORMAT)));
        nlsLengthFormat.setText(CommonUtils.toString(providerProperties.get(OracleConstants.PROP_SESSION_NLS_LENGTH_FORMAT)));
        nlsCurrencyFormat.setText(CommonUtils.toString(providerProperties.get(OracleConstants.PROP_SESSION_NLS_CURRENCY_FORMAT)));

        final Object checkSchemaContent = providerProperties.get(OracleConstants.PROP_CHECK_SCHEMA_CONTENT);
        if (checkSchemaContent != null) {
            hideEmptySchemasCheckbox.setSelection(CommonUtils.getBoolean(checkSchemaContent, false));
        }

        showDBAAlwaysCheckbox.setSelection(CommonUtils.getBoolean(providerProperties.get(OracleConstants.PROP_ALWAYS_SHOW_DBA), false));
        useDBAViewsCheckbox.setSelection(CommonUtils.getBoolean(providerProperties.get(OracleConstants.PROP_ALWAYS_USE_DBA_VIEWS), false));
        useSysSchemaCheckbox.setSelection(CommonUtils.getBoolean(providerProperties.get(OracleConstants.PROP_METADATA_USE_SYS_SCHEMA), false));

        useSimpleConstraints.setSelection(CommonUtils.getBoolean(
            providerProperties.get(OracleConstants.PROP_METADATA_USE_SIMPLE_CONSTRAINTS),
            globalPreferences.getBoolean(OracleConstants.PROP_METADATA_USE_SIMPLE_CONSTRAINTS)
        ));
        useRuleHint.setSelection(CommonUtils.getBoolean(
            providerProperties.get(OracleConstants.PROP_USE_RULE_HINT),
            globalPreferences.getBoolean(OracleConstants.PROP_USE_RULE_HINT)
        ));
        useOptimizerHint.setSelection(CommonUtils.getBoolean(
            providerProperties.get(OracleConstants.PROP_USE_META_OPTIMIZER),
            globalPreferences.getBoolean(OracleConstants.PROP_USE_META_OPTIMIZER)
        ));
        useAlternativeTableMetadataQuery.setSelection(CommonUtils.getBoolean(
            providerProperties.get(OracleConstants.PROP_METADATA_USE_ALTERNATIVE_TABLE_QUERY),
            globalPreferences.getBoolean(OracleConstants.PROP_METADATA_USE_ALTERNATIVE_TABLE_QUERY)
        ));
        searchInSynonyms.setSelection(CommonUtils.getBoolean(
            providerProperties.get(OracleConstants.PROP_SEARCH_METADATA_IN_SYNONYMS),
            globalPreferences.getBoolean(OracleConstants.PROP_SEARCH_METADATA_IN_SYNONYMS)
        ));
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

            setOrRemoveProperty(nlsDateFormat, OracleConstants.PROP_SESSION_NLS_DATE_FORMAT, providerProperties);
            setOrRemoveProperty(nlsTimestampFormat, OracleConstants.PROP_SESSION_NLS_TIMESTAMP_FORMAT, providerProperties);
            setOrRemoveProperty(nlsLengthFormat, OracleConstants.PROP_SESSION_NLS_LENGTH_FORMAT, providerProperties);
            setOrRemoveProperty(nlsCurrencyFormat, OracleConstants.PROP_SESSION_NLS_CURRENCY_FORMAT, providerProperties);

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
                    OracleConstants.PROP_METADATA_USE_SIMPLE_CONSTRAINTS,
                    String.valueOf(useSimpleConstraints.getSelection()));

            providerProperties.put(
                OracleConstants.PROP_USE_RULE_HINT,
                String.valueOf(useRuleHint.getSelection()));
            providerProperties.put(
                OracleConstants.PROP_USE_META_OPTIMIZER,
                String.valueOf(useOptimizerHint.getSelection()));
            providerProperties.put(
                    OracleConstants.PROP_METADATA_USE_ALTERNATIVE_TABLE_QUERY,
                    String.valueOf(useAlternativeTableMetadataQuery.getSelection()));
            providerProperties.put(OracleConstants.PROP_SEARCH_METADATA_IN_SYNONYMS, String.valueOf(searchInSynonyms.getSelection()));
        }
        saveConnectionURL(dataSource.getConnectionConfiguration());
    }

    private static void setOrRemoveProperty(Text text, String propName, Map<String, String> providerProperties) {
        String propValue = text.getText().trim();
        if (!propValue.isEmpty()) {
            providerProperties.put(propName, propValue);
        } else {
            providerProperties.remove(propName);
        }
    }

}
