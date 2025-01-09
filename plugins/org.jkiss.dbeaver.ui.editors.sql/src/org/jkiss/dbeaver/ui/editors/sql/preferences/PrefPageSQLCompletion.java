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
package org.jkiss.dbeaver.ui.editors.sql.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLModelPreferences;
import org.jkiss.dbeaver.model.sql.SQLTableAliasInsertMode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants.SQLAutocompletionMode;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants.SQLCompletionObjectNameFormKind;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageSQLCompletion
 */
public class PrefPageSQLCompletion extends TargetPrefPage
{
    private static final Log log = Log.getLog(PrefPageSQLCompletion.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.completion"; //$NON-NLS-1$

    private Button csAutoActivationCheck;
    private Spinner csAutoActivationDelaySpinner;
    private Button csAutoActivateOnKeystroke;
    private Button csAutoInsertCheck;
    private Button csTabChoice;
    private Combo csInsertCase;
    private Button csReplaceWordAfter;
    private Button csHideDuplicates;
    private Combo csObjectNameForm;
    private Button csInsertSpace;
    private Button csSortAlphabetically;
    private Button csShowServerHelpTopics;
    private Button csShowValues;
    private Combo csInsertTableAlias;

    private Button csMatchContains;
    private Button csUseGlobalSearch;
    private Button csShowColumnProcedures;
    private Button csHippieActivation;
    private Combo csCompletionMode;

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
            store.contains(SQLPreferenceConstants.AUTOCOMPLETION_MODE) ||
            store.contains(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY) ||
            store.contains(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION) ||
            store.contains(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO) ||
            store.contains(SQLPreferenceConstants.TAB_AUTOCOMPLETION) ||
            store.contains(SQLPreferenceConstants.PROPOSAL_INSERT_CASE) ||
            store.contains(SQLPreferenceConstants.PROPOSAL_REPLACE_WORD) ||
            store.contains(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS) ||
            store.contains(SQLModelPreferences.SQL_EDITOR_PROPOSAL_SHORT_NAME) ||
            store.contains(SQLModelPreferences.SQL_EDITOR_PROPOSAL_ALWAYS_FQ) ||
            store.contains(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS) ||
            store.contains(SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY) ||
            store.contains(SQLModelPreferences.SQL_PROPOSAL_INSERT_TABLE_ALIAS) ||

            store.contains(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS) ||
            store.contains(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT) ||
            store.contains(SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES) ||
            store.contains(SQLPreferenceConstants.SHOW_SERVER_HELP_TOPICS) ||
            store.contains(SQLPreferenceConstants.SHOW_VALUES)
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

        // Content assistant
        {
            Composite assistGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_completion_group_sql_assistant, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            csAutoActivationCheck = UIUtils.createCheckbox(
                assistGroup,
                SQLEditorMessages.pref_page_sql_completion_label_enable_auto_activation,
                SQLEditorMessages.pref_page_sql_completion_label_enable_auto_activation_tip,
                false,
                2
            );
            csHippieActivation = UIUtils.createCheckbox(
                assistGroup,
                SQLEditorMessages.pref_page_sql_completion_label_activate_hippie,
                SQLEditorMessages.pref_page_sql_completion_label_activate_hippie_tip,
                true,
                2
            );
            csCompletionMode = UIUtils.createLabelCombo(
                assistGroup,
                SQLEditorMessages.pref_page_sql_completion_label_completion_mode,
                SQLEditorMessages.pref_page_sql_completion_label_completion_mode_tip,
                SWT.READ_ONLY | SWT.DROP_DOWN);
            for (SQLAutocompletionMode mode : SQLAutocompletionMode.values()) {
                csCompletionMode.add(mode.title);
            }
            
            UIUtils.createControlLabel(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_auto_activation_delay + UIMessages.label_ms);
            csAutoActivationDelaySpinner = new Spinner(assistGroup, SWT.BORDER);
            csAutoActivationDelaySpinner.setSelection(0);
            csAutoActivationDelaySpinner.setDigits(0);
            csAutoActivationDelaySpinner.setIncrement(50);
            csAutoActivationDelaySpinner.setMinimum(0);
            csAutoActivationDelaySpinner.setMaximum(1000000);
            csAutoActivationDelaySpinner.setToolTipText(SQLEditorMessages.pref_page_sql_completion_label_set_auto_activation_delay_tip);

            csAutoActivateOnKeystroke = UIUtils.createCheckbox(
                assistGroup,
                SQLEditorMessages.pref_page_sql_completion_label_activate_on_typing,
                SQLEditorMessages.pref_page_sql_completion_label_activate_on_typing_tip,
                false,
                2
            );
            csAutoInsertCheck = UIUtils.createCheckbox(
                assistGroup,
                SQLEditorMessages.pref_page_sql_completion_label_auto_insert_proposal,
                SQLEditorMessages.pref_page_sql_completion_label_auto_insert_proposal_tip,
                false,
                2
            );
            csTabChoice = UIUtils.createCheckbox(
                assistGroup,
                SQLEditorMessages.pref_page_sql_completion_label_autocomplete_by_tab,
                SQLEditorMessages.pref_page_sql_completion_label_autocomplete_by_tab_tip,
                false,
                2
            );
            
            UIUtils.createControlLabel(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_insert_case);
            csInsertCase = new Combo(assistGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            csInsertCase.add(SQLEditorMessages.pref_page_sql_default);
            csInsertCase.add(SQLEditorMessages.pref_page_sql_insert_case_upper_case);
            csInsertCase.add(SQLEditorMessages.pref_page_sql_insert_case_lower_case);

            csReplaceWordAfter = UIUtils.createCheckbox(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_replace_word_after, SQLEditorMessages.pref_page_sql_completion_label_replace_word_after_tip, false, 2);
            csHideDuplicates = UIUtils.createCheckbox(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_hide_duplicate_names, null, false, 2);
            csObjectNameForm = UIUtils.createLabelCombo(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_object_names_form, SWT.READ_ONLY | SWT.DROP_DOWN);
            for (SQLCompletionObjectNameFormKind mode : SQLCompletionObjectNameFormKind.values()) {
                csObjectNameForm.add(mode.title);
            }
            csInsertSpace = UIUtils.createCheckbox(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_insert_space, null, false, 2);
            csSortAlphabetically = UIUtils.createCheckbox(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_sort_alphabetically, null, false, 2);
            csShowServerHelpTopics = UIUtils.createCheckbox(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_show_server_help_topics, SQLEditorMessages.pref_page_sql_completion_label_show_server_help_topics_tip, false, 2);
            csShowValues = UIUtils.createCheckbox(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_show_values, SQLEditorMessages.pref_page_sql_completion_label_show_values_tip, false, 2);
            csInsertTableAlias = UIUtils.createLabelCombo(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_insert_table_alias, SWT.READ_ONLY | SWT.DROP_DOWN);
            for (SQLTableAliasInsertMode mode : SQLTableAliasInsertMode.values()) {
                csInsertTableAlias.add(mode.getText());
            }
        }

        Composite rightPanel = new Composite(composite, SWT.NONE);
        rightPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        rightPanel.setLayout(new GridLayout(1, false));

        {
            Composite assistGroup = UIUtils.createControlGroup(rightPanel, SQLEditorMessages.pref_page_sql_format_group_search, 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);

            csMatchContains = UIUtils.createCheckbox(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_match_contains, SQLEditorMessages.pref_page_sql_completion_label_match_contains_tip, false, 2);
            csUseGlobalSearch = UIUtils.createCheckbox(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_use_global_search, SQLEditorMessages.pref_page_sql_completion_label_use_global_search_tip, false, 2);
            csShowColumnProcedures = UIUtils.createCheckbox(assistGroup, SQLEditorMessages.pref_page_sql_completion_label_show_column_procedures, SQLEditorMessages.pref_page_sql_completion_label_show_column_procedures_tip, false, 2);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            csAutoActivationCheck.setSelection(store.getBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION));
            csCompletionMode.select(SQLAutocompletionMode.fromPreferences(store).ordinal());
            csHippieActivation.setSelection(store.getBoolean(SQLPreferenceConstants.ENABLE_HIPPIE));
            csAutoActivationDelaySpinner.setSelection(store.getInt(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY));
            csAutoActivateOnKeystroke.setSelection(store.getBoolean(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION));
            csAutoInsertCheck.setSelection(store.getBoolean(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO));
            csTabChoice.setSelection(store.getBoolean(SQLPreferenceConstants.TAB_AUTOCOMPLETION));
            csInsertCase.select(store.getInt(SQLPreferenceConstants.PROPOSAL_INSERT_CASE));

            csReplaceWordAfter.setSelection(store.getBoolean(SQLPreferenceConstants.PROPOSAL_REPLACE_WORD));
            csHideDuplicates.setSelection(store.getBoolean(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS));
            csObjectNameForm.select(SQLCompletionObjectNameFormKind.getFromPreferences(store).ordinal());
            csInsertSpace.setSelection(store.getBoolean(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS));
            csSortAlphabetically.setSelection(store.getBoolean(SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY));
            csShowServerHelpTopics.setSelection(store.getBoolean(SQLPreferenceConstants.SHOW_SERVER_HELP_TOPICS));
            csShowValues.setSelection(store.getBoolean(SQLPreferenceConstants.SHOW_VALUES));
            csInsertTableAlias.select(SQLTableAliasInsertMode.fromPreferences(store).ordinal());

            csMatchContains.setSelection(store.getBoolean(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS));
            csUseGlobalSearch.setSelection(store.getBoolean(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT));
            csShowColumnProcedures.setSelection(store.getBoolean(SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES));

        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION, csAutoActivationCheck.getSelection());
            store.setValue(SQLPreferenceConstants.AUTOCOMPLETION_MODE, SQLAutocompletionMode.values()[csCompletionMode.getSelectionIndex()].getName());
            store.setValue(SQLPreferenceConstants.ENABLE_HIPPIE, csHippieActivation.getSelection());
            store.setValue(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY, csAutoActivationDelaySpinner.getSelection());
            store.setValue(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION, csAutoActivateOnKeystroke.getSelection());
            store.setValue(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO, csAutoInsertCheck.getSelection());
            store.setValue(SQLPreferenceConstants.TAB_AUTOCOMPLETION, csTabChoice.getSelection());
            store.setValue(SQLPreferenceConstants.PROPOSAL_INSERT_CASE, csInsertCase.getSelectionIndex());
            store.setValue(SQLPreferenceConstants.PROPOSAL_REPLACE_WORD, csReplaceWordAfter.getSelection());
            store.setValue(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS, csHideDuplicates.getSelection());
            SQLCompletionObjectNameFormKind.values()[csObjectNameForm.getSelectionIndex()].setToPreferences(store);
            store.setValue(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS, csInsertSpace.getSelection());
            store.setValue(SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY, csSortAlphabetically.getSelection());
            store.setValue(SQLPreferenceConstants.SHOW_SERVER_HELP_TOPICS, csShowServerHelpTopics.getSelection());
            store.setValue(SQLPreferenceConstants.SHOW_VALUES, csShowValues.getSelection());
            store.setValue(SQLModelPreferences.SQL_PROPOSAL_INSERT_TABLE_ALIAS, SQLTableAliasInsertMode.values()[csInsertTableAlias.getSelectionIndex()].name());

            store.setValue(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS, csMatchContains.getSelection());
            store.setValue(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT, csUseGlobalSearch.getSelection());
            store.setValue(SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES, csShowColumnProcedures.getSelection());
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
        store.setToDefault(SQLPreferenceConstants.TAB_AUTOCOMPLETION);
        store.setToDefault(SQLPreferenceConstants.PROPOSAL_INSERT_CASE);
        store.setToDefault(SQLPreferenceConstants.ENABLE_HIPPIE);

        store.setToDefault(SQLPreferenceConstants.PROPOSAL_REPLACE_WORD);
        store.setToDefault(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS);
        store.setToDefault(SQLModelPreferences.SQL_EDITOR_PROPOSAL_SHORT_NAME);
        store.setToDefault(SQLModelPreferences.SQL_EDITOR_PROPOSAL_ALWAYS_FQ);
        store.setToDefault(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS);
        store.setToDefault(SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY);
        store.setToDefault(SQLPreferenceConstants.SHOW_SERVER_HELP_TOPICS);
        store.setToDefault(SQLPreferenceConstants.SHOW_VALUES);
        store.setToDefault(SQLModelPreferences.SQL_PROPOSAL_INSERT_TABLE_ALIAS);

        store.setToDefault(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS);
        store.setToDefault(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT);
        store.setToDefault(SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES);
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        csAutoActivationCheck.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.ENABLE_AUTO_ACTIVATION));
        csAutoActivationDelaySpinner.setSelection(store.getDefaultInt(SQLPreferenceConstants.AUTO_ACTIVATION_DELAY));
        csAutoActivateOnKeystroke.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.ENABLE_KEYSTROKE_ACTIVATION));
        csAutoInsertCheck.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.INSERT_SINGLE_PROPOSALS_AUTO));
        csTabChoice.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.TAB_AUTOCOMPLETION));
        csInsertCase.select(store.getDefaultInt(SQLPreferenceConstants.PROPOSAL_INSERT_CASE));
        csReplaceWordAfter.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.PROPOSAL_REPLACE_WORD));
        csHideDuplicates.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS));
        csObjectNameForm.select(SQLCompletionObjectNameFormKind.getDefaultFromPreferences(store).ordinal());
        csInsertSpace.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS));
        csSortAlphabetically.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY));
        csShowServerHelpTopics.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SHOW_SERVER_HELP_TOPICS));
        csShowValues.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SHOW_VALUES));
        csInsertTableAlias.select(store.getDefaultInt(SQLModelPreferences.SQL_PROPOSAL_INSERT_TABLE_ALIAS));
        csMatchContains.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS));
        csUseGlobalSearch.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT));
        csShowColumnProcedures.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES));
        csHippieActivation.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.ENABLE_HIPPIE));
        csCompletionMode.select(SQLAutocompletionMode.valueByName(store.getDefaultString(SQLPreferenceConstants.AUTOCOMPLETION_MODE)).ordinal());
        super.performDefaults();
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}