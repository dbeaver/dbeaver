/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.format.tokenized;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.format.BaseFormatterConfigurationPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class SQLTokenizedFormatterConfigurationPage extends BaseFormatterConfigurationPage {

    private Spinner indentSizeSpinner;
    private Button useSpacesCheck;
    private Combo keywordCaseCombo;
    private Button lineFeedBeforeCommaCheck;

    @Override
    protected Composite createFormatSettings(Composite parent) {
        Group settings = UIUtils.createControlGroup(parent, "Settings", 5, GridData.FILL_HORIZONTAL, 0);

        keywordCaseCombo = UIUtils.createLabelCombo(settings, CoreMessages.pref_page_sql_format_label_keyword_case, SWT.DROP_DOWN | SWT.READ_ONLY);
        keywordCaseCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        keywordCaseCombo.add("Default");
        for (DBPIdentifierCase c : DBPIdentifierCase.values()) {
            keywordCaseCombo.add(DBPIdentifierCase.capitalizeCaseName(c.name()));
        }

        this.indentSizeSpinner = UIUtils.createLabelSpinner(settings, "Indent size", "Insert spaces for tabs", 4, 0, 100);
        this.useSpacesCheck = UIUtils.createCheckbox(settings, "Insert spaces for tabs", "Insert spaces for tabs", true, 1);
        this.lineFeedBeforeCommaCheck = UIUtils.createCheckbox(settings, "Insert line feed before commas", "Insert line feeds before/after commas", true, 5);

        return parent;
    }

    private IPreferenceStore getTextEditorsPreferenceStore() {
        return EditorsPlugin.getDefault().getPreferenceStore();
    }

    @Override
    public void loadSettings(DBPPreferenceStore preferenceStore) {
        super.loadSettings(preferenceStore);

        final String caseName = preferenceStore.getString(ModelPreferences.SQL_FORMAT_KEYWORD_CASE);

        DBPIdentifierCase keywordCase = CommonUtils.isEmpty(caseName) ? null : DBPIdentifierCase.valueOf(caseName);
        if (keywordCase == null) {
            keywordCaseCombo.select(0);
        } else {
            UIUtils.setComboSelection(
                keywordCaseCombo,
                DBPIdentifierCase.capitalizeCaseName(keywordCase.name()));
        }
        lineFeedBeforeCommaCheck.setSelection(preferenceStore.getBoolean(ModelPreferences.SQL_FORMAT_LF_BEFORE_COMMA));

        {
            // Text editor settings
            IPreferenceStore textEditorPrefs = getTextEditorsPreferenceStore();
            this.indentSizeSpinner.setSelection(textEditorPrefs.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH));
            useSpacesCheck.setSelection(textEditorPrefs.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS));
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
    }
}
