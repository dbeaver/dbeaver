/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

public class ModelPreferencesInitializer extends AbstractPreferenceInitializer {

  public ModelPreferencesInitializer() {
  }

  @Override
  public void initializeDefaultPreferences() {
      boolean isMac = Platform.getOS().toLowerCase().contains("macos");

/*
      // Init default preferences
      DBPPreferenceStore store = new BundlePreferenceStore(DBeaverActivator.getInstance().getBundle());

      // Agent
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.AGENT_ENABLED, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.AGENT_LONG_OPERATION_NOTIFY, !isMac);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.AGENT_LONG_OPERATION_TIMEOUT, 30);

      // Navigator
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.NAVIGATOR_EXPAND_ON_CONNECT, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.NAVIGATOR_GROUP_BY_DRIVER, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.NAVIGATOR_EDITOR_FULL_NAME, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK, NavigatorViewBase.DoubleClickBehavior.SQL_EDITOR.name());

      // Common
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.KEEP_STATEMENT_OPEN, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.QUERY_ROLLBACK_ON_ERROR, false);

      // SQL execution
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.NO_COMMIT.name());
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.SCRIPT_COMMIT_LINES, 1000);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.STOP_ROLLBACK.name());
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.SCRIPT_FETCH_RESULT_SETS, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.SCRIPT_STATEMENT_DELIMITER, SQLConstants.DEFAULT_STATEMENT_DELIMITER);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.SCRIPT_AUTO_FOLDERS, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.SCRIPT_TITLE_PATTERN, SQLEditorInput.DEFAULT_PATTERN);

      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.SQL_PARAMETERS_ENABLED, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.SQL_PARAMETERS_MARK, "?");

      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.STATEMENT_TIMEOUT, 10 * 1000);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.MEMORY_CONTENT_MAX_SIZE, 10000);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.READ_EXPENSIVE_PROPERTIES, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.META_SEPARATE_CONNECTION, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.META_CASE_SENSITIVE, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.EDITOR_SEPARATE_CONNECTION, false);

      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RS_EDIT_USE_ALL_COLUMNS, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RS_EDIT_MAX_TEXT_SIZE, 10 * 1000000);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RS_EDIT_LONG_AS_LOB, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.CONTENT_HEX_ENCODING, GeneralUtils.getDefaultFileEncoding());
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RS_COMMIT_ON_EDIT_APPLY, false);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RS_COMMIT_ON_CONTENT_APPLY, false);

      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.TEXT_EDIT_UNDO_LEVEL, 200);

      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, true);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, 500);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, true);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_INSERT_CASE, SQLPreferenceConstants.PROPOSAL_CASE_DEFAULT);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS, false);

      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, true);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, true);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, true);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_COMMENTS, true);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BEGIN_END, true);

      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.CURRENT_LINE, false);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.CURRENT_LINE_COLOR, "230,230,230");

      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.MATCHING_BRACKETS, true);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.MATCHING_BRACKETS_COLOR, "128,128,128");

      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PRINT_MARGIN, false);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PRINT_MARGIN_COLOR, "230,230,230");
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PRINT_MARGIN_COLUMN, 120);

      // Text editor default preferences
      RuntimeUtils.setDefaultPreferenceValue(store, AbstractTextEditor.PREFERENCE_TEXT_DRAG_AND_DROP_ENABLED, true);

      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.HEX_FONT_NAME, HexEditControl.DEFAULT_FONT_NAME);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.HEX_FONT_SIZE, 10);

      // General UI
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.UI_AUTO_UPDATE_CHECK, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.UI_KEEP_DATABASE_EDITORS, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.UI_PROXY_HOST, "");
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.UI_PROXY_PORT, 0);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.UI_PROXY_USER, "");
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.UI_PROXY_PASSWORD, "");
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.UI_DRIVERS_HOME, "");
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.UI_DRIVERS_SOURCES, "http://dbeaver.jkiss.org/files/jdbc/");

      // Network
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.NET_TUNNEL_PORT_MIN, 10000);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.NET_TUNNEL_PORT_MAX, 60000);

      // ResultSet
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_MAX_ROWS, 200);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_MAX_ROWS_USE_SQL, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_BINARY_PRESENTATION, DBDBinaryFormatter.FORMATS[0].getId());
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_BINARY_SHOW_STRINGS, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_BINARY_EDITOR_TYPE, IValueController.EditType.EDITOR);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_BINARY_STRING_MAX_LEN, 32);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_ORDER_SERVER_SIDE, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_SHOW_ODD_ROWS, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_SHOW_CELL_ICONS, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_DOUBLE_CLICK, Spreadsheet.DoubleClickBehavior.INLINE_EDITOR.name());
      RuntimeUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_AUTO_SWITCH_MODE, false);

      // QM
      RuntimeUtils.setDefaultPreferenceValue(store, QMConstants.PROP_HISTORY_DAYS, 90);
      RuntimeUtils.setDefaultPreferenceValue(store, QMConstants.PROP_ENTRIES_PER_PAGE, 200);
      RuntimeUtils.setDefaultPreferenceValue(store, QMConstants.PROP_OBJECT_TYPES,
          QMObjectType.toString(Arrays.asList(QMObjectType.txn, QMObjectType.query)));
      RuntimeUtils.setDefaultPreferenceValue(store, QMConstants.PROP_QUERY_TYPES,
          DBCExecutionPurpose.USER + "," +
              DBCExecutionPurpose.USER_SCRIPT);
      RuntimeUtils.setDefaultPreferenceValue(store, QMConstants.PROP_STORE_LOG_FILE, false);
      RuntimeUtils.setDefaultPreferenceValue(store, QMConstants.PROP_LOG_DIRECTORY, Platform.getLogFileLocation().toFile().getParent());

      // Data formats
      DataFormatterProfile.initDefaultPreferences(store, Locale.getDefault());
*/
  }

}
