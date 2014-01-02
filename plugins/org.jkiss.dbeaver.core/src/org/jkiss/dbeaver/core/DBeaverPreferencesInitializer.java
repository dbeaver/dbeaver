package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jkiss.dbeaver.DBeaverConstants;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.registry.DataFormatterProfile;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.qm.QMConstants;
import org.jkiss.dbeaver.runtime.qm.QMObjectType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.editors.binary.HexEditControl;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

public class DBeaverPreferencesInitializer extends AbstractPreferenceInitializer {

  public DBeaverPreferencesInitializer() {
  }

  @Override
  public void initializeDefaultPreferences() {
      // Init default preferences
      IPreferenceStore store = DBeaverActivator.getInstance().getPreferenceStore();
      {
          File driversHome = DriverDescriptor.getCustomDriversHome();
          System.setProperty(DBeaverConstants.PROP_DRIVERS_LOCATION, driversHome.getAbsolutePath());
      }

      // Agent
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_ENABLED, true);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY, true);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT, 30);

      // Navigator
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_EXPAND_ON_CONNECT, false);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_GROUP_BY_DRIVER, false);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME, false);

      // Common
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.DEFAULT_AUTO_COMMIT, true);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.KEEP_STATEMENT_OPEN, false);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.QUERY_ROLLBACK_ON_ERROR, false);

      // SQL execution
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.NO_COMMIT.name());
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_COMMIT_LINES, 1000);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.STOP_ROLLBACK.name());
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS, false);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SCRIPT_AUTO_FOLDERS, false);

      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE, false);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.STATEMENT_TIMEOUT, 10 * 1000);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.MEMORY_CONTENT_MAX_SIZE, 10000);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.READ_EXPENSIVE_PROPERTIES, true);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.META_SEPARATE_CONNECTION, true);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.META_CASE_SENSITIVE, false);

      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_USE_ALL_COLUMNS, false);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE, 10 * 1000000);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_EDIT_LONG_AS_LOB, true);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.CONTENT_HEX_ENCODING, ContentUtils.getDefaultFileEncoding());
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_COMMIT_ON_EDIT_APPLY, false);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RS_COMMIT_ON_CONTENT_APPLY, false);

      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.TEXT_EDIT_UNDO_LEVEL, 200);

      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, true);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, 500);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, true);
      RuntimeUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_INSERT_CASE, SQLPreferenceConstants.PROPOSAL_CASE_DEFAULT);

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

      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.HEX_FONT_NAME, HexEditControl.DEFAULT_FONT_NAME);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.HEX_FONT_SIZE, 10);

      // General UI
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_AUTO_UPDATE_CHECK, true);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_HOST, "");
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_PORT, 0);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_USER, "");
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_PROXY_PASSWORD, "");
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_DRIVERS_HOME, "");
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_DRIVERS_SOURCES, DBeaverConstants.DEFAULT_DRIVERS_SOURCE);

      // Network
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NET_TUNNEL_PORT_MIN, 10000);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NET_TUNNEL_PORT_MAX, 60000);

      // ResultSet
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_MAX_ROWS, 200);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_BINARY_PRESENTATION, DBDBinaryFormatter.FORMATS[0].getId());
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_BINARY_SHOW_STRINGS, true);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE, DBDValueController.EditType.EDITOR);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_BINARY_STRING_MAX_LEN, 32);
      RuntimeUtils.setDefaultPreferenceValue(store, DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE, true);

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
  }

} 