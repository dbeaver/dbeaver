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
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMConstants;
import org.jkiss.dbeaver.model.qm.QMObjectType;
import org.jkiss.dbeaver.registry.formatter.DataFormatterProfile;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLScriptBindingType;
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

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.SECURITY_USE_BOUNCY_CASTLE, true);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.TOOLBARS_SHOW_GENERAL_ALWAYS, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.TOOLBARS_SHOW_EDIT, false);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.TOOLBARS_DATABASE_SELECTOR_WIDTH, 20);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.TOOLBARS_SCHEMA_SELECTOR_WIDTH, 18);

        // SQL execution
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.NO_COMMIT.name());
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_COMMIT_LINES, 1000);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.STOP_ROLLBACK.name());
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_FETCH_RESULT_SETS, true);

        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.STATEMENT_INVALIDATE_BEFORE_EXECUTE, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.STATEMENT_TIMEOUT, 0);
        // Disable separate connection by default. Otherwise many people don't understand what happens (data editor and SQL editor have different contexts)
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE, true);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE, false);

        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.TEXT_EDIT_UNDO_LEVEL, 200);

        {
            // SQL prefs
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE, false);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_READ, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_WRITE, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_BIND_COMMENT_TYPE, SQLScriptBindingType.NAME.name());

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_DELETE_EMPTY, SQLPreferenceConstants.EmptyScriptCloseBehavior.DELETE_NEW.name());
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_AUTO_FOLDERS, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_CREATE_CONNECTION_FOLDERS, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_TITLE_PATTERN, SQLEditor.DEFAULT_TITLE_PATTERN);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, 0);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_INSERT_CASE, SQLPreferenceConstants.PROPOSAL_CASE_DEFAULT);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_REPLACE_WORD, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_SHORT_NAME, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_ALWAYS_FQ, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.USE_GLOBAL_ASSISTANT, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SHOW_SERVER_HELP_TOPICS, false);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.FOLDING_ENABLED, false);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_COMMENTS, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BEGIN_END, true);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQL_FORMAT_BOLD_KEYWORDS, true);
        }

        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE, true);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.BEEP_ON_QUERY_END, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.CLEAR_OUTPUT_BEFORE_EXECUTE, false);

        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.RESULT_SET_ORIENTATION, SQLEditor.ResultSetOrientation.HORIZONTAL.name());

        // Text editor default preferences
        PrefUtils.setDefaultPreferenceValue(store, AbstractTextEditor.PREFERENCE_TEXT_DRAG_AND_DROP_ENABLED, true);

        // General UI
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_AUTO_UPDATE_CHECK, true);
        PrefUtils.setDefaultPreferenceValue(store, DBeaverPreferences.UI_KEEP_DATABASE_EDITORS, true);

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
