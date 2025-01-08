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
package org.jkiss.dbeaver.ui.editors.data.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ValueFormatSelector;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * PrefPageResultSetGrid
 */
public class PrefPageResultSetPresentationGrid extends TargetPrefPage {
    private static final Log log = Log.getLog(PrefPageResultSetPresentationGrid.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset.grid"; //$NON-NLS-1$

    private Button gridShowOddRows;
    private Button gridHighlightRowsWithSelectedCells;
    private Button colorizeDataTypes;
    //private Button gridShowCellIcons;
    private Button gridShowAttrIcons;
    private Button gridShowAttrFilters;
    private Button gridShowAttrOrder;
    private Button useSmoothScrolling;
    private Button showBooleanAsCheckbox;
    private Button showWhitespaceCharacters;
    private ValueFormatSelector textValueFormat;
    private Button toggleBooleanOnClick;
    private Button moveAfterInlineEnter;
    private Combo gridDoubleClickBehavior;
    private Text gridRowBatchSize;
    private Text maxDefColumnWidth;

    public PrefPageResultSetPresentationGrid() {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor) {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS) ||
                store.contains(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES) ||
                //store.contains(ResultSetPreferences.RESULT_SET_SHOW_CELL_ICONS) ||
                store.contains(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ICONS) ||
                store.contains(ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS) ||
                store.contains(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ORDERING) ||
                store.contains(ResultSetPreferences.RESULT_SET_USE_SMOOTH_SCROLLING) ||
                store.contains(ResultSetPreferences.RESULT_SET_SHOW_BOOLEAN_AS_CHECKBOX) ||
                store.contains(ResultSetPreferences.RESULT_SET_SHOW_WHITESPACE_CHARACTERS) ||
                store.contains(ResultSetPreferences.RESULT_SET_CLICK_TOGGLE_BOOLEAN) ||
                store.contains(ResultSetPreferences.RESULT_SET_DOUBLE_CLICK) ||
                store.contains(ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE) ||
                store.contains(ResultSetPreferences.RESULT_SET_MAX_COLUMN_DEF_WIDTH);
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        {
            Group uiGroup = UIUtils.createControlGroup(composite,
                DataEditorsMessages.pref_page_database_resultsets_group_grid, 2, GridData.FILL_BOTH, 0);

            final Group appearanceGroup = UIUtils.createControlGroup(uiGroup,
                DataEditorsMessages.pref_page_database_resultsets_group_appearance, 2,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
            gridShowOddRows = UIUtils.createCheckbox(appearanceGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_mark_odd_rows, null, false, 2);
            colorizeDataTypes = UIUtils.createCheckbox(appearanceGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_colorize_data_types, null, false, 2);
            //gridShowCellIcons = UIUtils.createCheckbox(uiGroup, DataEditorsMessages.pref_page_database_resultsets_label_show_cell_icons, null, false, 2);
            gridShowAttrIcons = UIUtils.createCheckbox(appearanceGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_show_attr_icons,
                DataEditorsMessages.pref_page_database_resultsets_label_show_attr_icons_tip, false, 2);
            gridShowAttrFilters = UIUtils.createCheckbox(appearanceGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_show_attr_filters,
                DataEditorsMessages.pref_page_database_resultsets_label_show_attr_filters_tip, false, 2);
            gridShowAttrOrder = UIUtils.createCheckbox(appearanceGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_show_attr_ordering,
                DataEditorsMessages.pref_page_database_resultsets_label_show_attr_ordering_tip, false, 2);
            showBooleanAsCheckbox = UIUtils.createCheckbox(appearanceGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_show_boolean_as_checkbox,
                DataEditorsMessages.pref_page_database_resultsets_label_show_boolean_as_checkbox_tip, false, 2);
            showWhitespaceCharacters = UIUtils.createCheckbox(appearanceGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_show_whitespace_characters,
                DataEditorsMessages.pref_page_database_resultsets_label_show_whitespace_characters_tip, false, 2);
            textValueFormat = new ValueFormatSelector(appearanceGroup);
            maxDefColumnWidth = UIUtils.createLabelText(appearanceGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_max_def_column_width, "", SWT.BORDER);
            maxDefColumnWidth.setToolTipText(
                DataEditorsMessages.pref_page_database_resultsets_label_max_def_column_width_tip);
            maxDefColumnWidth.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));

            UIUtils.createPreferenceLink(
                appearanceGroup,
                DataEditorsMessages.pref_page_database_resultsets_link_colors_and_fonts,
                EditorUtils.COLORS_AND_FONTS_PAGE_ID,
                (IWorkbenchPreferenceContainer) getContainer(), null
            ).setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());

