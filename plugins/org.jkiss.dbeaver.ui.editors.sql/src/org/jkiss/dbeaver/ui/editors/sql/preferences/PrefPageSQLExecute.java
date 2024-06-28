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
package org.jkiss.dbeaver.ui.editors.sql.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ModelPreferences.SQLScriptStatementDelimiterMode;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.model.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants.StatisticsTabOnExecutionBehavior;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageSQLExecute
 */
public class PrefPageSQLExecute extends TargetPrefPage
{
    private static final Log log = Log.getLog(PrefPageSQLExecute.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sqlexecute"; //$NON-NLS-1$

    private Button invalidateBeforeExecuteCheck;
    private Spinner executeTimeoutText;
    private Button soundOnQueryEnd;
    private Button updateDefaultAfterExecute;
    private Button clearOutputBeforeExecute;

    private Combo commitTypeCombo;
    private Combo errorHandlingCombo;
    private Spinner commitLinesText;
    private Button fetchResultSetsCheck;
    private Button resetCursorCheck;
    private Button maxEditorCheck;
    private Combo showStatisticsCombo;
    private Button setSelectionToStatisticsTabCheck;
    private Button closeIncludedScriptAfterExecutionCheck;

    private Text statementDelimiterText;
    private Button ignoreNativeDelimiter;
    private Combo blankLineDelimiterCombo;
    private Button removeTrailingDelimiter;

    private Button enableSQLParameters;
    private Button enableSQLAnonymousParameters;
    private Text anonymousParameterMarkText;
    private Text namedParameterPrefixText;
    private Text controlCommandPrefixText;
    private Button enableParametersInEmbeddedCode;
    private Button enableVariables;

    public PrefPageSQLExecute()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(SQLPreferenceConstants.STATEMENT_INVALIDATE_BEFORE_EXECUTE) ||
            store.contains(SQLPreferenceConstants.STATEMENT_TIMEOUT) ||

            store.contains(SQLPreferenceConstants.SCRIPT_COMMIT_TYPE) ||
            store.contains(SQLPreferenceConstants.SCRIPT_ERROR_HANDLING) ||
            store.contains(SQLPreferenceConstants.SCRIPT_COMMIT_LINES) ||
            store.contains(SQLPreferenceConstants.SCRIPT_FETCH_RESULT_SETS) ||

            store.contains(ModelPreferences.SCRIPT_STATEMENT_DELIMITER) ||
            store.contains(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER) ||
            store.contains(ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK) ||
            store.contains(ModelPreferences.QUERY_REMOVE_TRAILING_DELIMITER) ||

            store.contains(ModelPreferences.SQL_PARAMETERS_ENABLED) ||
            store.contains(ModelPreferences.SQL_PARAMETERS_IN_EMBEDDED_CODE_ENABLED) ||
            store.contains(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED) ||
            store.contains(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK) ||
            store.contains(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX) ||
            store.contains(ModelPreferences.SQL_CONTROL_COMMAND_PREFIX) ||
            store.contains(ModelPreferences.SQL_VARIABLES_ENABLED) ||

            store.contains(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE) ||
            store.contains(SQLPreferenceConstants.MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE) ||
            store.contains(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE) ||
            store.contains(SQLPreferenceConstants.CLEAR_OUTPUT_BEFORE_EXECUTE)
        ;
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);
        Composite leftPane = UIUtils.createComposite(composite, 1);
        leftPane.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        Composite rightPane = UIUtils.createComposite(composite, 1);
        rightPane.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        // General settings
        {
            Composite commonGroup = UIUtils.createControlGroup(
                leftPane,
                SQLEditorMessages.pref_page_sql_editor_group_common,
                2,
                GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING,
                0
            );
            {
                invalidateBeforeExecuteCheck = UIUtils.createCheckbox(
                    commonGroup,
                    SQLEditorMessages.pref_page_sql_editor_label_invalidate_before_execute,
                    null,
                    false,
                    2
                );
                soundOnQueryEnd = UIUtils.createCheckbox(
                    commonGroup,
                    SQLEditorMessages.pref_page_sql_editor_label_sound_on_query_end,
                    null,
                    false,
                    2
                );
                updateDefaultAfterExecute = UIUtils.createCheckbox(
                    commonGroup,
                    SQLEditorMessages.pref_page_sql_editor_label_refresh_defaults_after_execute,
                    SQLEditorMessages.pref_page_sql_editor_label_refresh_defaults_after_execute_tip,
                    false,
                    2
                );
                clearOutputBeforeExecute = UIUtils.createCheckbox(
                    commonGroup,
                    SQLEditorMessages.pref_page_sql_editor_label_clear_output_before_execute,
                    SQLEditorMessages.pref_page_sql_editor_label_clear_output_before_execute_tip,
                    false,
                    2
                );

                UIUtils.createControlLabel(commonGroup, SQLEditorMessages.pref_page_sql_editor_label_sql_timeout + UIMessages.label_sec);
                executeTimeoutText = new Spinner(commonGroup, SWT.BORDER);
                executeTimeoutText.setSelection(0);
                executeTimeoutText.setDigits(0);
                executeTimeoutText.setIncrement(1);
                executeTimeoutText.setMinimum(0);
                executeTimeoutText.setMaximum(100000);
                executeTimeoutText.setToolTipText(SQLEditorMessages.pref_page_sql_editor_label_sql_timeout_tip);

            }
        }

