/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jkiss.dbeaver.ModelPreferences.SeparateConnectionBehavior;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.model.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants.SQLAutocompletionMode;
import org.jkiss.dbeaver.ui.editors.sql.SQLScriptBindingType;
import org.jkiss.dbeaver.utils.PrefUtils;

public class SQLEditorPreferencesInitializer extends AbstractPreferenceInitializer {

    public SQLEditorPreferencesInitializer() {
    }

    @Override
    public void initializeDefaultPreferences() {
        // Init default preferences
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        // SQL execution
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_COMMIT_TYPE, SQLScriptCommitType.NO_COMMIT.name());
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_COMMIT_LINES, 1000);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_ERROR_HANDLING, SQLScriptErrorHandling.STOP_ROLLBACK.name());
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_FETCH_RESULT_SETS, true);

        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.STATEMENT_INVALIDATE_BEFORE_EXECUTE, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.STATEMENT_TIMEOUT, 0);
        // Disable separate connection by default. Otherwise many people don't understand what happens (data editor and SQL editor have different contexts)
        PrefUtils.setDefaultPreferenceValue(
            store, SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION, SeparateConnectionBehavior.DEFAULT.name()
        );
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.EDITOR_CONNECT_ON_ACTIVATE, true);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.EDITOR_CONNECT_ON_EXECUTE, true);

        {
            // SQL prefs
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_SAVE_ON_CHANGE, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_SAVE_ON_CLOSE, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_SAVE_ON_EXECUTE, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_SAVE_ACTIVE_SCHEMA, true);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_READ, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_WRITE, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_BIND_COMMENT_TYPE, SQLScriptBindingType.NAME.name());

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_DELETE_EMPTY, SQLPreferenceConstants.EmptyScriptCloseBehavior.DELETE_NEW.name());
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_AUTO_FOLDERS, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_CREATE_CONNECTION_FOLDERS, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_TITLE_PATTERN, SQLEditor.DEFAULT_TITLE_PATTERN);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SCRIPT_FILE_NAME_PATTERN, SQLEditor.DEFAULT_SCRIPT_FILE_NAME);
            PrefUtils.setDefaultPreferenceValue(
                store, SQLPreferenceConstants.SCRIPT_BIG_FILE_LENGTH_BOUNDARY, SQLEditor.MAX_FILE_LENGTH_FOR_RULES
            );

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_HIPPIE, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTOCOMPLETION_MODE, SQLAutocompletionMode.NEW.getName());
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, 0);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.TAB_AUTOCOMPLETION, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_INSERT_CASE, SQLCompletionContext.PROPOSAL_CASE_DEFAULT);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_REPLACE_WORD, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SHOW_VALUES, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.USE_GLOBAL_ASSISTANT, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SHOW_SERVER_HELP_TOPICS, false);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.FOLDING_ENABLED, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.PROBLEM_MARKERS_ENABLED, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.ADVANCED_HIGHLIGHTING_ENABLE, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.READ_METADATA_FOR_SEMANTIC_ANALYSIS, true);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_COMMENTS, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQLEDITOR_CLOSE_BLOCKS, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SMART_WORD_ITERATOR, true);

            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE, false);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQL_FORMAT_BOLD_KEYWORDS, true);
            PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.SQL_FORMAT_ACTIVE_QUERY, true);
        }

        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE, true);
        PrefUtils.setDefaultPreferenceValue(
            store,
            SQLPreferenceConstants.SHOW_STATISTICS_ON_EXECUTION,
            SQLPreferenceConstants.StatisticsTabOnExecutionBehavior.FOR_MULTIPLE_QUERIES
        );
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.CLOSE_INCLUDED_SCRIPT_AFTER_EXECUTION, true);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.BEEP_ON_QUERY_END, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.RESULT_SET_MAX_TABS_PER_QUERY, 20);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.CLEAR_OUTPUT_BEFORE_EXECUTE, false);

        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.RESULT_SET_CLOSE_ON_ERROR, false);
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.RESULT_SET_REPLACE_CURRENT_TAB, true);
        PrefUtils.setDefaultPreferenceValue(
            store,
            SQLPreferenceConstants.RESULT_SET_ORIENTATION, SQLEditor.ResultSetOrientation.HORIZONTAL.name());
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.OUTPUT_PANEL_AUTO_SHOW, true);

        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.DEFAULT_SQL_EDITOR_OPEN_COMMAND, SQLEditorCommands.CMD_SQL_EDITOR_RECENT);

        // Text editor default preferences
        PrefUtils.setDefaultPreferenceValue(store, AbstractTextEditor.PREFERENCE_TEXT_DRAG_AND_DROP_ENABLED, true);

        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.NEW_SCRIPT_TEMPLATE, SQLUtils.generateCommentLine(null, SQLEditorMessages.pref_page_sql_editor_new_script_template_template));
        PrefUtils.setDefaultPreferenceValue(store, SQLPreferenceConstants.NEW_SCRIPT_TEMPLATE_ENABLED, false);
    }

}
