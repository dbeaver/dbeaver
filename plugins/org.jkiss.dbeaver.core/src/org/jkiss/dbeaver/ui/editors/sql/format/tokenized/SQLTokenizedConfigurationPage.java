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
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.format.BaseFormatterConfigurationPage;

public class SQLTokenizedConfigurationPage extends BaseFormatterConfigurationPage {

    private Spinner indentSizeSpinner;
    private Button useSpacesCheck;
    private Button compactFormatCheck;
    private Combo keywordCaseCombo;

    @Override
    protected Composite createFormatSettings(Composite parent, SQLFormatterConfiguration configuration) {
        Group settings = UIUtils.createControlGroup(parent, "Settings", 2, GridData.FILL_HORIZONTAL, 0);

        keywordCaseCombo = UIUtils.createLabelCombo(settings, CoreMessages.pref_page_sql_format_label_keyword_case, SWT.DROP_DOWN | SWT.READ_ONLY);
        keywordCaseCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        keywordCaseCombo.add("Default");
        for (DBPIdentifierCase c : DBPIdentifierCase.values()) {
            keywordCaseCombo.add(DBPIdentifierCase.capitalizeCaseName(c.name()));
        }
        DBPIdentifierCase keywordCase = configuration.getKeywordCase();
        if (keywordCase == null) {
            keywordCaseCombo.select(0);
        } else {
            UIUtils.setComboSelection(
                keywordCaseCombo,
                DBPIdentifierCase.capitalizeCaseName(keywordCase.name()));
        }

        IPreferenceStore preferenceStore = getPreferenceStore();
        boolean useSpaces = preferenceStore.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS);
        int tabWidth = preferenceStore .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);

        this.indentSizeSpinner = UIUtils.createLabelSpinner(settings, "Indent size", "Insert spaces for tabs", tabWidth, 0, 100);
        this.useSpacesCheck = UIUtils.createCheckbox(settings, "Insert spaces for tabs", "Insert spaces for tabs", useSpaces, 2);
        this.compactFormatCheck = UIUtils.createCheckbox(settings, "Compact formatting", "Compact formatting. Less line feeds and indentation", false, 2);

        return parent;
    }

    @Override
    protected void saveFormatSettings(SQLFormatterConfiguration configuration) {
        // Save formatter settings
    }

    private IPreferenceStore getPreferenceStore() {
        return EditorsPlugin.getDefault().getPreferenceStore();
    }

}
