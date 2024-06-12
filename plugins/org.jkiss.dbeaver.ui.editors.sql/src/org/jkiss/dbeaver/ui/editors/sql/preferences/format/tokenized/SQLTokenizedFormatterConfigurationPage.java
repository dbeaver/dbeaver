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
package org.jkiss.dbeaver.ui.editors.sql.preferences.format.tokenized;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.editors.sql.preferences.format.BaseFormatterConfigurationPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class SQLTokenizedFormatterConfigurationPage extends BaseFormatterConfigurationPage {

    private Spinner indentSizeSpinner;
    private Button useSpacesCheck;
    private Combo keywordCaseCombo;
    private Button lineFeedBeforeCommaCheck;
    private Button breakLineBeforeCloseBracket;
    private Button insertDelimiterInEmptyLines;

    @Override
    protected Composite createFormatSettings(Composite parent) {
        Group settings = UIUtils.createControlGroup(parent, SQLEditorMessages.pref_page_sql_format_label_settings, 4, GridData.FILL_HORIZONTAL, 0);
        SelectionListener selectListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                changeListener.run();
            }
        };

        keywordCaseCombo = UIUtils.createLabelCombo(settings, SQLEditorMessages.pref_page_sql_format_label_keyword_case, SWT.DROP_DOWN | SWT.READ_ONLY);
        keywordCaseCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        keywordCaseCombo.add("Default");
        for (DBPIdentifierCase c : DBPIdentifierCase.values()) {
            keywordCaseCombo.add(DBPIdentifierCase.capitalizeCaseName(c.name()));
        }
        keywordCaseCombo.addSelectionListener(selectListener);

        this.indentSizeSpinner = UIUtils.createLabelSpinner(settings,
                SQLEditorMessages.pref_page_sql_format_label_indent_size,
                SQLEditorMessages.pref_page_sql_format_label_indent_size, 4, 0, 100);
        indentSizeSpinner.addSelectionListener(selectListener);

        this.useSpacesCheck = UIUtils.createCheckbox(settings,
                SQLEditorMessages.pref_page_sql_format_label_insert_spaces_for_tabs,
                SQLEditorMessages.pref_page_sql_format_label_insert_spaces_for_tabs, true, 2);
        useSpacesCheck.addSelectionListener(selectListener);
        this.lineFeedBeforeCommaCheck = UIUtils.createCheckbox(settings,
                SQLEditorMessages.pref_page_sql_format_label_insert_line_feed_before_commas,
                SQLEditorMessages.pref_page_sql_format_label_insert_line_feed_before_commas, true, 2);
        lineFeedBeforeCommaCheck.addSelectionListener(selectListener);
        this.breakLineBeforeCloseBracket = UIUtils.createCheckbox(settings,
                SQLEditorMessages.pref_page_sql_format_label_add_line_feed_before_close_bracket,
                SQLEditorMessages.pref_page_sql_format_label_add_line_feed_before_close_bracket,
                true, 2);
        breakLineBeforeCloseBracket.addSelectionListener(selectListener);
        this.insertDelimiterInEmptyLines = UIUtils.createCheckbox(settings,
            SQLEditorMessages.pref_page_sql_format_label_insert_delimiters_in_empty_lines,
            SQLEditorMessages.pref_page_sql_format_tip_insert_delimiters_in_empty_lines,
            true, 2);
        insertDelimiterInEmptyLines.addSelectionListener(selectListener);

        return parent;
    }

    private IPreferenceStore getTextEditorsPreferenceStore() {
        return EditorsPlugin.getDefault().getPreferenceStore();
    }

    @Override
    public void loadSettings(DBPPreferenceStore preferenceStore, boolean useDefaults) {
        super.loadSettings(preferenceStore, useDefaults);

        final String caseName = useDefaults
            ? preferenceStore.getDefaultString(ModelPreferences.SQL_FORMAT_KEYWORD_CASE)
            : preferenceStore.getString(ModelPreferences.SQL_FORMAT_KEYWORD_CASE);

        DBPIdentifierCase keywordCase = CommonUtils.isEmpty(caseName) ? null : CommonUtils.valueOf(DBPIdentifierCase.class, caseName);
        if (keywordCase == null) {
            keywordCaseCombo.select(0);
        } else {
            UIUtils.setComboSelection(
                keywordCaseCombo,
                DBPIdentifierCase.capitalizeCaseName(keywordCase.name()));
        }
        lineFeedBeforeCommaCheck.setSelection(
            useDefaults
                ? preferenceStore.getDefaultBoolean(ModelPreferences.SQL_FORMAT_LF_BEFORE_COMMA)
                : preferenceStore.getBoolean(ModelPreferences.SQL_FORMAT_LF_BEFORE_COMMA)
        );
        breakLineBeforeCloseBracket.setSelection(
            useDefaults
                ? preferenceStore.getDefaultBoolean(ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET)
                : preferenceStore.getBoolean(ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET)
        );
        insertDelimiterInEmptyLines.setSelection(
            useDefaults
                ? preferenceStore.getDefaultBoolean(ModelPreferences.SQL_FORMAT_INSERT_DELIMITERS_IN_EMPTY_LINES)
                : preferenceStore.getBoolean(ModelPreferences.SQL_FORMAT_INSERT_DELIMITERS_IN_EMPTY_LINES)
        );


        {
            // Text editor settings
            IPreferenceStore textEditorPrefs = getTextEditorsPreferenceStore();
            this.indentSizeSpinner.setSelection(
                useDefaults
                    ? textEditorPrefs.getDefaultInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH)
                    : textEditorPrefs.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH)
            );
            useSpacesCheck.setSelection(
                useDefaults
                    ? textEditorPrefs.getDefaultBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS)
                    : textEditorPrefs.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS)
            );
        }
    }

    @Override
    public void saveSettings(DBPPreferenceStore preferenceStore) {
        super.saveSettings(preferenceStore);
        final String caseName;
        if (keywordCaseCombo.getSelectionIndex() == 0) {
            caseName = "";
        } else {
            caseName = keywordCaseCombo.getText().toUpperCase(Locale.ENGLISH);
        }
        preferenceStore.setValue(ModelPreferences.SQL_FORMAT_KEYWORD_CASE, caseName);
        preferenceStore.setValue(ModelPreferences.SQL_FORMAT_LF_BEFORE_COMMA, lineFeedBeforeCommaCheck.getSelection());
        preferenceStore.setValue(ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET, breakLineBeforeCloseBracket.getSelection());
        preferenceStore.setValue(ModelPreferences.SQL_FORMAT_INSERT_DELIMITERS_IN_EMPTY_LINES, insertDelimiterInEmptyLines.getSelection());

        {
            // Text editor settings
            IPreferenceStore textEditorPrefs = getTextEditorsPreferenceStore();
            textEditorPrefs.setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, this.indentSizeSpinner.getSelection());
            textEditorPrefs.setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, useSpacesCheck.getSelection());
        }

    }

    @Override
    public void resetSettings(DBPPreferenceStore preferenceStore) {
        super.resetSettings(preferenceStore);
        preferenceStore.setToDefault(ModelPreferences.SQL_FORMAT_KEYWORD_CASE);
        preferenceStore.setToDefault(ModelPreferences.SQL_FORMAT_LF_BEFORE_COMMA);
        preferenceStore.setToDefault(ModelPreferences.SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET);
        preferenceStore.setToDefault(ModelPreferences.SQL_FORMAT_INSERT_DELIMITERS_IN_EMPTY_LINES);
    }

}
