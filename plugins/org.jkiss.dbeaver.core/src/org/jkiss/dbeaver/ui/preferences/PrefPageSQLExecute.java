/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.runtime.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageSQLExecute
 */
public class PrefPageSQLExecute extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sqlexecute"; //$NON-NLS-1$

    private Button invalidateBeforeExecuteCheck;
    private Spinner executeTimeoutText;
    private Button soundOnQueryEnd;
    private Button updateDefaultAfterExecute;

    private Combo commitTypeCombo;
    private Combo errorHandlingCombo;
    private Spinner commitLinesText;
    private Button fetchResultSetsCheck;
    private Button resetCursorCheck;

    private Text statementDelimiterText;
    private Button ignoreNativeDelimiter;
    private Button blankLineDelimiter;

    private Button enableSQLParameters;
    private Button enableSQLAnonymousParameters;
    private Text anonymousParameterMarkText;
    private Text namedParameterPrefixText;
    private Button enableParametersInDDL;

    public PrefPageSQLExecute()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE) ||
            store.contains(DBeaverPreferences.STATEMENT_TIMEOUT) ||

            store.contains(DBeaverPreferences.SCRIPT_COMMIT_TYPE) ||
            store.contains(DBeaverPreferences.SCRIPT_ERROR_HANDLING) ||
            store.contains(DBeaverPreferences.SCRIPT_COMMIT_LINES) ||
            store.contains(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS) ||

            store.contains(ModelPreferences.SCRIPT_STATEMENT_DELIMITER) ||
            store.contains(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER) ||
            store.contains(ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK) ||
            store.contains(ModelPreferences.SQL_PARAMETERS_ENABLED) ||
            store.contains(ModelPreferences.SQL_PARAMETERS_IN_DDL_ENABLED) ||
            store.contains(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED) ||
            store.contains(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK) ||
            store.contains(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX) ||
            store.contains(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE) ||
            store.contains(SQLPreferenceConstants.BEEP_ON_QUERY_END) ||
            store.contains(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE)
        ;
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    @Override
    protected Control createPreferenceContent(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        // General settings
        {
            Composite commonGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_sql_editor_group_common, 2, GridData.FILL_HORIZONTAL, 0);
            UIUtils.setGridSpan(commonGroup, 2, 1);
            {
                invalidateBeforeExecuteCheck = UIUtils.createCheckbox(commonGroup, CoreMessages.pref_page_sql_editor_label_invalidate_before_execute, null, false, 2);
                soundOnQueryEnd = UIUtils.createCheckbox(commonGroup, CoreMessages.pref_page_sql_editor_label_sound_on_query_end, null, false, 2);
                updateDefaultAfterExecute = UIUtils.createCheckbox(commonGroup, CoreMessages.pref_page_sql_editor_label_refresh_defaults_after_execute, null, false, 2);

                UIUtils.createControlLabel(commonGroup, CoreMessages.pref_page_sql_editor_label_sql_timeout);
                executeTimeoutText = new Spinner(commonGroup, SWT.BORDER);
                executeTimeoutText.setSelection(0);
                executeTimeoutText.setDigits(0);
                executeTimeoutText.setIncrement(1);
                executeTimeoutText.setMinimum(0);
                executeTimeoutText.setMaximum(100000);
                executeTimeoutText.setToolTipText(CoreMessages.pref_page_sql_editor_label_sql_timeout_tip);

            }
        }

        // Scripts
        {
            Composite scriptsGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_sql_editor_group_scripts, 2, GridData.FILL_HORIZONTAL, 0);
            UIUtils.setGridSpan(scriptsGroup, 2, 1);
            {
                UIUtils.createControlLabel(scriptsGroup, CoreMessages.pref_page_sql_editor_label_commit_type);

                commitTypeCombo = new Combo(scriptsGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                commitTypeCombo.add(CoreMessages.pref_page_sql_editor_combo_item_script_end, SQLScriptCommitType.AT_END.ordinal());
                commitTypeCombo.add(CoreMessages.pref_page_sql_editor_combo_item_each_line_autocommit, SQLScriptCommitType.AUTOCOMMIT.ordinal());
                commitTypeCombo.add(CoreMessages.pref_page_sql_editor_combo_item_each_spec_line, SQLScriptCommitType.NLINES.ordinal());
                commitTypeCombo.add(CoreMessages.pref_page_sql_editor_combo_item_no_commit, SQLScriptCommitType.NO_COMMIT.ordinal());
            }

            {
                UIUtils.createControlLabel(scriptsGroup, CoreMessages.pref_page_sql_editor_label_commit_after_line);
                commitLinesText = new Spinner(scriptsGroup, SWT.BORDER);
                commitLinesText.setSelection(0);
                commitLinesText.setDigits(0);
                commitLinesText.setIncrement(1);
                commitLinesText.setMinimum(1);
                commitLinesText.setMaximum(1024 * 1024);
            }

            {
                UIUtils.createControlLabel(scriptsGroup, CoreMessages.pref_page_sql_editor_label_error_handling);

                errorHandlingCombo = new Combo(scriptsGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
                errorHandlingCombo.add(CoreMessages.pref_page_sql_editor_combo_item_stop_rollback, SQLScriptErrorHandling.STOP_ROLLBACK.ordinal());
                errorHandlingCombo.add(CoreMessages.pref_page_sql_editor_combo_item_stop_commit, SQLScriptErrorHandling.STOP_COMMIT.ordinal());
                errorHandlingCombo.add(CoreMessages.pref_page_sql_editor_combo_item_ignore, SQLScriptErrorHandling.IGNORE.ordinal());
            }

            fetchResultSetsCheck = UIUtils.createCheckbox(scriptsGroup, CoreMessages.pref_page_sql_editor_checkbox_fetch_resultsets, null, false, 2);
            resetCursorCheck = UIUtils.createCheckbox(scriptsGroup, CoreMessages.pref_page_sql_editor_checkbox_reset_cursor, null, false, 2);
        }
        // Parameters
        {
            Composite paramsGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_sql_editor_group_parameters, 2, GridData.FILL_HORIZONTAL, 0);
            enableSQLParameters = UIUtils.createCheckbox(paramsGroup, CoreMessages.pref_page_sql_editor_checkbox_enable_sql_parameters, null, false, 2);
            enableSQLAnonymousParameters = UIUtils.createCheckbox(paramsGroup, CoreMessages.pref_page_sql_editor_checkbox_enable_sql_anonymous_parameters, null, false, 2);
            anonymousParameterMarkText = UIUtils.createLabelText(paramsGroup, CoreMessages.pref_page_sql_editor_text_anonymous_parameter_mark, "", SWT.BORDER, new GridData(32, SWT.DEFAULT));
            anonymousParameterMarkText.setTextLimit(1);
            namedParameterPrefixText = UIUtils.createLabelText(paramsGroup, CoreMessages.pref_page_sql_editor_text_named_parameter_prefix, "", SWT.BORDER, new GridData(32, SWT.DEFAULT));
            namedParameterPrefixText.setTextLimit(1);
            enableParametersInDDL = UIUtils.createCheckbox(paramsGroup, CoreMessages.pref_page_sql_editor_enable_parameters_in_ddl, null, false, 2);
        }

        // Delimiters
        {
            Composite delimGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_sql_editor_group_delimiters, 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
            statementDelimiterText = UIUtils.createLabelText(delimGroup, CoreMessages.pref_page_sql_editor_text_statement_delimiter, "", SWT.BORDER, new GridData(32, SWT.DEFAULT));
            //statementDelimiterText.setTextLimit(1);
            ignoreNativeDelimiter = UIUtils.createCheckbox(delimGroup, CoreMessages.pref_page_sql_editor_checkbox_ignore_native_delimiter, null, false, 2);
            blankLineDelimiter = UIUtils.createCheckbox(delimGroup, CoreMessages.pref_page_sql_editor_checkbox_blank_line_delimiter, null, false, 2);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            invalidateBeforeExecuteCheck.setSelection(store.getBoolean(DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE));
            executeTimeoutText.setSelection(store.getInt(DBeaverPreferences.STATEMENT_TIMEOUT));
            soundOnQueryEnd.setSelection(store.getBoolean(SQLPreferenceConstants.BEEP_ON_QUERY_END));
            updateDefaultAfterExecute.setSelection(store.getBoolean(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE));

            commitTypeCombo.select(SQLScriptCommitType.valueOf(store.getString(DBeaverPreferences.SCRIPT_COMMIT_TYPE)).ordinal());
            errorHandlingCombo.select(SQLScriptErrorHandling.valueOf(store.getString(DBeaverPreferences.SCRIPT_ERROR_HANDLING)).ordinal());
            commitLinesText.setSelection(store.getInt(DBeaverPreferences.SCRIPT_COMMIT_LINES));
            fetchResultSetsCheck.setSelection(store.getBoolean(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS));
            resetCursorCheck.setSelection(store.getBoolean(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE));

            statementDelimiterText.setText(store.getString(ModelPreferences.SCRIPT_STATEMENT_DELIMITER));
            ignoreNativeDelimiter.setSelection(store.getBoolean(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER));
            blankLineDelimiter.setSelection(store.getBoolean(ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK));

            enableSQLParameters.setSelection(store.getBoolean(ModelPreferences.SQL_PARAMETERS_ENABLED));
            enableSQLAnonymousParameters.setSelection(store.getBoolean(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED));
            anonymousParameterMarkText.setText(store.getString(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK));
            namedParameterPrefixText.setText(store.getString(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX));
            enableParametersInDDL.setSelection(store.getBoolean(ModelPreferences.SQL_PARAMETERS_IN_DDL_ENABLED));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE, invalidateBeforeExecuteCheck.getSelection());
            store.setValue(DBeaverPreferences.STATEMENT_TIMEOUT, executeTimeoutText.getSelection());
            store.setValue(SQLPreferenceConstants.BEEP_ON_QUERY_END, soundOnQueryEnd.getSelection());
            store.setValue(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE, updateDefaultAfterExecute.getSelection());

            store.setValue(DBeaverPreferences.SCRIPT_COMMIT_TYPE, CommonUtils.fromOrdinal(SQLScriptCommitType.class, commitTypeCombo.getSelectionIndex()).name());
            store.setValue(DBeaverPreferences.SCRIPT_COMMIT_LINES, commitLinesText.getSelection());
            store.setValue(DBeaverPreferences.SCRIPT_ERROR_HANDLING, CommonUtils.fromOrdinal(SQLScriptErrorHandling.class, errorHandlingCombo.getSelectionIndex()).name());
            store.setValue(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS, fetchResultSetsCheck.getSelection());
            store.setValue(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE, resetCursorCheck.getSelection());

            store.setValue(ModelPreferences.SCRIPT_STATEMENT_DELIMITER, statementDelimiterText.getText());
            store.setValue(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER, ignoreNativeDelimiter.getSelection());
            store.setValue(ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK, blankLineDelimiter.getSelection());

            store.setValue(ModelPreferences.SQL_PARAMETERS_ENABLED, enableSQLParameters.getSelection());
            store.setValue(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED, enableSQLAnonymousParameters.getSelection());
            store.setValue(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK, anonymousParameterMarkText.getText());
            store.setValue(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX, namedParameterPrefixText.getText());
            store.setValue(ModelPreferences.SQL_PARAMETERS_IN_DDL_ENABLED, enableParametersInDDL.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE);
        store.setToDefault(DBeaverPreferences.STATEMENT_TIMEOUT);

        store.setToDefault(DBeaverPreferences.SCRIPT_COMMIT_TYPE);
        store.setToDefault(DBeaverPreferences.SCRIPT_ERROR_HANDLING);
        store.setToDefault(DBeaverPreferences.SCRIPT_COMMIT_LINES);
        store.setToDefault(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS);

        store.setToDefault(SQLPreferenceConstants.RESET_CURSOR_ON_EXECUTE);

        store.setToDefault(ModelPreferences.SCRIPT_STATEMENT_DELIMITER);
        store.setToDefault(ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER);

        store.setToDefault(ModelPreferences.SQL_PARAMETERS_ENABLED);
        store.setToDefault(ModelPreferences.SQL_PARAMETERS_IN_DDL_ENABLED);
        store.setToDefault(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_ENABLED);
        store.setToDefault(ModelPreferences.SQL_ANONYMOUS_PARAMETERS_MARK);
        store.setToDefault(ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK);

        store.setToDefault(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX);
        store.setToDefault(SQLPreferenceConstants.BEEP_ON_QUERY_END);
        store.setToDefault(SQLPreferenceConstants.REFRESH_DEFAULTS_AFTER_EXECUTE);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}