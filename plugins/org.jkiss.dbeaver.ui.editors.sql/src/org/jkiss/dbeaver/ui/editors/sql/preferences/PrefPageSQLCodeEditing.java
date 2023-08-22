/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;

/**
 * PrefPageSQLCodeEditing
 */
public class PrefPageSQLCodeEditing extends TargetPrefPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.codeeditor"; //$NON-NLS-1$

    // Folding
    private Button csFoldingEnabled;
    private Button csSmartWordsIterator;
    // Highlighting
    private Button csMarkOccurrencesUnderCursor;
    private Button csMarkOccurrencesForSelection;
    private Button csProblemMarkersEnabled;
    // Auto-close
    private Button acSingleQuotesCheck;
    private Button acDoubleQuotesCheck;
    private Button acBracketsCheck;
    // Auto-Format
    private Button afKeywordCase;
    private Button afExtractFromSource;


    public PrefPageSQLCodeEditing() {
        super();
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);

        // Folding
        {
            Composite foldingGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_completion_group_misc, 2, GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            csFoldingEnabled = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_folding_enabled, SQLEditorMessages.pref_page_sql_completion_label_folding_enabled_tip, false, 2);
            csSmartWordsIterator = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_smart_word_iterator, SQLEditorMessages.pref_page_sql_completion_label_smart_word_iterator_tip, false, 2);
            csMarkOccurrencesUnderCursor = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences_tip, false, 2);
            csMarkOccurrencesForSelection = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences_for_selections, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences_for_selections_tip, false, 2);
            csProblemMarkersEnabled = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_problem_markers_enabled, SQLEditorMessages.pref_page_sql_completion_label_problem_markers_enabled_tip, false, 2);
        }

        // Autoclose
        {
            Composite acGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_format_group_auto_close, 1, GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING, 0);

            acSingleQuotesCheck = UIUtils.createCheckbox(acGroup, SQLEditorMessages.pref_page_sql_format_label_single_quotes, false);
            acDoubleQuotesCheck = UIUtils.createCheckbox(acGroup, SQLEditorMessages.pref_page_sql_format_label_double_quotes, false);
            acBracketsCheck = UIUtils.createCheckbox(acGroup, SQLEditorMessages.pref_page_sql_format_label_brackets, false);
        }

        {
            // Formatting
            Composite afGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_format_group_auto_format, 1, GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING, 0);
            afKeywordCase = UIUtils.createCheckbox(
                afGroup,
                SQLEditorMessages.pref_page_sql_format_label_convert_keyword_case,
                SQLEditorMessages.pref_page_sql_format_label_convert_keyword_case_tip,
                false, 1);
            afExtractFromSource = UIUtils.createCheckbox(
                afGroup,
                SQLEditorMessages.pref_page_sql_format_label_extract_sql_from_source_code,
                SQLEditorMessages.pref_page_sql_format_label_extract_sql_from_source_code_tip, false, 1);
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store) {
        csFoldingEnabled.setSelection(store.getBoolean(SQLPreferenceConstants.FOLDING_ENABLED));
        csSmartWordsIterator.setSelection(store.getBoolean(SQLPreferenceConstants.SMART_WORD_ITERATOR));
        csMarkOccurrencesUnderCursor.setSelection(store.getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR));
        csMarkOccurrencesForSelection.setSelection(store.getBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION));
        csProblemMarkersEnabled.setSelection(store.getBoolean(SQLPreferenceConstants.PROBLEM_MARKERS_ENABLED));

        acSingleQuotesCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES));
        acDoubleQuotesCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES));
        acBracketsCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS));

        afKeywordCase.setSelection(store.getBoolean(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO));
        afExtractFromSource.setSelection(store.getBoolean(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE));
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store) {
        store.setValue(SQLPreferenceConstants.FOLDING_ENABLED, csFoldingEnabled.getSelection());
        store.setValue(SQLPreferenceConstants.SMART_WORD_ITERATOR, csSmartWordsIterator.getSelection());
        store.setValue(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR, csMarkOccurrencesUnderCursor.getSelection());
        store.setValue(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION, csMarkOccurrencesForSelection.getSelection());
        store.setValue(SQLPreferenceConstants.PROBLEM_MARKERS_ENABLED, csProblemMarkersEnabled.getSelection());

        store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES, acSingleQuotesCheck.getSelection());
        store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES, acDoubleQuotesCheck.getSelection());
        store.setValue(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS, acBracketsCheck.getSelection());

        store.setValue(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO, afKeywordCase.getSelection());
        store.setValue(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE, afExtractFromSource.getSelection());
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store) {
        store.setToDefault(SQLPreferenceConstants.FOLDING_ENABLED);
        store.setToDefault(SQLPreferenceConstants.SMART_WORD_ITERATOR);
        store.setToDefault(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR);
        store.setToDefault(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION);
        store.setToDefault(SQLPreferenceConstants.PROBLEM_MARKERS_ENABLED);

        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES);
        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES);
        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS);

        store.setToDefault(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO);
        store.setToDefault(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE);
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer container) {
        final DBPPreferenceStore store = container.getPreferenceStore();
        return store.contains(SQLPreferenceConstants.FOLDING_ENABLED)
            || store.contains(SQLPreferenceConstants.SMART_WORD_ITERATOR)
            || store.contains(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR)
            || store.contains(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION)
            || store.contains(SQLPreferenceConstants.PROBLEM_MARKERS_ENABLED)
            || store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES)
            || store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES)
            || store.contains(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS)
            || store.contains(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO)
            || store.contains(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE);
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return true;
    }

    @Override
    protected String getPropertyPageID() {
        return PrefPageSQLCodeEditing.PAGE_ID;
    }
}