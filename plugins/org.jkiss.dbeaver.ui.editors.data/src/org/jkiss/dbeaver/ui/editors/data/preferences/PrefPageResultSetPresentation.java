/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.data.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.ValueFormatSelector;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageResultSetPresentation
 */
public class PrefPageResultSetPresentation extends TargetPrefPage
{
    static final Log log = Log.getLog(PrefPageResultSetPresentation.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset.presentation"; //$NON-NLS-1$

    private Button autoSwitchMode;
    private Button showDescription;
    private Button columnWidthByValue;
    private Button showConnectionName;
    private Button transformComplexTypes;

    private Button rightJustifyNumbers;
    private Button rightJustifyDateTime;

    private Button gridShowOddRows;
    private Button colorizeDataTypes;
    private Spinner gridRowBatchSize;
    private Button gridShowCellIcons;
    private Button gridShowAttrFilters;
    private Button gridShowAttrOrder;
    private Button gridShowAttrIcons;
    private Combo gridDoubleClickBehavior;

    private Spinner textTabSize;
    private Spinner textMaxColumnSize;
    private ValueFormatSelector textValueFormat;
    private Button showNulls;
    private Button textDelimiterLeading;
    private Button textDelimiterTrailing;
    private Button textExtraSpaces;

    public PrefPageResultSetPresentation()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE) ||
            store.contains(ResultSetPreferences.RESULT_SET_SHOW_DESCRIPTION) ||
            store.contains(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES) ||
            store.contains(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME) ||
            store.contains(ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS) ||
            store.contains(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES) ||
            store.contains(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS) ||
            store.contains(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME) ||
            store.contains(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES) ||
            store.contains(ResultSetPreferences.RESULT_SET_SHOW_CELL_ICONS) ||
            store.contains(ResultSetPreferences.RESULT_SET_DOUBLE_CLICK) ||
            store.contains(ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS) ||
            store.contains(ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE) ||

