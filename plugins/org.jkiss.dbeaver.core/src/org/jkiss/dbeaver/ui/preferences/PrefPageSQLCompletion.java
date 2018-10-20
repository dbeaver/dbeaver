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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.core.CoreMessages;

/**
 * PrefPageSQLEditor
 */
public class PrefPageSQLCompletion extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.completion"; //$NON-NLS-1$

    private Button csAutoActivationCheck;
    private Spinner csAutoActivationDelaySpinner;
    private Button csAutoActivateOnKeystroke;
    private Button csAutoInsertCheck;
    private Combo csInsertCase;
    private Button csReplaceWordAfter;
    private Button csHideDuplicates;
    private Button csShortName;
    private Button csLongName;
    private Button csInsertSpace;
    private Button csMatchContains;
    private Button csUseGlobalSearch;
    private Button csShowColumnProcedures;

    // Auto-close
    private Button acSingleQuotesCheck;
    private Button acDoubleQuotesCheck;
    private Button acBracketsCheck;
    // Auto-Format
    private Button afKeywordCase;
    private Button afExtractFromSource;

    public PrefPageSQLCompletion()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION) ||
            store.contains(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY) ||
            store.contains(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION) ||
            store.contains(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO) ||
            store.contains(SQLPreferenceConstants.PROPOSAL_INSERT_CASE) ||
            store.contains(SQLPreferenceConstants.PROPOSAL_REPLACE_WORD) ||
            store.contains(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS) ||
            store.contains(SQLPreferenceConstants.PROPOSAL_SHORT_NAME) ||
            store.contains(SQLPreferenceConstants.PROPOSAL_ALWAYS_FQ) ||
            store.contains(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS) ||
            store.contains(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS) ||
            store.contains(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT) ||
            store.contains(SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES) ||

            store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES) ||
            store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES) ||
            store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS) ||
            store.contains(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO) ||
            store.contains(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE)
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

        // Content assistant
        {
            Composite assistGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_sql_completion_group_sql_assistant, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            csAutoActivationCheck = UIUtils.createCheckbox(assistGroup, CoreMessages.pref_page_sql_completion_label_enable_auto_activation, CoreMessages.pref_page_sql_completion_label_enable_auto_activation_tip, false, 2);

            UIUtils.createControlLabel(assistGroup, CoreMessages.pref_page_sql_completion_label_auto_activation_delay);
            csAutoActivationDelaySpinner = new Spinner(assistGroup, SWT.BORDER);
            csAutoActivationDelaySpinner.setSelection(0);
            csAutoActivationDelaySpinner.setDigits(0);
            csAutoActivationDelaySpinner.setIncrement(50);
            csAutoActivationDelaySpinner.setMinimum(0);
            csAutoActivationDelaySpinner.setMaximum(1000000);
            csAutoActivationDelaySpinner.setToolTipText(CoreMessages.pref_page_sql_completion_label_set_auto_activation_delay_tip);

            csAutoActivateOnKeystroke = UIUtils.createCheckbox(
                assistGroup,
                CoreMessages.pref_page_sql_completion_label_activate_on_typing,
                CoreMessages.pref_page_sql_completion_label_activate_on_typing_tip,
                false, 2);
            csAutoInsertCheck = UIUtils.createCheckbox(
                assistGroup,
                CoreMessages.pref_page_sql_completion_label_auto_insert_proposal,
                CoreMessages.pref_page_sql_completion_label_auto_insert_proposal_tip,
                false, 2);

            UIUtils.createControlLabel(assistGroup, CoreMessages.pref_page_sql_completion_label_insert_case);
            csInsertCase = new Combo(assistGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            csInsertCase.add("Default");
            csInsertCase.add("Upper case");
            csInsertCase.add("Lower case");

            csReplaceWordAfter = UIUtils.createCheckbox(assistGroup, CoreMessages.pref_page_sql_completion_label_replace_word_after, CoreMessages.pref_page_sql_completion_label_replace_word_after_tip, false, 2);
            csHideDuplicates = UIUtils.createCheckbox(assistGroup, CoreMessages.pref_page_sql_completion_label_hide_duplicate_names, null, false, 2);
            csShortName = UIUtils.createCheckbox(assistGroup, CoreMessages.pref_page_sql_completion_label_use_short_names, null, false, 2);
            csLongName = UIUtils.createCheckbox(assistGroup, CoreMessages.pref_page_sql_completion_label_use_long_names, null, false, 2);
            csInsertSpace = UIUtils.createCheckbox(assistGroup, CoreMessages.pref_page_sql_completion_label_insert_space, null, false, 2);
            csMatchContains = UIUtils.createCheckbox(assistGroup, CoreMessages.pref_page_sql_completion_label_match_contains, CoreMessages.pref_page_sql_completion_label_match_contains_tip, false, 2);
            csUseGlobalSearch = UIUtils.createCheckbox(assistGroup, CoreMessages.pref_page_sql_completion_label_use_global_search, CoreMessages.pref_page_sql_completion_label_use_global_search_tip, false, 2);
            csShowColumnProcedures = UIUtils.createCheckbox(assistGroup, CoreMessages.pref_page_sql_completion_label_show_column_procedures, CoreMessages.pref_page_sql_completion_label_show_column_procedures_tip, false, 2);
        }

        Composite autoFormatPanel = new Composite(composite, SWT.NONE);
        autoFormatPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        autoFormatPanel.setLayout(new GridLayout(1, false));

        // Autoclose
        {
            Composite acGroup = UIUtils.createControlGroup(autoFormatPanel, CoreMessages.pref_page_sql_format_group_auto_close, 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);

            acSingleQuotesCheck = UIUtils.createCheckbox(acGroup, CoreMessages.pref_page_sql_format_label_single_quotes, false);
            acDoubleQuotesCheck = UIUtils.createCheckbox(acGroup, CoreMessages.pref_page_sql_format_label_double_quotes, false);
            acBracketsCheck = UIUtils.createCheckbox(acGroup, CoreMessages.pref_page_sql_format_label_brackets, false);
        }

        {
            // Formatting
            Composite afGroup = UIUtils.createControlGroup(autoFormatPanel, CoreMessages.pref_page_sql_format_group_auto_format, 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
            afKeywordCase = UIUtils.createCheckbox(
                afGroup,
                CoreMessages.pref_page_sql_format_label_convert_keyword_case,
                CoreMessages.pref_page_sql_format_label_convert_keyword_case_tip,
                false, 1);
            afExtractFromSource = UIUtils.createCheckbox(
                afGroup,
                CoreMessages.pref_page_sql_format_label_extract_sql_from_source_code,
                CoreMessages.pref_page_sql_format_label_extract_sql_from_source_code_tip, false, 1);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            csAutoActivationCheck.setSelection(store.getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION));
            csAutoActivationDelaySpinner.setSelection(store.getInt(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY));
            csAutoActivateOnKeystroke.setSelection(store.getBoolean(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION));
            csAutoInsertCheck.setSelection(store.getBoolean(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO));
            csInsertCase.select(store.getInt(SQLPreferenceConstants.PROPOSAL_INSERT_CASE));

            csReplaceWordAfter.setSelection(store.getBoolean(SQLPreferenceConstants.PROPOSAL_REPLACE_WORD));
            csHideDuplicates.setSelection(store.getBoolean(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS));
            csShortName.setSelection(store.getBoolean(SQLPreferenceConstants.PROPOSAL_SHORT_NAME));
            csLongName.setSelection(store.getBoolean(SQLPreferenceConstants.PROPOSAL_ALWAYS_FQ));
            csInsertSpace.setSelection(store.getBoolean(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS));
            csMatchContains.setSelection(store.getBoolean(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS));
            csUseGlobalSearch.setSelection(store.getBoolean(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT));
            csShowColumnProcedures.setSelection(store.getBoolean(SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES));

            acSingleQuotesCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES));
            acDoubleQuotesCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES));
            acBracketsCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS));
            afKeywordCase.setSelection(store.getBoolean(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO));
            afExtractFromSource.setSelection(store.getBoolean(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE));

        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, csAutoActivationCheck.getSelection());
            store.setValue(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, csAutoActivationDelaySpinner.getSelection());
            store.setValue(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION, csAutoActivateOnKeystroke.getSelection());
            store.setValue(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, csAutoInsertCheck.getSelection());
            store.setValue(SQLPreferenceConstants.PROPOSAL_INSERT_CASE, csInsertCase.getSelectionIndex());
            store.setValue(SQLPreferenceConstants.PROPOSAL_REPLACE_WORD, csReplaceWordAfter.getSelection());
            store.setValue(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS, csHideDuplicates.getSelection());
            store.setValue(SQLPreferenceConstants.PROPOSAL_SHORT_NAME, csShortName.getSelection());
            store.setValue(SQLPreferenceConstants.PROPOSAL_ALWAYS_FQ, csLongName.getSelection());
            store.setValue(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS, csInsertSpace.getSelection());
            store.setValue(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS, csMatchContains.getSelection());
            store.setValue(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT, csUseGlobalSearch.getSelection());
            store.setValue(SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES, csShowColumnProcedures.getSelection());

            store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, acSingleQuotesCheck.getSelection());
            store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, acDoubleQuotesCheck.getSelection());
            store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, acBracketsCheck.getSelection());

            store.setValue(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO, afKeywordCase.getSelection());
            store.setValue(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE, afExtractFromSource.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION);
        store.setToDefault(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY);
        store.setToDefault(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION);
        store.setToDefault(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO);
        store.setToDefault(SQLPreferenceConstants.PROPOSAL_INSERT_CASE);

        store.setToDefault(SQLPreferenceConstants.PROPOSAL_REPLACE_WORD);
        store.setToDefault(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS);
        store.setToDefault(SQLPreferenceConstants.PROPOSAL_SHORT_NAME);
        store.setToDefault(SQLPreferenceConstants.PROPOSAL_ALWAYS_FQ);
        store.setToDefault(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS);
        store.setToDefault(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS);
        store.setToDefault(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT);
        store.setToDefault(SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES);

        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES);
        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES);
        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS);
        store.setToDefault(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO);
        store.setToDefault(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}