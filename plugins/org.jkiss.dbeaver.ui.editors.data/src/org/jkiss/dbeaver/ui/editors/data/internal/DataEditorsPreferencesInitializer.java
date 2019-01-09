/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.data.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.utils.PrefUtils;

public class DataEditorsPreferencesInitializer extends AbstractPreferenceInitializer {

    public DataEditorsPreferencesInitializer() {
    }

    @Override
    public void initializeDefaultPreferences() {
        // Init default preferences
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        // Common
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.KEEP_STATEMENT_OPEN, false);

        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RS_EDIT_USE_ALL_COLUMNS, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RS_EDIT_MAX_TEXT_SIZE, 10 * 1000000);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RS_EDIT_LONG_AS_LOB, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RS_EDIT_AUTO_UPDATE_VALUE, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RS_COMMIT_ON_EDIT_APPLY, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RS_COMMIT_ON_CONTENT_APPLY, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RS_EDIT_NEW_ROWS_AFTER, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RS_EDIT_REFRESH_AFTER_UPDATE, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RS_GROUPING_DEFAULT_SORTING, "");
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RS_GROUPING_SHOW_DUPLICATES_ONLY, false);

        // ResultSet
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_REREAD_ON_SCROLLING, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_READ_METADATA, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_READ_REFERENCES, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_MAX_ROWS, 200);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_CANCEL_TIMEOUT, 5000);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_BINARY_EDITOR_TYPE, IValueController.EditType.EDITOR);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_ORDER_SERVER_SIDE, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_SHOW_CELL_ICONS, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_SHOW_ATTR_ICONS, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_SHOW_ATTR_ORDERING, true);

        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_DOUBLE_CLICK, Spreadsheet.DoubleClickBehavior.INLINE_EDITOR.name());
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_SHOW_DESCRIPTION, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME, true);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE, 1);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_STRING_USE_CONTENT_EDITOR, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_SET_USE_NAVIGATOR_FILTERS, true);

        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_TEXT_TAB_SIZE, 4);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_TEXT_MAX_COLUMN_SIZE, 255);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT, DBDDisplayFormat.EDIT.name());
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_TEXT_SHOW_NULLS, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING, false);
        PrefUtils.setDefaultPreferenceValue(store, ResultSetPreferences.RESULT_TEXT_DELIMITER_TRAILING, true);
    }

}
