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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLSemanticAnalysisDepth;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;

import java.util.List;

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
    private List<Button> btnAnalysisDepth;
    // Auto-close
    private Button acSingleQuotesCheck;
    private Button acDoubleQuotesCheck;
    private Button acBracketsCheck;
    // Auto-Format
    private Button afKeywordCase;
    private Button afExtractFromSource;

    private SQLSemanticAnalysisDepth semanticAnalysisDepth;  

    public PrefPageSQLCodeEditing() {
        super();
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);

        // Folding
        {
            Composite foldingGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_completion_group_misc, 1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            csFoldingEnabled = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_folding_enabled, SQLEditorMessages.pref_page_sql_completion_label_folding_enabled_tip, false, 1);
            csSmartWordsIterator = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_smart_word_iterator, SQLEditorMessages.pref_page_sql_completion_label_smart_word_iterator_tip, false, 1);
            csMarkOccurrencesUnderCursor = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences_tip, false, 1);
            csMarkOccurrencesForSelection = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences_for_selections, SQLEditorMessages.pref_page_sql_completion_label_mark_occurrences_for_selections_tip, false, 1);
            csProblemMarkersEnabled = UIUtils.createCheckbox(foldingGroup, SQLEditorMessages.pref_page_sql_completion_label_problem_markers_enabled, SQLEditorMessages.pref_page_sql_completion_label_problem_markers_enabled_tip, false, 1);
            UIUtils.createHorizontalLine(foldingGroup, 1, 0);           
            Composite analysisDepthGroupContainer = UIUtils.createComposite(foldingGroup, 2);
            analysisDepthGroupContainer.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
            Composite analysisDepthGroup = UIUtils.createComposite(analysisDepthGroupContainer, 1);
            UIUtils.createLabel(analysisDepthGroup, "Experimental query analysis:");
            SelectionListener analysisDepthListener = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    semanticAnalysisDepth = (SQLSemanticAnalysisDepth)e.widget.getData();
                }
            };
            btnAnalysisDepth = List.of(
                UIUtils.createRadioButton(analysisDepthGroup, "No extra analysis", SQLSemanticAnalysisDepth.None, analysisDepthListener),
                UIUtils.createRadioButton(analysisDepthGroup, "Identifiers highlighting", SQLSemanticAnalysisDepth.Highlighting, analysisDepthListener),
                UIUtils.createRadioButton(analysisDepthGroup, "Identifiers highlighting and classification", SQLSemanticAnalysisDepth.Classification, analysisDepthListener),
                UIUtils.createRadioButton(analysisDepthGroup, "Identifiers highlighting, classification and validation (by metadata)", SQLSemanticAnalysisDepth.Validation, analysisDepthListener)
            );
            String msg = "No extra analysis - disable this experimental feature.\n\n"
                    + "Identifiers highlighting - parse query and highlight any thing treated as table or column name.\n\n"
                    + "Highlighting and classification - same as above, plus resolve all the aliases and table names according to FROM clauses.\n\n"
                    + "Classification and validation - same as above, plus validate all the table and column names with respect to real database objects (uses database metadata connection or fails back to 'Highlighting and classification' when connection is not established).";
            UIUtils.createInfoToolButton(analysisDepthGroupContainer, msg)
                   .setLayoutData(GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.BEGINNING).create());
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
        semanticAnalysisDepth = SQLSemanticAnalysisDepth.getPreference(store, SQLPreferenceConstants.SEMANTIC_ANALYSIS_DEPTH);
        btnAnalysisDepth.forEach(b -> b.setSelection(false));
        btnAnalysisDepth.get(semanticAnalysisDepth.value).setSelection(true);

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
        store.setValue(SQLPreferenceConstants.SEMANTIC_ANALYSIS_DEPTH, semanticAnalysisDepth.value);
        
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
        store.setToDefault(SQLPreferenceConstants.SEMANTIC_ANALYSIS_DEPTH);

        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES);
        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES);
        store.setToDefault(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS);

        store.setToDefault(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO);
        store.setToDefault(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE);
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        csFoldingEnabled.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.FOLDING_ENABLED));
        csSmartWordsIterator.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SMART_WORD_ITERATOR));
        csMarkOccurrencesUnderCursor.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR));
        csMarkOccurrencesForSelection.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION));
        csProblemMarkersEnabled.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.PROBLEM_MARKERS_ENABLED));
        
        semanticAnalysisDepth = SQLSemanticAnalysisDepth.fromInt(store.getDefaultInt(SQLPreferenceConstants.SEMANTIC_ANALYSIS_DEPTH));
        btnAnalysisDepth.forEach(b -> b.setSelection(false));
        btnAnalysisDepth.get(semanticAnalysisDepth.value).setSelection(true);

        acSingleQuotesCheck.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_SINGLE_QUOTES));
        acDoubleQuotesCheck.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_DOUBLE_QUOTES));
        acBracketsCheck.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_BRACKETS));
        afKeywordCase.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO));
        afExtractFromSource.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE));
        super.performDefaults();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer container) {
        final DBPPreferenceStore store = container.getPreferenceStore();
        return store.contains(SQLPreferenceConstants.FOLDING_ENABLED)
            || store.contains(SQLPreferenceConstants.SMART_WORD_ITERATOR)
            || store.contains(SQLPreferenceConstants.MARK_OCCURRENCES_UNDER_CURSOR)
            || store.contains(SQLPreferenceConstants.MARK_OCCURRENCES_FOR_SELECTION)
            || store.contains(SQLPreferenceConstants.PROBLEM_MARKERS_ENABLED)
            || store.contains(SQLPreferenceConstants.SEMANTIC_ANALYSIS_DEPTH)
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