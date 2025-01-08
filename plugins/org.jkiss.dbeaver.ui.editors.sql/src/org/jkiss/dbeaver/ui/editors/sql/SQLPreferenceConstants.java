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
package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLModelPreferences;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

public class SQLPreferenceConstants {

    public enum EmptyScriptCloseBehavior {
        NOTHING(SQLEditorMessages.script_close_behavior_do_not_delete),
        DELETE_NEW(SQLEditorMessages.script_close_behavior_delete_only_new_scripts),
        DELETE_ALWAYS(SQLEditorMessages.script_close_behavior_delete_always);

        private final String title;

        EmptyScriptCloseBehavior(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public static EmptyScriptCloseBehavior getByTitle(String title) {
            for (EmptyScriptCloseBehavior escb : values()) {
                if (escb.getTitle().equals(title)) {
                    return escb;
                }
            }
            return NOTHING;
        }
        public static EmptyScriptCloseBehavior getByName(String name) {
            switch (name) {
                case "true":
                    return SQLPreferenceConstants.EmptyScriptCloseBehavior.DELETE_NEW;
                case "false":
                    return SQLPreferenceConstants.EmptyScriptCloseBehavior.NOTHING;
                default:
                    try {
                        return SQLPreferenceConstants.EmptyScriptCloseBehavior.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        return NOTHING;
                    }
            }
        }

    }
    
    public enum SQLAutocompletionMode {
        DEFAULT(true, false, SQLEditorMessages.pref_page_sql_completion_label_completion_mode_default),
        NEW(false, true, SQLEditorMessages.pref_page_sql_completion_label_completion_mode_new_engine),
        COMBINED(true, true, SQLEditorMessages.pref_page_sql_completion_label_completion_mode_combined);
    
        public final boolean useOldAnalyzer;
        public final boolean useNewAnalyzer;

        public final String title;
    
        SQLAutocompletionMode(boolean useOldAnalyzer, boolean useNewAnalyzer, String title) {
            this.useOldAnalyzer = useOldAnalyzer;
            this.useNewAnalyzer = useNewAnalyzer;
            this.title = title;
        }
        
        public String getName() {
            return this.toString();
        }

        public static SQLAutocompletionMode valueByName(String name) {
            if (name == null) {
                return DEFAULT;
            }  else {
                try {
                    return SQLAutocompletionMode.valueOf(name);
                } catch (IllegalArgumentException e) {
                    return SQLAutocompletionMode.DEFAULT;
                }
            }
        }

        @NotNull
        public static SQLAutocompletionMode fromPreferences(@NotNull DBPPreferenceStore preferenceStore) {
            return valueByName(preferenceStore.getString(SQLModelPreferences.AUTOCOMPLETION_MODE));
        }
    }

    public enum SQLCompletionObjectNameFormKind {
        DEFAULT(false, false, SQLEditorMessages.pref_page_sql_default),
        UNQUALIFIED(true, false, SQLEditorMessages.pref_page_sql_completion_label_use_short_names),
        QUALIFIED(false, true, SQLEditorMessages.pref_page_sql_completion_label_use_long_names);

        public final boolean unqualified;
        public final boolean qualified;
        @NotNull
        public final String title;

        SQLCompletionObjectNameFormKind(boolean unqualified, boolean qualified, @NotNull String title) {
            this.unqualified = unqualified;
            this.qualified = qualified;
            this.title = title;
        }

        @NotNull
        public String getName() {
            return this.toString();
        }

        public void setToPreferences(@NotNull DBPPreferenceStore preferenceStore) {
            preferenceStore.setValue(SQLModelPreferences.SQL_EDITOR_PROPOSAL_SHORT_NAME, this.unqualified);
            preferenceStore.setValue(SQLModelPreferences.SQL_EDITOR_PROPOSAL_ALWAYS_FQ, this.qualified);
        }

        @NotNull
        private static SQLCompletionObjectNameFormKind fromBooleanFlags(boolean useShortName, boolean useFqNames) {
            if (useShortName) {
                return UNQUALIFIED;
            } else if (useFqNames) {
                return QUALIFIED;
            } else {
                return DEFAULT;
            }
        }

        @NotNull
        public static SQLCompletionObjectNameFormKind getFromPreferences(@NotNull DBPPreferenceStore preferenceStore) {
            return fromBooleanFlags(
                preferenceStore.getBoolean(SQLModelPreferences.SQL_EDITOR_PROPOSAL_SHORT_NAME),
                preferenceStore.getBoolean(SQLModelPreferences.SQL_EDITOR_PROPOSAL_ALWAYS_FQ)
            );
        }


