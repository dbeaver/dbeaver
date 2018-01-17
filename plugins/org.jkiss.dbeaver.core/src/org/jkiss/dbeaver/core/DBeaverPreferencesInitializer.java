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
package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMConstants;
import org.jkiss.dbeaver.model.qm.QMObjectType;
import org.jkiss.dbeaver.registry.formatter.DataFormatterProfile;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

public class DBeaverPreferencesInitializer extends AbstractPreferenceInitializer {

    public DBeaverPreferencesInitializer() {
    }

    @Override
    public void initializeDefaultPreferences() {
        boolean isWindows = RuntimeUtils.isPlatformWindows();

        // Init default preferences
        DBPPreferenceStore store = new BundlePreferenceStore(DBeaverActivator.getInstance().getBundle());

        // Resources
        //PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.DEFAULT_RESOURCE_ENCODING, GeneralUtils.UTF8_ENCODING);

        // Agent
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY, isWindows);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT, 30);

        // Navigator
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_EXPAND_ON_CONNECT, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_SYNC_EDITOR_DATASOURCE, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_REFRESH_EDITORS_ON_OPEN, false);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_GROUP_BY_DRIVER, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_OBJECT_DOUBLE_CLICK, NavigatorViewBase.DoubleClickBehavior.EDIT.name());
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK, NavigatorViewBase.DoubleClickBehavior.EXPAND.name());
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_SHOW_SQL_PREVIEW, true);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_SHOW_OBJECT_TIPS, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.ENTITY_EDITOR_DETACH_INFO, true);

        // Common
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.KEEP_STATEMENT_OPEN, false);

        // SQL execution
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.NO_COMMIT.name());
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_COMMIT_LINES, 1000);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.STOP_ROLLBACK.name());
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS, true);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_DELETE_EMPTY, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_AUTO_FOLDERS, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_CREATE_CONNECTION_FOLDERS, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_TITLE_PATTERN, SQLEditor.DEFAULT_TITLE_PATTERN);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.STATEMENT_TIMEOUT, 0);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.READ_EXPENSIVE_PROPERTIES, false);
        // Disable separate connection by default. Otherwise many people don't understand what happens (data editor and SQL editor have different contexts)
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.EDITOR_SEPARATE_CONNECTION, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.EDITOR_CONNECT_ON_ACTIVATE, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.EDITOR_CONNECT_ON_EXECUTE, false);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_USE_ALL_COLUMNS, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE, 10 * 1000000);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_LONG_AS_LOB, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_AUTO_UPDATE_VALUE, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_COMMIT_ON_EDIT_APPLY, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_COMMIT_ON_CONTENT_APPLY, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_NEW_ROWS_AFTER, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_REFRESH_AFTER_UPDATE, true);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.TEXT_EDIT_UNDO_LEVEL, 200);

        {
            // SQL prefs
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE, false);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, 0);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_INSERT_CASE, SQLPreferenceConstants.PROPOSAL_CASE_DEFAULT);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_SHORT_NAME, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_ALWAYS_FQ, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.USE_GLOBAL_ASSISTANT, false);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_COMMENTS, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BEGIN_END, true);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE, true);
        }

        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE, true);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.BEEP_ON_QUERY_END, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE, false);

        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.RESULT_SET_ORIENTATION, SQLEditor.ResultSetOrientation.HORIZONTAL.name());

        // Text editor default preferences
        PrefUtils.setDefaultPreferenceValue(store, AbstractTextEditor.PREFERENCE_TEXT_DRAG_AND_DROP_ENABLED, true);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.HEX_FONT_NAME, HexEditControl.DEFAULT_FONT_NAME);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.HEX_FONT_SIZE, 10);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.HEX_DEF_WIDTH, 8);

        // General UI
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_AUTO_UPDATE_CHECK, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_KEEP_DATABASE_EDITORS, true);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_HOST, "");
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_PORT, 1080);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_USER, "");
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_PASSWORD, "");
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_DRIVERS_VERSION_UPDATE, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_DRIVERS_HOME, "");
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_DRIVERS_SOURCES, "https://dbeaver.jkiss.org/files/jdbc/");

        // ResultSet
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_READ_METADATA, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_READ_REFERENCES, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_MAX_ROWS, 200);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_CANCEL_TIMEOUT, 5000);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE, IValueController.EditType.EDITOR);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_SHOW_ATTR_FILTERS, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_DOUBLE_CLICK, Spreadsheet.DoubleClickBehavior.INLINE_EDITOR.name());
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_SHOW_DESCRIPTION, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_SHOW_CONNECTION_NAME, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_COLORIZE_DATA_TYPES, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_ROW_BATCH_SIZE, 1);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_STRING_USE_CONTENT_EDITOR, false);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_TEXT_MAX_COLUMN_SIZE, 255);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_TEXT_VALUE_FORMAT, DBDDisplayFormat.EDIT.name());
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_TEXT_SHOW_NULLS, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_TEXT_DELIMITER_LEADING, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_TEXT_DELIMITER_TRAILING, true);

        // QM
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_HISTORY_DAYS, 90);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_ENTRIES_PER_PAGE, 200);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_OBJECT_TYPES,
            QMObjectType.toString(Arrays.asList(QMObjectType.txn, QMObjectType.query)));
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_QUERY_TYPES,
            DBCExecutionPurpose.USER + "," + DBCExecutionPurpose.USER_FILTERED + "," + DBCExecutionPurpose.USER_SCRIPT);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_STORE_LOG_FILE, false);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_LOG_DIRECTORY, GeneralUtils.getMetadataFolder().getAbsolutePath());

        // Data formats
        DataFormatterProfile.initDefaultPreferences(store, Locale.getDefault());

        // Logs
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.LOGS_DEBUG_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.LOGS_DEBUG_LOCATION,
                "${" + SystemVariablesResolver.VAR_WORKSPACE + "}" + File.separator + ".metadata" + File.separator + "dbeaver-debug.log");
    }

}