            final Group behaviorGroup = UIUtils.createControlGroup(uiGroup,
                DataEditorsMessages.pref_page_database_resultsets_group_behavior, 2,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
            gridHighlightRowsWithSelectedCells = UIUtils.createCheckbox(behaviorGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_highlight_rows_with_selected_cells, null, false,
                2);
            useSmoothScrolling = UIUtils.createCheckbox(behaviorGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_use_smooth_scrolling,
                DataEditorsMessages.pref_page_database_resultsets_label_use_smooth_scrolling_tip, false, 2);
            toggleBooleanOnClick = UIUtils.createCheckbox(behaviorGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_toggle_boolean_on_click,
                DataEditorsMessages.pref_page_database_resultsets_label_toggle_boolean_on_click_tip, false, 2);
            moveAfterInlineEnter = UIUtils.createCheckbox(behaviorGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_enter_for_inline_behavior,
                DataEditorsMessages.pref_page_database_resultsets_label_enter_for_inline_behavior_tip, false, 2);

            UIUtils.createPreferenceLink(
                behaviorGroup,
                "<a>" + DataEditorsMessages.pref_page_database_resultsets_label_show_boolean_config_link + "  - ''{0}''</a>",
                "org.jkiss.dbeaver.preferences.main.misc",
                (IWorkbenchPreferenceContainer) getContainer(), null
            ).setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());

            gridDoubleClickBehavior = UIUtils.createLabelCombo(behaviorGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_double_click_behavior, SWT.READ_ONLY);
            gridDoubleClickBehavior.add(DataEditorsMessages.pref_page_result_selector_none,
                Spreadsheet.DoubleClickBehavior.NONE.ordinal());
            gridDoubleClickBehavior.add(DataEditorsMessages.pref_page_result_selector_editor,
                Spreadsheet.DoubleClickBehavior.EDITOR.ordinal());
            gridDoubleClickBehavior.add(DataEditorsMessages.pref_page_result_selector_inline_editor,
                Spreadsheet.DoubleClickBehavior.INLINE_EDITOR.ordinal());
            gridDoubleClickBehavior.add(DataEditorsMessages.pref_page_result_selector_copy_cell,
                Spreadsheet.DoubleClickBehavior.COPY_VALUE.ordinal());
            gridDoubleClickBehavior.add(DataEditorsMessages.pref_page_result_selector_paste_cell_value,
                Spreadsheet.DoubleClickBehavior.COPY_PASTE_VALUE.ordinal());