        @NotNull
        public static SQLCompletionObjectNameFormKind getDefaultFromPreferences(@NotNull DBPPreferenceStore preferenceStore) {
            return fromBooleanFlags(
                preferenceStore.getDefaultBoolean(SQLModelPreferences.SQL_EDITOR_PROPOSAL_SHORT_NAME),
                preferenceStore.getDefaultBoolean(SQLModelPreferences.SQL_EDITOR_PROPOSAL_ALWAYS_FQ)
            );
        }
    }


    public static final String INSERT_SINGLE_PROPOSALS_AUTO            = "SQLEditor.ContentAssistant.insert.single.proposal";
    public static final String ENABLE_HIPPIE                           = "SQLEditor.ContentAssistant.activate.hippie";
    public static final String ENABLE_AUTO_ACTIVATION                  = "SQLEditor.ContentAssistant.auto.activation.enable";
    public static final String AUTOCOMPLETION_MODE                     = SQLModelPreferences.AUTOCOMPLETION_MODE;
    public static final String ADVANCED_HIGHLIGHTING_ENABLE            = SQLModelPreferences.ADVANCED_HIGHLIGHTING_ENABLE;
    public static final String READ_METADATA_FOR_SEMANTIC_ANALYSIS     = SQLModelPreferences.READ_METADATA_FOR_SEMANTIC_ANALYSIS;
    public static final String ENABLE_KEYSTROKE_ACTIVATION             = "SQLEditor.ContentAssistant.auto.keystrokes.activation";
    public static final String AUTO_ACTIVATION_DELAY                   = "SQLEditor.ContentAssistant.auto.activation.delay";
    public static final String PROPOSAL_INSERT_CASE                    = "SQLEditor.ContentAssistant.insert.case";
    public static final String TAB_AUTOCOMPLETION                      = "SQLEditor.ContentAssistant.autocompletion.tab";
    public static final String PROPOSAL_REPLACE_WORD                   = "SQLEditor.ContentAssistant.replace.word";
    public static final String PROPOSAL_SORT_ALPHABETICALLY            = "SQLEditor.ContentAssistant.proposals.sort.alphabetically";
    public static final String HIDE_DUPLICATE_PROPOSALS                = "SQLEditor.ContentAssistant.hide.duplicates";
    public static final String INSERT_SPACE_AFTER_PROPOSALS            = "SQLEditor.ContentAssistant.insert.space.after.proposal";
    public static final String USE_GLOBAL_ASSISTANT                    = "SQLEditor.ContentAssistant.use.global.search";
    public static final String PROPOSALS_MATCH_CONTAINS                = "SQLEditor.ContentAssistant.matching.fuzzy";
    public static final String SHOW_COLUMN_PROCEDURES                  = "SQLEditor.ContentAssistant.show.column.procedures";
    public static final String SHOW_SERVER_HELP_TOPICS                 = "SQLEditor.ContentAssistant.show.helpTopics";
    public static final String SHOW_VALUES                             = "SQLEditor.ContentAssistant.show.values";

    public static final String MARK_OCCURRENCES_UNDER_CURSOR           = "SQLEditor.markOccurrences";
    public static final String MARK_OCCURRENCES_FOR_SELECTION          = "SQLEditor.markOccurrences.forSelection";
    public static final String FOLDING_ENABLED                         = "SQLEditor.Folding.enabled";
    public static final String PROBLEM_MARKERS_ENABLED                 = "SQLEditor.problemMarkers.enabled";

    // Auto-save
    public static final String AUTO_SAVE_ON_CHANGE                     = "SQLEditor.autoSaveOnChange";
    public static final String AUTO_SAVE_ON_CLOSE                      = "SQLEditor.autoSaveOnClose";
    public static final String AUTO_SAVE_ON_EXECUTE                    = "SQLEditor.autoSaveOnExecute";
    public static final String AUTO_SAVE_ACTIVE_SCHEMA                 = "SQLEditor.autoSaveActiveSchema";

