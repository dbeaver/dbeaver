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
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.qm.QMConstants;
import org.jkiss.dbeaver.model.qm.QMObjectType;
import org.jkiss.dbeaver.registry.DataFormatterProfile;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.util.Arrays;
import java.util.Locale;

public class DBeaverPreferencesInitializer extends AbstractPreferenceInitializer {

  public DBeaverPreferencesInitializer() {
  }

  @Override
  public void initializeDefaultPreferences() {
      boolean isMac = RuntimeUtils.isPlatformMacOS();

      // Init default preferences
      DBPPreferenceStore store = new BundlePreferenceStore(DBeaverActivator.getInstance().getBundle());

      // Agent
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_ENABLED, true);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY, !isMac);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT, 30);

      // Navigator
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_EXPAND_ON_CONNECT, false);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_GROUP_BY_DRIVER, false);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME, false);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_CONNECTION_DOUBLE_CLICK, NavigatorViewBase.DoubleClickBehavior.SQL_EDITOR.name());

      // Common
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.KEEP_STATEMENT_OPEN, false);

      // SQL execution
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.NO_COMMIT.name());
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_COMMIT_LINES, 1000);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.STOP_ROLLBACK.name());
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS, false);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_AUTO_FOLDERS, false);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_TITLE_PATTERN, SQLEditorInput.DEFAULT_PATTERN);

      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE, false);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.STATEMENT_TIMEOUT, 10 * 1000);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.READ_EXPENSIVE_PROPERTIES, false);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.EDITOR_SEPARATE_CONNECTION, false);

      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_USE_ALL_COLUMNS, false);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE, 10 * 1000000);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_LONG_AS_LOB, true);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_COMMIT_ON_EDIT_APPLY, false);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_COMMIT_ON_CONTENT_APPLY, false);

      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.TEXT_EDIT_UNDO_LEVEL, 200);

      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, true);
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, 500);
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, true);
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_INSERT_CASE, SQLPreferenceConstants.PROPOSAL_CASE_DEFAULT);
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS, false);

      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, true);
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, true);
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, true);
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_COMMENTS, true);
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BEGIN_END, true);

      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.CURRENT_LINE, false);
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.CURRENT_LINE_COLOR, "230,230,230");

      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.MATCHING_BRACKETS, true);
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.MATCHING_BRACKETS_COLOR, "128,128,128");

      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PRINT_MARGIN, false);
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PRINT_MARGIN_COLOR, "230,230,230");
      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PRINT_MARGIN_COLUMN, 120);

      PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE, false);

      // Text editor default preferences
      PrefUtils.setDefaultPreferenceValue(store, AbstractTextEditor.PREFERENCE_TEXT_DRAG_AND_DROP_ENABLED, true);

      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.HEX_FONT_NAME, HexEditControl.DEFAULT_FONT_NAME);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.HEX_FONT_SIZE, 10);

      // General UI
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_AUTO_UPDATE_CHECK, true);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_KEEP_DATABASE_EDITORS, true);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_HOST, "");
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_PORT, 0);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_USER, "");
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_PASSWORD, "");
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_DRIVERS_HOME, "");
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_DRIVERS_SOURCES, "http://dbeaver.jkiss.org/files/jdbc/");

      // ResultSet
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT, true);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_MAX_ROWS, 200);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE, IValueController.EditType.EDITOR);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE, true);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS, true);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS, true);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_DOUBLE_CLICK, Spreadsheet.DoubleClickBehavior.INLINE_EDITOR.name());
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE, false);
      PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_TEXT_MAX_COLUMN_SIZE, 255);

      // QM
      PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_HISTORY_DAYS, 90);
      PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_ENTRIES_PER_PAGE, 200);
      PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_OBJECT_TYPES,
          QMObjectType.toString(Arrays.asList(QMObjectType.txn, QMObjectType.query)));
      PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_QUERY_TYPES,
          DBCExecutionPurpose.USER + "," +
              DBCExecutionPurpose.USER_SCRIPT);
      PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_STORE_LOG_FILE, false);
      PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_LOG_DIRECTORY, Platform.getLogFileLocation().toFile().getParent());

      // Data formats
      DataFormatterProfile.initDefaultPreferences(store, Locale.getDefault());
  }

}
