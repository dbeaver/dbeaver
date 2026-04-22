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
package org.jkiss.dbeaver.ui.editors.data.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageResultSetPresentation
 */
public class PrefPageResultSetPresentation extends TargetPrefPage {
    private static final Log log = Log.getLog(PrefPageResultSetPresentation.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset.presentation"; //$NON-NLS-1$

    private Button autoSwitchMode;
    private Button showFiltersInSingleTabMode;
    private Combo columnHeaderExtra;
    private Button columnWidthByValue;
    private Button showConnectionName;
    private Button transformComplexTypes;
    private Button rightJustifyNumbers;
    private Button rightJustifyDateTime;
    private Button autoCompleteProposal;

    public PrefPageResultSetPresentation() {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(@NotNull DBPDataSourceContainer dataSourceDescriptor) {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE) ||
            store.contains(ResultSetPreferences.RESULT_SET_SHOW_FILTERS_IN_SINGLE_TAB_MODE) ||
            store.contains(ResultSetPreferences.RESULT_SET_COLUMN_HEADER_EXTRA) ||
            store.contains(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES) ||
            store.contains(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME) ||
            store.contains(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES) ||
            store.contains(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS) ||
            store.contains(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME) ||
            store.contains(ResultSetPreferences.RESULT_SET_FILTER_AUTO_COMPLETE_PROPOSIAL);
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            Composite uiGroup = UIUtils.createTitledComposite(composite, DataEditorsMessages.pref_page_database_resultsets_group_common, 2);
            autoSwitchMode = UIUtils.createCheckbox(uiGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_switch_mode_on_rows, null, false, 2);
            showFiltersInSingleTabMode = UIUtils.createCheckbox(uiGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_filters_panel_in_singletab_mode,
                null, true, 2);
            columnHeaderExtra = UIUtils.createLabelCombo(uiGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_column_header_extra, SWT.READ_ONLY
            );
            columnHeaderExtra.add(
                DataEditorsMessages.pref_page_database_resultsets_label_column_header_extra_nothing,
                ResultSetPreferences.ColumnHeaderExtraContent.NOTHING.ordinal()
            );
            columnHeaderExtra.add(
                DataEditorsMessages.pref_page_database_resultsets_label_column_header_extra_description,
                ResultSetPreferences.ColumnHeaderExtraContent.DESCRIPTION.ordinal()
            );
            columnHeaderExtra.add(
                DataEditorsMessages.pref_page_database_resultsets_label_column_header_extra_data_type,
                ResultSetPreferences.ColumnHeaderExtraContent.DATA_TYPE.ordinal()
            );
            columnWidthByValue = UIUtils.createCheckbox(uiGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_calc_column_width_by_values,
                DataEditorsMessages.pref_page_database_resultsets_label_calc_column_width_by_values_tip,
                false, 2);
            showConnectionName = UIUtils.createCheckbox(uiGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_show_connection_name,
                null, false, 2);
            transformComplexTypes = UIUtils.createCheckbox(uiGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_structurize_complex_types,
                DataEditorsMessages.pref_page_database_resultsets_label_structurize_complex_types_tip,
                false, 2);
            rightJustifyNumbers = UIUtils.createCheckbox(uiGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_right_justify_numbers_and_date,
                null, false, 2);
            rightJustifyDateTime = UIUtils.createCheckbox(uiGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_right_justify_datetime,
                null, false, 2);
            autoCompleteProposal = UIUtils.createCheckbox(uiGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_auto_completion,
                DataEditorsMessages.pref_page_database_resultsets_label_auto_completion_tip,
                true, 2);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(@NotNull DBPPreferenceStore store) {
        try {
            autoSwitchMode.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE));
            showFiltersInSingleTabMode.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_FILTERS_IN_SINGLE_TAB_MODE));
            columnHeaderExtra.select(
                CommonUtils.valueOf(
                    ResultSetPreferences.ColumnHeaderExtraContent.class,
                    store.getString(ResultSetPreferences.RESULT_SET_COLUMN_HEADER_EXTRA),
                    ResultSetPreferences.ColumnHeaderExtraContent.NOTHING
                ).ordinal()
            );
            columnWidthByValue.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES));
            showConnectionName.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME));
            rightJustifyNumbers.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS));
            rightJustifyDateTime.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME));
            transformComplexTypes.setSelection(store.getBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES));
            autoCompleteProposal.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_FILTER_AUTO_COMPLETE_PROPOSIAL));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(@NotNull DBPPreferenceStore store) {
        try {
            store.setValue(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE, autoSwitchMode.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_FILTERS_IN_SINGLE_TAB_MODE, showFiltersInSingleTabMode.getSelection());
            int selectedIndex = Math.max(0, columnHeaderExtra.getSelectionIndex());
            store.setValue(ResultSetPreferences.RESULT_SET_COLUMN_HEADER_EXTRA,
                ResultSetPreferences.ColumnHeaderExtraContent.values()[selectedIndex].name()
            );
            store.setValue(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES, columnWidthByValue.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME, showConnectionName.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS, rightJustifyNumbers.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME, rightJustifyDateTime.getSelection());
            store.setValue(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES, transformComplexTypes.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_FILTER_AUTO_COMPLETE_PROPOSIAL, autoCompleteProposal.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(@NotNull DBPPreferenceStore store) {
        store.setToDefault(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_FILTERS_IN_SINGLE_TAB_MODE);
        store.setToDefault(ResultSetPreferences.RESULT_SET_COLUMN_HEADER_EXTRA);
        store.setToDefault(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME);
        store.setToDefault(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS);
        store.setToDefault(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME);
        store.setToDefault(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES);  
        store.setToDefault(ResultSetPreferences.RESULT_SET_FILTER_AUTO_COMPLETE_PROPOSIAL);
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        autoSwitchMode.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE));
        showFiltersInSingleTabMode.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_SHOW_FILTERS_IN_SINGLE_TAB_MODE));
        columnHeaderExtra.select(
            CommonUtils.valueOf(
                ResultSetPreferences.ColumnHeaderExtraContent.class,
                store.getDefaultString(ResultSetPreferences.RESULT_SET_COLUMN_HEADER_EXTRA),
                ResultSetPreferences.ColumnHeaderExtraContent.NOTHING
            ).ordinal()
        );
        columnWidthByValue.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES));
        showConnectionName.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME));
        transformComplexTypes.setSelection(store.getDefaultBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES));
        rightJustifyNumbers.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS));
        rightJustifyDateTime.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME));
        autoCompleteProposal.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_FILTER_AUTO_COMPLETE_PROPOSIAL));
        super.performDefaults();
    }

    @NotNull
    @Override
    protected String getPropertyPageID() {
        return PAGE_ID;
    }

}