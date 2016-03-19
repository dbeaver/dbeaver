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

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.format.external.SQLExternalFormatter;
import org.jkiss.dbeaver.model.sql.format.tokenized.SQLTokenizedFormatter;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * PrefPageSQLFormat
 */
public class PrefPageSQLFormat extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.format"; //$NON-NLS-1$

    private static final CharSequence SQL_PREVIEW_TEXT =
        "SELECT * FROM TABLE1 t WHERE a > 100 AND b BETWEEN 12 AND 45;\n" +
        "SELECT t.*,j1.x,j2.y FROM TABLE1 t JOIN JT1 j1 ON j1.a = t.a LEFT OUTER JOIN JT2 j2 ON j2.a=t.a AND j2.b=j1.b;\n" +
        "DELETE FROM TABLE1 WHERE a=1;\n" +
        "UPDATE TABLE1 SET a=2 WHERE a=1;\n";

    private Combo formatterSelector;

    private Combo keywordCaseCombo;
    private Button autoConvertKeywordCase;

    private Text externalCmdText;

    private IEditorSite subSite;
    private SQLEditorBase sqlViewer;

    public PrefPageSQLFormat()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(SQLPreferenceConstants.FORMAT_FORMATTER) ||
            store.contains(SQLPreferenceConstants.FORMAT_KEYWORD_CASE) ||
            store.contains(SQLPreferenceConstants.FORMAT_KEYWORD_CASE_AUTO) ||
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
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        formatterSelector = UIUtils.createLabelCombo(composite, "Formatter", SWT.DROP_DOWN | SWT.READ_ONLY);
        formatterSelector.add(SQLTokenizedFormatter.FORMATTER_ID);
        formatterSelector.add(SQLExternalFormatter.FORMATTER_ID);

        // Default formatter settings
        {
            Composite defaultGroup = UIUtils.createControlGroup(composite, "Formatter", 2, GridData.FILL_HORIZONTAL, 0);
            keywordCaseCombo = UIUtils.createLabelCombo(defaultGroup, "Keyword case", SWT.DROP_DOWN | SWT.READ_ONLY);
            keywordCaseCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            keywordCaseCombo.add("Database");
            for (DBPIdentifierCase c :DBPIdentifierCase.values()) {
                keywordCaseCombo.add(capitalizeCaseName(c.name()));
            }
            autoConvertKeywordCase = UIUtils.createLabelCheckbox(defaultGroup, "Auto-convert case", false);
        }

        // External formatter
        {
            Composite externalGroup = UIUtils.createControlGroup(composite, "External formatter", 2, GridData.FILL_HORIZONTAL, 0);

            externalCmdText = UIUtils.createLabelText(externalGroup, "External formatter command", "");
        }

        {
            // SQL preview
            Composite previewGroup = UIUtils.createControlGroup(composite, "SQL preview", 2, GridData.FILL_BOTH, 0);
            previewGroup.setLayout(new FillLayout());

            sqlViewer = new SQLEditorBase() {
                @Override
                public DBCExecutionContext getExecutionContext() {
                    return null;
                }
            };
            try {
                subSite = new SubEditorSite(DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite());
                StringEditorInput sqlInput = new StringEditorInput("SQL preview", SQL_PREVIEW_TEXT, true, GeneralUtils.getDefaultConsoleEncoding());
                sqlViewer.init(subSite, sqlInput);
            } catch (PartInitException e) {
                log.error(e);
            }

            sqlViewer.createPartControl(previewGroup);
            Object text = sqlViewer.getAdapter(Control.class);
            if (text instanceof StyledText) {
                ((StyledText) text).setWordWrap(true);
            }
            sqlViewer.reloadSyntaxRules();

            previewGroup.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    sqlViewer.dispose();
                }
            });
        }

        return composite;
    }

    private static String capitalizeCaseName(String name) {
        return CommonUtils.capitalizeWord(name.toLowerCase(Locale.ENGLISH));
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        UIUtils.setComboSelection(formatterSelector, store.getString(SQLPreferenceConstants.FORMAT_FORMATTER));
        final String caseName = store.getString(SQLPreferenceConstants.FORMAT_KEYWORD_CASE);
        if (CommonUtils.isEmpty(caseName)) {
            keywordCaseCombo.select(0);
        } else {
            UIUtils.setComboSelection(keywordCaseCombo, capitalizeCaseName(caseName));
        }
        autoConvertKeywordCase.setSelection(store.getBoolean(SQLPreferenceConstants.FORMAT_KEYWORD_CASE_AUTO));

        externalCmdText.setText(store.getString(SQLPreferenceConstants.FORMAT_EXTERNAL_CMD));

        sqlViewer.getTextViewer().doOperation(ISourceViewer.FORMAT);
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        store.setValue(SQLPreferenceConstants.FORMAT_FORMATTER, formatterSelector.getText());

        final String caseName;
        if (keywordCaseCombo.getSelectionIndex() == 0) {
            caseName = "";
        } else {
            caseName = keywordCaseCombo.getText().toUpperCase(Locale.ENGLISH);
        }
        store.setValue(SQLPreferenceConstants.FORMAT_KEYWORD_CASE, caseName);
        store.setValue(SQLPreferenceConstants.FORMAT_KEYWORD_CASE_AUTO, autoConvertKeywordCase.getSelection());

        store.setValue(SQLPreferenceConstants.FORMAT_EXTERNAL_CMD, externalCmdText.getText());
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(SQLPreferenceConstants.FORMAT_FORMATTER);

        store.setToDefault(SQLPreferenceConstants.FORMAT_KEYWORD_CASE);
        store.setToDefault(SQLPreferenceConstants.FORMAT_KEYWORD_CASE_AUTO);

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