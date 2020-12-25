/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

public class SQLPreferenceConstants
{

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

    public static final String INSERT_SINGLE_PROPOSALS_AUTO            = "SQLEditor.ContentAssistant.insert.single.proposal";
    public static final String ENABLE_AUTO_ACTIVATION                  = "SQLEditor.ContentAssistant.auto.activation.enable";
    public static final String ENABLE_KEYSTROKE_ACTIVATION             = "SQLEditor.ContentAssistant.auto.keystrokes.activation";
    public static final String AUTO_ACTIVATION_DELAY                   = "SQLEditor.ContentAssistant.auto.activation.delay";
    public static final String PROPOSAL_INSERT_CASE                    = "SQLEditor.ContentAssistant.insert.case";
    public static final String PROPOSAL_REPLACE_WORD                   = "SQLEditor.ContentAssistant.replace.word";
    public static final String PROPOSAL_SORT_ALPHABETICALLY            = "SQLEditor.ContentAssistant.proposals.sort.alphabetically";
    public static final String HIDE_DUPLICATE_PROPOSALS                = "SQLEditor.ContentAssistant.hide.duplicates";
    public static final String PROPOSAL_SHORT_NAME                     = "SQLEditor.ContentAssistant.proposals.short.name";
    public static final String PROPOSAL_ALWAYS_FQ                      = "SQLEditor.ContentAssistant.proposals.long.name";
    public static final String INSERT_SPACE_AFTER_PROPOSALS            = "SQLEditor.ContentAssistant.insert.space.after.proposal";
    public static final String USE_GLOBAL_ASSISTANT                    = "SQLEditor.ContentAssistant.use.global.search";
    public static final String PROPOSALS_MATCH_CONTAINS                = "SQLEditor.ContentAssistant.matching.fuzzy";
    public static final String SHOW_COLUMN_PROCEDURES                  = "SQLEditor.ContentAssistant.show.column.procedures";
    public static final String SHOW_SERVER_HELP_TOPICS                 = "SQLEditor.ContentAssistant.show.helpTopics";

    public static final String MARK_OCCURRENCES_UNDER_CURSOR           = "SQLEditor.markOccurrences";
    public static final String MARK_OCCURRENCES_FOR_SELECTION          = "SQLEditor.markOccurrences.forSelection";
    public static final String FOLDING_ENABLED                         = "SQLEditor.Folding.enabled";

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
    public static final String SQLEDITOR_CLOSE_BEGIN_END               = "SQLEditor.closeBeginEndStatement";

    // Matching brackets
    public final static String MATCHING_BRACKETS                        = "SQLEditor.matchingBrackets";
    public final static String MATCHING_BRACKETS_HIGHLIGHT              = "SQLEditor.matchingBracketsHighlight";
    // Reuse "AbstractTextEditor.Color.Foreground" color
    public final static String MATCHING_BRACKETS_COLOR                  = "AbstractTextEditor.Color.Foreground";//"org.jkiss.dbeaver.sql.editor.color.matchingBrackets.foreground";

    public final static String RESET_CURSOR_ON_EXECUTE                  = "SQLEditor.resetCursorOnExecute";
    public final static String MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE        = "SQLEditor.maxEditorOnScriptExecute";

    public final static String SQL_FORMAT_KEYWORD_CASE_AUTO             = "SQLEditor.format.keywordCaseAuto";
    public final static String SQL_FORMAT_EXTRACT_FROM_SOURCE           = "SQLEditor.format.extractFromSource";
    public final static String SQL_FORMAT_BOLD_KEYWORDS                 = "SQLEditor.format.boldKeywords";
    public final static String SQL_FORMAT_ACTIVE_QUERY                  = "SQLEditor.format.activeQuery";

    public final static String BEEP_ON_QUERY_END                        = "SQLEditor.beepOnQueryEnd";
    public final static String REFRESH_DEFAULTS_AFTER_EXECUTE           = "SQLEditor.refreshDefaultsAfterExecute";
    public final static String CLEAR_OUTPUT_BEFORE_EXECUTE              = "SQLEditor.clearOutputBeforeExecute";

    public final static String RESULT_SET_CLOSE_ON_ERROR                = "SQLEditor.resultSet.closeOnError";
    public final static String RESULT_SET_ORIENTATION                   = "SQLEditor.resultSet.orientation";
    public static final String RESULTS_PANEL_RATIO                      = "SQLEditor.resultSet.ratio";

    public static final String SCRIPT_BIND_EMBEDDED_READ                = "SQLEditor.script.bind.embedded.read"; //$NON-NLS-1$
    public static final String SCRIPT_BIND_EMBEDDED_WRITE               = "SQLEditor.script.bind.embedded.write"; //$NON-NLS-1$
    public static final String SCRIPT_BIND_COMMENT_TYPE                 = "SQLEditor.script.bind.commentType"; //$NON-NLS-1$

    public static final String SCRIPT_DELETE_EMPTY                      = "script.delete.empty"; //$NON-NLS-1$
    public static final String SCRIPT_AUTO_FOLDERS                      = "script.auto.folders"; //$NON-NLS-1$
    public static final String SCRIPT_CREATE_CONNECTION_FOLDERS         = "script.auto.connection.folders"; //$NON-NLS-1$
    public static final String SCRIPT_TITLE_PATTERN                     = "script.title.pattern"; //$NON-NLS-1$

    public static final String SCRIPT_COMMIT_TYPE                       = "script.commit.type"; //$NON-NLS-1$
    public static final String SCRIPT_COMMIT_LINES                      = "script.commit.lines"; //$NON-NLS-1$
    public static final String SCRIPT_ERROR_HANDLING                    = "script.error.handling"; //$NON-NLS-1$
    public static final String SCRIPT_FETCH_RESULT_SETS                 = "script.fetch.resultset"; //$NON-NLS-1$
    public static final String STATEMENT_INVALIDATE_BEFORE_EXECUTE      = "statement.invalidate.before.execute"; //$NON-NLS-1$
    public static final String STATEMENT_TIMEOUT                        = "statement.timeout"; //$NON-NLS-1$
    public static final String EDITOR_SEPARATE_CONNECTION               = "database.editor.separate.connection"; //$NON-NLS-1$
    public static final String EDITOR_CONNECT_ON_ACTIVATE               = "database.editor.connect.on.activate"; //$NON-NLS-1$
    public static final String EDITOR_CONNECT_ON_EXECUTE                = "database.editor.connect.on.execute"; //$NON-NLS-1$

    public static final String CONFIRM_DANGER_SQL                       = "dangerous_sql"; //$NON-NLS-1$
    public static final String CONFIRM_MASS_PARALLEL_SQL                = "mass_parallel_sql"; //$NON-NLS-1$
    public static final String CONFIRM_RUNNING_QUERY_CLOSE              = "close_running_query"; //$NON-NLS-1$
    public static final String CONFIRM_RESULT_TABS_CLOSE                = "close_result_tabs"; //$NON-NLS-1$


}