    // Typing constants
    public static final String SQLEDITOR_CLOSE_SINGLE_QUOTES           = "SQLEditor.closeSingleQuotes";
    public static final String SQLEDITOR_CLOSE_DOUBLE_QUOTES           = "SQLEditor.closeDoubleQuotes";
    public static final String SQLEDITOR_CLOSE_BRACKETS                = "SQLEditor.closeBrackets";
    public static final String SQLEDITOR_CLOSE_COMMENTS                = "SQLEditor.closeComments";
    public static final String SQLEDITOR_CLOSE_BLOCKS                  = "SQLEditor.closeBlocks";

    public static final String SMART_WORD_ITERATOR                     = "SQLEditor.smartWordIterator";

    // Matching brackets
    public final static String MATCHING_BRACKETS                        = "SQLEditor.matchingBrackets";
    public final static String MATCHING_BRACKETS_HIGHLIGHT              = "SQLEditor.matchingBracketsHighlight";
    // Reuse "AbstractTextEditor.Color.Foreground" color
    public final static String MATCHING_BRACKETS_COLOR                  = "AbstractTextEditor.Color.Foreground";

    public final static String RESET_CURSOR_ON_EXECUTE                  = "SQLEditor.resetCursorOnExecute";
    public final static String MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE        = "SQLEditor.maxEditorOnScriptExecute";
    public static final String SHOW_STATISTICS_ON_EXECUTION             = "SQLEditor.showStatisticsForQueriesWithResults";
    public static final String SET_SELECTION_TO_STATISTICS_TAB          = "SQLEditor.setSelectionToStatisticsTab";
    public static final String CLOSE_INCLUDED_SCRIPT_AFTER_EXECUTION    = "SQLEditor.closeIncludedScriptAfterExecution";

    public final static String SQL_FORMAT_KEYWORD_CASE_AUTO             = "SQLEditor.format.keywordCaseAuto";
    public final static String SQL_FORMAT_EXTRACT_FROM_SOURCE           = "SQLEditor.format.extractFromSource";
    public final static String SQL_FORMAT_BOLD_KEYWORDS                 = "SQLEditor.format.boldKeywords";
    public final static String SQL_FORMAT_ACTIVE_QUERY                  = "SQLEditor.format.activeQuery";

    public final static String BEEP_ON_QUERY_END                        = "SQLEditor.beepOnQueryEnd";
    public final static String REFRESH_DEFAULTS_AFTER_EXECUTE           = "SQLEditor.refreshDefaultsAfterExecute";
    public final static String CLEAR_OUTPUT_BEFORE_EXECUTE              = "SQLEditor.clearOutputBeforeExecute";

    public static final String RESULT_SET_MAX_TABS_PER_QUERY            = "SQLEditor.resultSet.queryTabLimit";
    public final static String RESULT_SET_CLOSE_ON_ERROR                = "SQLEditor.resultSet.closeOnError";
    public final static String RESULT_SET_REPLACE_CURRENT_TAB           = "SQLEditor.resultSet.replaceCurrentTab"; //$NON-NLS-1$
    public final static String RESULT_SET_ORIENTATION                   = "SQLEditor.resultSet.orientation";
    public static final String RESULTS_PANEL_RATIO                      = "SQLEditor.resultSet.ratio";
    public static final String MULTIPLE_RESULTS_PER_TAB                 = "SQLEditor.resultSet.multipleResultsPerTab";
    public static final String EXTRA_PANEL_RATIO                        = "SQLEditor.extraPanels.ratio";
    public static final String EXTRA_PANEL_LOCATION                     = "SQLEditor.extraPanels.location";
    public static final String OUTPUT_PANEL_AUTO_SHOW                   = "SQLEditor.outputPanel.autoShow";

    public static final String SCRIPT_BIND_EMBEDDED_READ                = "SQLEditor.script.bind.embedded.read"; //$NON-NLS-1$
    public static final String SCRIPT_BIND_EMBEDDED_WRITE               = "SQLEditor.script.bind.embedded.write"; //$NON-NLS-1$
    public static final String SCRIPT_BIND_COMMENT_TYPE                 = "SQLEditor.script.bind.commentType"; //$NON-NLS-1$
    public static final String SCRIPT_BIG_FILE_LENGTH_BOUNDARY          = "SQLEditor.script.bigFileLengthBoundary"; //$NON-NLS-1$