        // Scripts
        {
            Composite scriptsGroup = UIUtils.createControlGroup(rightPane, SQLEditorMessages.pref_page_sql_editor_group_scripts, 2, GridData.FILL_HORIZONTAL, 0);
            {
                UIUtils.createControlLabel(scriptsGroup, SQLEditorMessages.pref_page_sql_editor_label_commit_type);

                commitTypeCombo = new Combo(scriptsGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                commitTypeCombo.add(SQLEditorMessages.pref_page_sql_editor_combo_item_script_end, SQLScriptCommitType.AT_END.ordinal());
                commitTypeCombo.add(SQLEditorMessages.pref_page_sql_editor_combo_item_each_line_autocommit, SQLScriptCommitType.AUTOCOMMIT.ordinal());
                commitTypeCombo.add(SQLEditorMessages.pref_page_sql_editor_combo_item_each_spec_line, SQLScriptCommitType.NLINES.ordinal());
                commitTypeCombo.add(SQLEditorMessages.pref_page_sql_editor_combo_item_no_commit, SQLScriptCommitType.NO_COMMIT.ordinal());
            }

            {
                UIUtils.createControlLabel(scriptsGroup, SQLEditorMessages.pref_page_sql_editor_label_commit_after_line);
                commitLinesText = new Spinner(scriptsGroup, SWT.BORDER);
                commitLinesText.setSelection(0);
                commitLinesText.setDigits(0);
                commitLinesText.setIncrement(1);
                commitLinesText.setMinimum(1);
                commitLinesText.setMaximum(1024 * 1024);
            }

            {
                UIUtils.createControlLabel(scriptsGroup, SQLEditorMessages.pref_page_sql_editor_label_error_handling);

                errorHandlingCombo = new Combo(scriptsGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                errorHandlingCombo.add(SQLEditorMessages.pref_page_sql_editor_combo_item_stop_rollback, SQLScriptErrorHandling.STOP_ROLLBACK.ordinal());
                errorHandlingCombo.add(SQLEditorMessages.pref_page_sql_editor_combo_item_stop_commit, SQLScriptErrorHandling.STOP_COMMIT.ordinal());
                errorHandlingCombo.add(SQLEditorMessages.pref_page_sql_editor_combo_item_ignore, SQLScriptErrorHandling.IGNORE.ordinal());
            }

            fetchResultSetsCheck = UIUtils.createCheckbox(scriptsGroup, SQLEditorMessages.pref_page_sql_editor_checkbox_fetch_resultsets, null, false, 2);
            resetCursorCheck = UIUtils.createCheckbox(scriptsGroup, SQLEditorMessages.pref_page_sql_editor_checkbox_reset_cursor, null, false, 2);
            maxEditorCheck = UIUtils.createCheckbox(scriptsGroup, SQLEditorMessages.pref_page_sql_editor_checkbox_max_editor_on_script_exec, null, false, 2);
            showStatisticsCombo = UIUtils.createLabelCombo(
                scriptsGroup,
                SQLEditorMessages.pref_page_sql_editor_checkbox_show_statistics_for_queries_with_results,
                SQLEditorMessages.pref_page_sql_editor_checkbox_show_statistics_for_queries_with_results_tip,
                SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY
            );
            for (StatisticsTabOnExecutionBehavior statisticsTabOnExecution : StatisticsTabOnExecutionBehavior.values()) {
                showStatisticsCombo.add(statisticsTabOnExecution.getTitle());
            }
            setSelectionToStatisticsTabCheck = UIUtils.createCheckbox(
                    scriptsGroup,
                    SQLEditorMessages.pref_page_sql_editor_checkbox_select_statistics_tab,
                    SQLEditorMessages.pref_page_sql_editor_checkbox_select_statistics_tab_tip,
                    true,
                    2
            );
            closeIncludedScriptAfterExecutionCheck = UIUtils.createCheckbox(
                scriptsGroup,
                SQLEditorMessages.pref_page_sql_editor_checkbox_close_included_script_after_execution,
                SQLEditorMessages.pref_page_sql_editor_checkbox_close_included_script_after_execution_tip,
                true,
                2
            );
        }
        // Parameters
        {
            Composite paramsGroup = UIUtils.createControlGroup(leftPane, SQLEditorMessages.pref_page_sql_editor_group_parameters, 2, GridData.VERTICAL_ALIGN_FILL, 0);
            enableSQLParameters = UIUtils.createCheckbox(paramsGroup, SQLEditorMessages.pref_page_sql_editor_checkbox_enable_sql_parameters, null, false, 2);
            enableSQLAnonymousParameters = UIUtils.createCheckbox(paramsGroup, SQLEditorMessages.pref_page_sql_editor_checkbox_enable_sql_anonymous_parameters, null, false, 2);
            anonymousParameterMarkText = UIUtils.createLabelText(paramsGroup, SQLEditorMessages.pref_page_sql_editor_text_anonymous_parameter_mark, "", SWT.BORDER, new GridData(32, SWT.DEFAULT));
            anonymousParameterMarkText.setTextLimit(1);
            namedParameterPrefixText = UIUtils.createLabelText(paramsGroup, SQLEditorMessages.pref_page_sql_editor_text_named_parameter_prefix, "", SWT.BORDER, new GridData(32, SWT.DEFAULT));
            namedParameterPrefixText.setTextLimit(1);
            controlCommandPrefixText = UIUtils.createLabelText(paramsGroup, SQLEditorMessages.pref_page_sql_editor_text_control_command_prefix, "", SWT.BORDER, new GridData(32, SWT.DEFAULT));
            enableParametersInEmbeddedCode = UIUtils.createCheckbox(paramsGroup, SQLEditorMessages.pref_page_sql_editor_enable_parameters_in_ddl, SQLEditorMessages.pref_page_sql_editor_enable_parameters_in_ddl_tip, false, 2);
            enableVariables = UIUtils.createCheckbox(paramsGroup, SQLEditorMessages.pref_page_sql_editor_enable_variables, SQLEditorMessages.pref_page_sql_editor_enable_variables_tip, false, 2);

            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            gd.verticalIndent = 12;

            UIUtils.createLink(paramsGroup, SQLEditorMessages.pref_page_sql_editor_text_explanation_link, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    switch (e.text) {
                        case "params":
                            ShellUtils.launchProgram(HelpUtils.getHelpExternalReference("SQL-Execution#dynamic-parameters-binding"));
                            break;
                        case "commands":
                            ShellUtils.launchProgram(HelpUtils.getHelpExternalReference("Client-Side-Scripting"));
                            break;
                        default:
                            break;
                    }
                }
            }).setLayoutData(gd);
        }