            store.contains(ResultSetPreferences.RESULT_TEXT_TAB_SIZE) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_MAX_COLUMN_SIZE) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_SHOW_NULLS) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_DELIMITER_TRAILING) ||
            store.contains(ResultSetPreferences.RESULT_TEXT_EXTRA_SPACES)
            ;
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    @Override
    protected Control createPreferenceContent(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        {
            Group uiGroup = UIUtils.createControlGroup(composite, ResultSetMessages.pref_page_database_resultsets_group_common, 1, SWT.NONE, 0);
            ((GridData)uiGroup.getLayoutData()).horizontalSpan = 2;
            autoSwitchMode = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_switch_mode_on_rows, false);
            showDescription = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_show_column_description, false);
            columnWidthByValue = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_calc_column_width_by_values, ResultSetMessages.pref_page_database_resultsets_label_calc_column_width_by_values_tip, false, 1);
            showConnectionName = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_show_connection_name, false);
            transformComplexTypes = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_structurize_complex_types, ResultSetMessages.pref_page_database_resultsets_label_structurize_complex_types_tip, false, 1);
            rightJustifyNumbers = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_right_justify_numbers_and_date, null, false, 1);
            rightJustifyDateTime = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_right_justify_datetime, null, false, 1);
        }

        {
            Group uiGroup = UIUtils.createControlGroup(composite, ResultSetMessages.pref_page_database_resultsets_group_grid, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            gridShowOddRows = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_mark_odd_rows, null, false, 2);
            colorizeDataTypes = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_colorize_data_types, null, false, 2);
            gridRowBatchSize = UIUtils.createLabelSpinner(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_row_batch_size, 1, 1, Short.MAX_VALUE);
            gridRowBatchSize.setToolTipText(ResultSetMessages.pref_page_database_resultsets_label_row_batch_size_tip);
            gridShowCellIcons = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_show_cell_icons, null, false, 2);
            gridShowAttrIcons = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_show_attr_icons, ResultSetMessages.pref_page_database_resultsets_label_show_attr_icons_tip, false, 2);
            gridShowAttrFilters = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_show_attr_filters, ResultSetMessages.pref_page_database_resultsets_label_show_attr_filters_tip, false, 2);
            gridShowAttrOrder = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_show_attr_ordering, ResultSetMessages.pref_page_database_resultsets_label_show_attr_ordering_tip, false, 2);
            gridDoubleClickBehavior = UIUtils.createLabelCombo(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_double_click_behavior, SWT.READ_ONLY);
            gridDoubleClickBehavior.add(ResultSetMessages.pref_page_result_label_none, Spreadsheet.DoubleClickBehavior.NONE.ordinal());
            gridDoubleClickBehavior.add(ResultSetMessages.pref_page_result_label_editor, Spreadsheet.DoubleClickBehavior.EDITOR.ordinal());
            gridDoubleClickBehavior.add(ResultSetMessages.pref_page_result_label_inline_editor, Spreadsheet.DoubleClickBehavior.INLINE_EDITOR.ordinal());
        }

        {
            Group uiGroup = UIUtils.createControlGroup(composite, ResultSetMessages.pref_page_database_resultsets_group_plain_text, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            textTabSize = UIUtils.createLabelSpinner(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_tab_width, 0, 1, 100);
            textMaxColumnSize = UIUtils.createLabelSpinner(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_maximum_column_length, 0, 10, Integer.MAX_VALUE);
            textValueFormat = new ValueFormatSelector(uiGroup);
            showNulls = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_text_show_nulls, null, false, 2);
            textDelimiterLeading = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_text_delimiter_leading, null, false, 2);
            textDelimiterTrailing = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_text_delimiter_trailing, null, false, 2);
            textExtraSpaces = UIUtils.createCheckbox(uiGroup, ResultSetMessages.pref_page_database_resultsets_label_text_extra_spaces, null, false, 2);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            gridShowOddRows.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS));
            colorizeDataTypes.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES));
            rightJustifyNumbers.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS));
            rightJustifyDateTime.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME));
            transformComplexTypes.setSelection(store.getBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES));

            gridRowBatchSize.setSelection(store.getInt(ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE));
            gridShowCellIcons.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_CELL_ICONS));
            gridShowAttrIcons.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ICONS));
            gridShowAttrFilters.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS));
            gridShowAttrOrder.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ORDERING));
            gridDoubleClickBehavior.select(
                Spreadsheet.DoubleClickBehavior.valueOf(store.getString(ResultSetPreferences.RESULT_SET_DOUBLE_CLICK)).ordinal());
            autoSwitchMode.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE));
            showDescription.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_DESCRIPTION));
            columnWidthByValue.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES));
            showConnectionName.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME));

            textTabSize.setSelection(store.getInt(ResultSetPreferences.RESULT_TEXT_TAB_SIZE));
            textMaxColumnSize.setSelection(store.getInt(ResultSetPreferences.RESULT_TEXT_MAX_COLUMN_SIZE));
            textValueFormat.select(DBDDisplayFormat.safeValueOf(store.getString(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT)));
            showNulls.setSelection(store.getBoolean(ResultSetPreferences.RESULT_TEXT_SHOW_NULLS));
            textDelimiterLeading.setSelection(store.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING));
            textDelimiterTrailing.setSelection(store.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_TRAILING));
            textExtraSpaces.setSelection(store.getBoolean(ResultSetPreferences.RESULT_TEXT_EXTRA_SPACES));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS, gridShowOddRows.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES, colorizeDataTypes.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS, rightJustifyNumbers.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME, rightJustifyDateTime.getSelection());
            store.setValue(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES, transformComplexTypes.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE, gridRowBatchSize.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_CELL_ICONS, gridShowCellIcons.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ICONS, gridShowAttrIcons.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS, gridShowAttrFilters.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ORDERING, gridShowAttrOrder.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_DOUBLE_CLICK, CommonUtils.fromOrdinal(Spreadsheet.DoubleClickBehavior.class, gridDoubleClickBehavior.getSelectionIndex()).name());
            store.setValue(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE, autoSwitchMode.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_DESCRIPTION, showDescription.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES, columnWidthByValue.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME, showConnectionName.getSelection());

            store.setValue(ResultSetPreferences.RESULT_TEXT_TAB_SIZE, textTabSize.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_MAX_COLUMN_SIZE, textMaxColumnSize.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT, textValueFormat.getSelection().name());
            store.setValue(ResultSetPreferences.RESULT_TEXT_SHOW_NULLS, showNulls.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING, textDelimiterLeading.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_DELIMITER_TRAILING, textDelimiterLeading.getSelection());
            store.setValue(ResultSetPreferences.RESULT_TEXT_EXTRA_SPACES, textExtraSpaces.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS);
        store.setToDefault(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES);
        store.setToDefault(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS);
        store.setToDefault(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME);
        store.setToDefault(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES);

        store.setToDefault(ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_CELL_ICONS);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ICONS);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ORDERING);
        store.setToDefault(ResultSetPreferences.RESULT_SET_DOUBLE_CLICK);
        store.setToDefault(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_DESCRIPTION);
        store.setToDefault(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME);

        store.setToDefault(ResultSetPreferences.RESULT_TEXT_TAB_SIZE);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_MAX_COLUMN_SIZE);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_SHOW_NULLS);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_DELIMITER_TRAILING);
        store.setToDefault(ResultSetPreferences.RESULT_TEXT_EXTRA_SPACES);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}