    public static final String SCRIPT_DELETE_EMPTY                      = "script.delete.empty"; //$NON-NLS-1$
    public static final String SCRIPT_AUTO_FOLDERS                      = "script.auto.folders"; //$NON-NLS-1$
    public static final String SCRIPT_CREATE_CONNECTION_FOLDERS         = "script.auto.connection.folders"; //$NON-NLS-1$
    public static final String SCRIPT_TITLE_PATTERN                     = "script.title.pattern"; //$NON-NLS-1$
    public static final String SCRIPT_FILE_NAME_PATTERN                 = "script.file.name.pattern";

    public static final String VAR_CONNECTION_NAME = "connectionName";
    public static final String VAR_FILE_NAME = "fileName";
    public static final String VAR_FILE_EXT = "fileExt";
    public static final String VAR_DRIVER_NAME = "driverName";
    public static final String VAR_ACTIVE_DATABASE = "database";
    public static final String VAR_ACTIVE_SCHEMA = "schema";
    public static final String VAR_ACTIVE_PROJECT = "projectName";


    public static final String SCRIPT_COMMIT_TYPE                       = "script.commit.type"; //$NON-NLS-1$
    public static final String SCRIPT_COMMIT_LINES                      = "script.commit.lines"; //$NON-NLS-1$
    public static final String SCRIPT_ERROR_HANDLING                    = "script.error.handling"; //$NON-NLS-1$
    public static final String SCRIPT_FETCH_RESULT_SETS                 = "script.fetch.resultset"; //$NON-NLS-1$
    public static final String NEW_SCRIPT_TEMPLATE_ENABLED              = "new.script.template.enabled"; //$NON-NLS-1$
    public static final String NEW_SCRIPT_TEMPLATE                      = "new.script.template"; //$NON-NLS-1$
    public static final String STATEMENT_INVALIDATE_BEFORE_EXECUTE      = "statement.invalidate.before.execute"; //$NON-NLS-1$
    public static final String STATEMENT_TIMEOUT                        = "statement.timeout"; //$NON-NLS-1$
    public static final String EDITOR_SEPARATE_CONNECTION               = "database.editor.separate.connection"; //$NON-NLS-1$
    public static final String EDITOR_CONNECT_ON_ACTIVATE               = "database.editor.connect.on.activate"; //$NON-NLS-1$
    public static final String EDITOR_CONNECT_ON_EXECUTE                = "database.editor.connect.on.execute"; //$NON-NLS-1$

    public static final String CONFIRM_DANGER_SQL                       = "dangerous_sql"; //$NON-NLS-1$
    public static final String CONFIRM_DROP_SQL                         = "drop_sql"; //$NON-NLS-1$
    public static final String CONFIRM_MASS_PARALLEL_SQL                = "mass_parallel_sql"; //$NON-NLS-1$
    public static final String CONFIRM_RUNNING_QUERY_CLOSE              = "close_running_query"; //$NON-NLS-1$
    public static final String CONFIRM_RESULT_TABS_CLOSE                = "close_result_tabs"; //$NON-NLS-1$
    public static final String CONFIRM_SAVE_SQL_CONSOLE                 = "save_sql_console"; //$NON-NLS-1$

    public static final String DEFAULT_SQL_EDITOR_OPEN_COMMAND          = "SQLEditor.defaultOpenCommand";

    public static final String LOCATION_RIGHT       = "right";
    public static final String LOCATION_BOTTOM      = "bottom";
    public static final String LOCATION_RESULTS     = "results";

    public enum StatisticsTabOnExecutionBehavior {
        NEVER("Only when no data"),
        FOR_MULTIPLE_QUERIES("For multiple queries with results"),
        ALWAYS("Always");

        private final String title;

        StatisticsTabOnExecutionBehavior(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public static StatisticsTabOnExecutionBehavior getByTitle(String title) {
            for (StatisticsTabOnExecutionBehavior statisticsTabOnExecution : values()) {
                if (statisticsTabOnExecution.getTitle().equals(title)) {
                    return statisticsTabOnExecution;
                }
            }
            return StatisticsTabOnExecutionBehavior.NEVER;
        }
        public static StatisticsTabOnExecutionBehavior getByName(String name) {
            switch (name) {
                case "true":
                    return StatisticsTabOnExecutionBehavior.FOR_MULTIPLE_QUERIES;
                case "false":
                    return StatisticsTabOnExecutionBehavior.NEVER;
                default:
                    try {
                        return StatisticsTabOnExecutionBehavior.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        return StatisticsTabOnExecutionBehavior.NEVER;
                    }
            }
        }

    }
}