        // Delimiters
        {
            Composite delimGroup = UIUtils.createControlGroup(rightPane, SQLEditorMessages.pref_page_sql_editor_group_delimiters, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
            statementDelimiterText = UIUtils.createLabelText(delimGroup, SQLEditorMessages.pref_page_sql_editor_text_statement_delimiter, "", SWT.BORDER, new GridData(32, SWT.DEFAULT));
            //statementDelimiterText.setTextLimit(1);
            ignoreNativeDelimiter = UIUtils.createCheckbox(delimGroup, SQLEditorMessages.pref_page_sql_editor_checkbox_ignore_native_delimiter, SQLEditorMessages.pref_page_sql_editor_checkbox_ignore_native_delimiter_tip, false, 2);
            
            blankLineDelimiterCombo = UIUtils.createLabelCombo(delimGroup, SQLEditorMessages.pref_page_sql_editor_checkbox_blank_line_delimiter, SWT.READ_ONLY | SWT.DROP_DOWN);
            for (SQLScriptStatementDelimiterMode mode : SQLScriptStatementDelimiterMode.values()) {
                blankLineDelimiterCombo.add(mode.title);
            }
            
            removeTrailingDelimiter = UIUtils.createCheckbox(delimGroup, SQLEditorMessages.pref_page_sql_editor_checkbox_remove_trailing_delimiter, SQLEditorMessages.pref_page_sql_editor_checkbox_remove_trailing_delimiter_tip, false, 2);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store) {
        try {
            loadPreferences(store, false);
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store) {
        try {
            store.setValue(SQLPreferenceConstants.STATEMENT_INVALIDATE_BEFORE_EXECUTE, invalidateBeforeExecuteCheck.getSelection());
            store.setValue(SQLPreferenceConstants.STATEMENT_TIMEOUT, executeTimeoutText.getSelection());
            store.setValue(SQLPreferenceConstants.BEEP_ON_QUERY_END, soundOnQueryEnd.getSelection());
            store.setValue(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE, updateDefaultAfterExecute.getSelection());
            store.setValue(SQLPreferenceConstants.CLEAR_OUTPUT_BEFORE_EXECUTE, clearOutputBeforeExecute.getSelection());

            store.setValue(SQLPreferenceConstants.SCRIPT_COMMIT_TYPE, CommonUtils.fromOrdinal(SQLScriptCommitType.class, commitTypeCombo.getSelectionIndex()).name());
            store.setValue(SQLPreferenceConstants.SCRIPT_COMMIT_LINES, commitLinesText.getSelection());
            store.setValue(SQLPreferenceConstants.SCRIPT_ERROR_HANDLING, CommonUtils.fromOrdinal(SQLScriptErrorHandling.class, errorHandlingCombo.getSelectionIndex()).name());
            store.setValue(SQLPreferenceConstants.SCRIPT_FETCH_RESULT_SETS, fetchResultSetsCheck.getSelection());
            store.setValue(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE, resetCursorCheck.getSelection());
            store.setValue(SQLPreferenceConstants.MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE, maxEditorCheck.getSelection());
            store.setValue(
                SQLPreferenceConstants.SHOW_STATISTICS_ON_EXECUTION,
                StatisticsTabOnExecutionBehavior.getByTitle(showStatisticsCombo.getText()).name()
            );
            store.setValue(
                    SQLPreferenceConstants.SET_SELECTION_TO_STATISTICS_TAB,
                    setSelectionToStatisticsTabCheck.getSelection()
            );
            store.setValue(
                SQLPreferenceConstants.CLOSE_INCLUDED_SCRIPT_AFTER_EXECUTION,
                closeIncludedScriptAfterExecutionCheck.getSelection()
            );

            store.setValue(ModelPreferences.SCRIPT_STATEMENT_DELIMITER, statementDelimiterText.getText());
            store.setValue(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER, ignoreNativeDelimiter.getSelection());
            store.setValue(
                ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK,
                SQLScriptStatementDelimiterMode.values()[blankLineDelimiterCombo.getSelectionIndex()].getName()
            );
            store.setValue(ModelPreferences.QUERY_REMOVE_TRAILING_DELIMITER, removeTrailingDelimiter.getSelection());

            store.setValue(ModelPreferences.SQL_PARAMETERS_ENABLED, enableSQLParameters.getSelection());
            store.setValue(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED, enableSQLAnonymousParameters.getSelection());
            store.setValue(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK, anonymousParameterMarkText.getText());
            store.setValue(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX, namedParameterPrefixText.getText());
            store.setValue(ModelPreferences.SQL_CONTROL_COMMAND_PREFIX, controlCommandPrefixText.getText());
            store.setValue(ModelPreferences.SQL_PARAMETERS_IN_EMBEDDED_CODE_ENABLED, enableParametersInEmbeddedCode.getSelection());
            store.setValue(ModelPreferences.SQL_VARIABLES_ENABLED, enableVariables.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store) {
        store.setToDefault(SQLPreferenceConstants.STATEMENT_INVALIDATE_BEFORE_EXECUTE);
        store.setToDefault(SQLPreferenceConstants.STATEMENT_TIMEOUT);

        store.setToDefault(SQLPreferenceConstants.SCRIPT_COMMIT_TYPE);
        store.setToDefault(SQLPreferenceConstants.SCRIPT_ERROR_HANDLING);
        store.setToDefault(SQLPreferenceConstants.SCRIPT_COMMIT_LINES);
        store.setToDefault(SQLPreferenceConstants.SCRIPT_FETCH_RESULT_SETS);

        store.setToDefault(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE);
        store.setToDefault(SQLPreferenceConstants.MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE);

        store.setToDefault(ModelPreferences.SCRIPT_STATEMENT_DELIMITER);
        store.setToDefault(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER);
        store.setToDefault(ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK);
        store.setToDefault(ModelPreferences.QUERY_REMOVE_TRAILING_DELIMITER);

        store.setToDefault(ModelPreferences.SQL_PARAMETERS_ENABLED);
        store.setToDefault(ModelPreferences.SQL_PARAMETERS_IN_EMBEDDED_CODE_ENABLED);
        store.setToDefault(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED);
        store.setToDefault(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK);
        store.setToDefault(ModelPreferences.SQL_CONTROL_COMMAND_PREFIX);
        store.setToDefault(ModelPreferences.SQL_VARIABLES_ENABLED);

        store.setToDefault(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX);
        store.setToDefault(SQLPreferenceConstants.BEEP_ON_QUERY_END);
        store.setToDefault(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE);
        store.setToDefault(SQLPreferenceConstants.CLEAR_OUTPUT_BEFORE_EXECUTE);
    }

    @Override
    protected void performDefaults() {
        loadPreferences(getTargetPreferenceStore(), true);
        super.performDefaults();
    }

    @Override
    protected String getPropertyPageID() {
        return PAGE_ID;
    }

    private void loadPreferences(DBPPreferenceStore store, boolean useDefaults) {
        try {
            invalidateBeforeExecuteCheck.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.STATEMENT_INVALIDATE_BEFORE_EXECUTE)
                    : store.getBoolean(SQLPreferenceConstants.STATEMENT_INVALIDATE_BEFORE_EXECUTE)
            );
            executeTimeoutText.setSelection(
                useDefaults
                    ? store.getDefaultInt(SQLPreferenceConstants.STATEMENT_TIMEOUT)
                    : store.getInt(SQLPreferenceConstants.STATEMENT_TIMEOUT)
            );
            soundOnQueryEnd.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.BEEP_ON_QUERY_END)
                    : store.getBoolean(SQLPreferenceConstants.BEEP_ON_QUERY_END)
            );
            updateDefaultAfterExecute.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE)
                    : store.getBoolean(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE)
            );
            clearOutputBeforeExecute.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.CLEAR_OUTPUT_BEFORE_EXECUTE)
                    : store.getBoolean(SQLPreferenceConstants.CLEAR_OUTPUT_BEFORE_EXECUTE)
            );

            SQLScriptCommitType commitType = CommonUtils.valueOf(
                SQLScriptCommitType.class,
                useDefaults ? null : store.getString(SQLPreferenceConstants.SCRIPT_COMMIT_TYPE),
                SQLScriptCommitType.valueOf(store.getDefaultString(SQLPreferenceConstants.SCRIPT_COMMIT_TYPE))
            );
            commitTypeCombo.select(commitType.ordinal());

            SQLScriptErrorHandling errorHandling = CommonUtils.valueOf(
                SQLScriptErrorHandling.class,
                useDefaults ? null : store.getString(SQLPreferenceConstants.SCRIPT_ERROR_HANDLING),
                SQLScriptErrorHandling.valueOf(store.getDefaultString(SQLPreferenceConstants.SCRIPT_ERROR_HANDLING))
            );
            errorHandlingCombo.select(errorHandling.ordinal());

            commitLinesText.setSelection(
                useDefaults
                    ? store.getDefaultInt(SQLPreferenceConstants.SCRIPT_COMMIT_LINES)
                    : store.getInt(SQLPreferenceConstants.SCRIPT_COMMIT_LINES)
            );
            fetchResultSetsCheck.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.SCRIPT_FETCH_RESULT_SETS)
                    : store.getBoolean(SQLPreferenceConstants.SCRIPT_FETCH_RESULT_SETS)
            );
            resetCursorCheck.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE)
                    : store.getBoolean(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE)
            );
            maxEditorCheck.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE)
                    : store.getBoolean(SQLPreferenceConstants.MAXIMIZE_EDITOR_ON_SCRIPT_EXECUTE)
            );
            UIUtils.setComboSelection(
                showStatisticsCombo,
                StatisticsTabOnExecutionBehavior.getByName(
                    useDefaults
                        ? store.getDefaultString(SQLPreferenceConstants.SHOW_STATISTICS_ON_EXECUTION)
                        : store.getString(SQLPreferenceConstants.SHOW_STATISTICS_ON_EXECUTION)
                ).getTitle()
            );
            setSelectionToStatisticsTabCheck.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.SET_SELECTION_TO_STATISTICS_TAB)
                    : store.getBoolean(SQLPreferenceConstants.SET_SELECTION_TO_STATISTICS_TAB)
            );
            closeIncludedScriptAfterExecutionCheck.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(SQLPreferenceConstants.CLOSE_INCLUDED_SCRIPT_AFTER_EXECUTION)
                    : store.getBoolean(SQLPreferenceConstants.CLOSE_INCLUDED_SCRIPT_AFTER_EXECUTION)
            );
            statementDelimiterText.setText(
                useDefaults
                    ? store.getDefaultString(ModelPreferences.SCRIPT_STATEMENT_DELIMITER)
                    : store.getString(ModelPreferences.SCRIPT_STATEMENT_DELIMITER)
            );
            ignoreNativeDelimiter.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER)
                    : store.getBoolean(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER)
            );
            UIUtils.setComboSelection(
                blankLineDelimiterCombo,
                SQLScriptStatementDelimiterMode.valueByName(
                    useDefaults
                        ? store.getDefaultString(ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK)
                        : store.getString(ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK)
                ).getTitle()
            );
            removeTrailingDelimiter.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(ModelPreferences.QUERY_REMOVE_TRAILING_DELIMITER)
                    : store.getBoolean(ModelPreferences.QUERY_REMOVE_TRAILING_DELIMITER)
            );
            enableSQLParameters.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(ModelPreferences.SQL_PARAMETERS_ENABLED)
                    : store.getBoolean(ModelPreferences.SQL_PARAMETERS_ENABLED)
            );
            enableSQLAnonymousParameters.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED)
                    : store.getBoolean(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED)
            );
            anonymousParameterMarkText.setText(
                useDefaults
                    ? store.getDefaultString(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK)
                    : store.getString(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK)
            );
            namedParameterPrefixText.setText(
                useDefaults
                    ? store.getDefaultString(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX)
                    : store.getString(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX)
            );
            controlCommandPrefixText.setText(
                useDefaults
                    ? store.getDefaultString(ModelPreferences.SQL_CONTROL_COMMAND_PREFIX)
                    : store.getString(ModelPreferences.SQL_CONTROL_COMMAND_PREFIX)
            );
            enableParametersInEmbeddedCode.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(ModelPreferences.SQL_PARAMETERS_IN_EMBEDDED_CODE_ENABLED)
                    : store.getBoolean(ModelPreferences.SQL_PARAMETERS_IN_EMBEDDED_CODE_ENABLED)
            );
            enableVariables.setSelection(
                useDefaults
                    ? store.getDefaultBoolean(ModelPreferences.SQL_VARIABLES_ENABLED)
                    : store.getBoolean(ModelPreferences.SQL_VARIABLES_ENABLED)
            );
        } catch (Exception e) {
            log.warn(e);
        }
    }

}