/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * PrefPageSQLFormat
 */
public class PrefPageSQLFormat extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.format"; //$NON-NLS-1$

    private Combo keywordCaseCombo;
    private Button autoConvertKeywordCase;

    private Button useExternalCheckbox;
    private Text externalCmdText;

    public PrefPageSQLFormat()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(SQLPreferenceConstants.FORMAT_KEYWORD_CASE) ||
            store.contains(SQLPreferenceConstants.FORMAT_KEYWORD_CASE_AUTO) ||
            store.contains(SQLPreferenceConstants.FORMAT_EXTERNAL) ||
            store.contains(SQLPreferenceConstants.FORMAT_EXTERNAL_CMD)
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
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        // Default formatter settings
        {
            Composite formatGroup = UIUtils.createControlGroup(composite, "Formatter", 2, GridData.FILL_HORIZONTAL, 0);
            keywordCaseCombo = UIUtils.createLabelCombo(formatGroup, "Keyword case", SWT.DROP_DOWN | SWT.READ_ONLY);
            keywordCaseCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            keywordCaseCombo.add("Database");
            for (DBPIdentifierCase c :DBPIdentifierCase.values()) {
                keywordCaseCombo.add(capitalizeCaseName(c.name()));
            }
            autoConvertKeywordCase = UIUtils.createLabelCheckbox(formatGroup, "Auto-convert case", false);
        }

        // External formatter
        {
            Composite externalGroup = UIUtils.createControlGroup(composite, "External formatter", 2, GridData.FILL_HORIZONTAL, 0);

            useExternalCheckbox = UIUtils.createLabelCheckbox(externalGroup, "Use external formatter", false);
            externalCmdText = UIUtils.createLabelText(externalGroup, "External formatter command", "");
        }

        return composite;
    }

    private static String capitalizeCaseName(String name) {
        return CommonUtils.capitalizeWord(name.toLowerCase(Locale.ENGLISH));
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        final String caseName = store.getString(SQLPreferenceConstants.FORMAT_KEYWORD_CASE);
        if (CommonUtils.isEmpty(caseName)) {
            keywordCaseCombo.select(0);
        } else {
            UIUtils.setComboSelection(keywordCaseCombo, capitalizeCaseName(caseName));
        }
        autoConvertKeywordCase.setSelection(store.getBoolean(SQLPreferenceConstants.FORMAT_KEYWORD_CASE_AUTO));

        useExternalCheckbox.setSelection(store.getBoolean(SQLPreferenceConstants.FORMAT_EXTERNAL));
        externalCmdText.setText(store.getString(SQLPreferenceConstants.FORMAT_EXTERNAL_CMD));
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        final String caseName;
        if (keywordCaseCombo.getSelectionIndex() == 0) {
            caseName = "";
        } else {
            caseName = keywordCaseCombo.getText().toUpperCase(Locale.ENGLISH);
        }
        store.setValue(SQLPreferenceConstants.FORMAT_KEYWORD_CASE, caseName);
        store.setValue(SQLPreferenceConstants.FORMAT_KEYWORD_CASE_AUTO, autoConvertKeywordCase.getSelection());

        store.setValue(SQLPreferenceConstants.FORMAT_EXTERNAL, useExternalCheckbox.getSelection());
        store.setValue(SQLPreferenceConstants.FORMAT_EXTERNAL_CMD, externalCmdText.getText());
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(SQLPreferenceConstants.FORMAT_KEYWORD_CASE);
        store.setToDefault(SQLPreferenceConstants.FORMAT_KEYWORD_CASE_AUTO);

        store.setToDefault(SQLPreferenceConstants.FORMAT_EXTERNAL);
        store.setToDefault(SQLPreferenceConstants.FORMAT_EXTERNAL_CMD);
    }

    @Override
    public void applyData(Object data)
    {
        super.applyData(data);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}