            gridRowBatchSize = UIUtils.createLabelText(behaviorGroup,
                DataEditorsMessages.pref_page_database_resultsets_label_row_batch_size, "", SWT.BORDER);
            gridRowBatchSize.setToolTipText(DataEditorsMessages.pref_page_database_resultsets_label_row_batch_size_tip);

        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store) {
        try {
            gridShowOddRows.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS));
            gridHighlightRowsWithSelectedCells.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_HIGHLIGHT_SELECTED_ROWS));
            colorizeDataTypes.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES));
            gridShowAttrIcons.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ICONS));
            gridShowAttrFilters.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS));
            gridShowAttrOrder.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ORDERING));
            useSmoothScrolling.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_USE_SMOOTH_SCROLLING));
            showBooleanAsCheckbox.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_BOOLEAN_AS_CHECKBOX));
            showWhitespaceCharacters.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_WHITESPACE_CHARACTERS));
            textValueFormat.select(CommonUtils.valueOf(DBDDisplayFormat.class, store.getString(ResultSetPreferences.RESULT_GRID_VALUE_FORMAT), DBDDisplayFormat.UI));
            toggleBooleanOnClick.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_CLICK_TOGGLE_BOOLEAN));
            moveAfterInlineEnter.setSelection(store.getBoolean(ResultSetPreferences.RESULT_SET_INLINE_ENTER));
            gridDoubleClickBehavior.select(
                CommonUtils.valueOf(
                        Spreadsheet.DoubleClickBehavior.class,
                        store.getString(ResultSetPreferences.RESULT_SET_DOUBLE_CLICK),
                        Spreadsheet.DoubleClickBehavior.NONE)
                    .ordinal());
            gridRowBatchSize.setText(store.getString(ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE));
            maxDefColumnWidth.setText(store.getString(ResultSetPreferences.RESULT_SET_MAX_COLUMN_DEF_WIDTH));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store) {
        try {
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS, gridShowOddRows.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_INLINE_ENTER, moveAfterInlineEnter.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_HIGHLIGHT_SELECTED_ROWS, gridHighlightRowsWithSelectedCells.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES, colorizeDataTypes.getSelection());
            //store.setValue(ResultSetPreferences.RESULT_SET_SHOW_CELL_ICONS, gridShowCellIcons.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ICONS, gridShowAttrIcons.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS, gridShowAttrFilters.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ORDERING, gridShowAttrOrder.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_USE_SMOOTH_SCROLLING, useSmoothScrolling.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_BOOLEAN_AS_CHECKBOX, showBooleanAsCheckbox.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_SHOW_WHITESPACE_CHARACTERS, showWhitespaceCharacters.getSelection());
            store.setValue(ResultSetPreferences.RESULT_GRID_VALUE_FORMAT, textValueFormat.getSelection().name());
            store.setValue(ResultSetPreferences.RESULT_SET_CLICK_TOGGLE_BOOLEAN, toggleBooleanOnClick.getSelection());
            store.setValue(ResultSetPreferences.RESULT_SET_DOUBLE_CLICK, CommonUtils.fromOrdinal(
                Spreadsheet.DoubleClickBehavior.class, gridDoubleClickBehavior.getSelectionIndex()).name());
            store.setValue(ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE, CommonUtils.toInt(gridRowBatchSize.getText()));
            int maxColumnWidth = CommonUtils.toInt(maxDefColumnWidth.getText());
            maxColumnWidth = maxColumnWidth < 0 ? 0 : maxColumnWidth > 100 ? 100 : maxColumnWidth;
            store.setValue(ResultSetPreferences.RESULT_SET_MAX_COLUMN_DEF_WIDTH, maxColumnWidth);
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store) {
        store.setToDefault(ResultSetPreferences.RESULT_SET_INLINE_ENTER);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS);
        store.setToDefault(ResultSetPreferences.RESULT_SET_HIGHLIGHT_SELECTED_ROWS);
        store.setToDefault(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ICONS);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ORDERING);
        store.setToDefault(ResultSetPreferences.RESULT_SET_USE_SMOOTH_SCROLLING);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_BOOLEAN_AS_CHECKBOX);
        store.setToDefault(ResultSetPreferences.RESULT_SET_SHOW_WHITESPACE_CHARACTERS);
        store.setToDefault(ResultSetPreferences.RESULT_GRID_VALUE_FORMAT);
        store.setToDefault(ResultSetPreferences.RESULT_SET_CLICK_TOGGLE_BOOLEAN);
        store.setToDefault(ResultSetPreferences.RESULT_SET_DOUBLE_CLICK);
        store.setToDefault(ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE);
        store.setToDefault(ResultSetPreferences.RESULT_SET_MAX_COLUMN_DEF_WIDTH);
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        gridShowOddRows.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS));
        gridHighlightRowsWithSelectedCells.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_HIGHLIGHT_SELECTED_ROWS));
        colorizeDataTypes.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES));
        gridShowAttrIcons.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ICONS));
        gridShowAttrFilters.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS));
        gridShowAttrOrder.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ORDERING));
        useSmoothScrolling.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_USE_SMOOTH_SCROLLING));
        showBooleanAsCheckbox.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_SHOW_BOOLEAN_AS_CHECKBOX));
        showWhitespaceCharacters.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_SHOW_WHITESPACE_CHARACTERS));
        textValueFormat.select(DBDDisplayFormat.UI);
        toggleBooleanOnClick.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_CLICK_TOGGLE_BOOLEAN));
        moveAfterInlineEnter.setSelection(store.getDefaultBoolean(ResultSetPreferences.RESULT_SET_INLINE_ENTER));
        UIUtils.setComboSelection(gridDoubleClickBehavior, store.getDefaultString(ResultSetPreferences.RESULT_SET_DOUBLE_CLICK));
        gridRowBatchSize.setText(String.valueOf(store.getDefaultInt(ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE)));
        maxDefColumnWidth.setText(String.valueOf(store.getDefaultInt(ResultSetPreferences.RESULT_SET_MAX_COLUMN_DEF_WIDTH)));
        super.performDefaults();
    }

    @Override
    protected String getPropertyPageID() {
        return PAGE_ID;
    }

}