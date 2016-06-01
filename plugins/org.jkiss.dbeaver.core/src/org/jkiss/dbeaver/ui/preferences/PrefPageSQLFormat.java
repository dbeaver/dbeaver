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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorSite;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
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
import org.jkiss.dbeaver.utils.ContentUtils;
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

    private final static String FORMAT_FILE_NAME = "format_preview.sql";

    private Combo formatterSelector;

    private Combo keywordCaseCombo;

    private Text externalCmdText;
    private Button externalUseFile;
    private Spinner externalTimeout;

    private SQLEditorBase sqlViewer;
    private Composite defaultGroup;
    private Composite externalGroup;

    public PrefPageSQLFormat()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(ModelPreferences.SQL_FORMAT_FORMATTER) ||
            store.contains(ModelPreferences.SQL_FORMAT_KEYWORD_CASE) ||
            store.contains(ModelPreferences.SQL_FORMAT_EXTERNAL_CMD) ||
            store.contains(ModelPreferences.SQL_FORMAT_EXTERNAL_FILE) ||
            store.contains(ModelPreferences.SQL_FORMAT_EXTERNAL_TIMEOUT)
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
        formatterSelector.add(capitalizeCaseName(SQLTokenizedFormatter.FORMATTER_ID));
        formatterSelector.add(capitalizeCaseName(SQLExternalFormatter.FORMATTER_ID));
        formatterSelector.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                showFormatterSettings();
            }
        });
        formatterSelector.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        // Default formatter settings
        {
            defaultGroup = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_HORIZONTAL, 0);
            keywordCaseCombo = UIUtils.createLabelCombo(defaultGroup, "Keyword case", SWT.DROP_DOWN | SWT.READ_ONLY);
            keywordCaseCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            keywordCaseCombo.add("Database");
            for (DBPIdentifierCase c :DBPIdentifierCase.values()) {
                keywordCaseCombo.add(capitalizeCaseName(c.name()));
            }
        }

        // External formatter
        {
            externalGroup = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_HORIZONTAL, 0);

            externalCmdText = UIUtils.createLabelText(externalGroup, "Command line", "");
            externalUseFile = UIUtils.createLabelCheckbox(externalGroup,
                "Use temp file",
                "Use temporary file to pass SQL text.\nTo pass file name in command line use parameter ${file}", false);
            externalTimeout = UIUtils.createLabelSpinner(externalGroup,
                "Exec timeout",
                "Time to wait until formatter process finish (ms)",
                100, 100, 10000);
        }

        {
            // SQL preview
            Composite previewGroup = UIUtils.createControlGroup(composite, "SQL preview", 2, GridData.FILL_BOTH, 0);
            previewGroup.setLayout(new FillLayout());

            sqlViewer = new SQLEditorBase() {
                @Override
                public DBCExecutionContext getExecutionContext() {
                    final DBPDataSourceContainer container = getDataSourceContainer();
                    if (container != null) {
                        final DBPDataSource dataSource = container.getDataSource();
                        if (dataSource != null) {
                            return dataSource.getDefaultContext(false);
                        }
                    }
                    return null;
                }
            };
            try {
                final String sqlText = ContentUtils.readToString(getClass().getResourceAsStream(FORMAT_FILE_NAME), ContentUtils.DEFAULT_CHARSET);
                IEditorSite subSite = new SubEditorSite(DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite());
                StringEditorInput sqlInput = new StringEditorInput("SQL preview", sqlText, true, GeneralUtils.getDefaultFileEncoding());
                sqlViewer.init(subSite, sqlInput);
            } catch (Exception e) {
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

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        UIUtils.setComboSelection(formatterSelector, capitalizeCaseName(store.getString(ModelPreferences.SQL_FORMAT_FORMATTER)));
        final String caseName = store.getString(ModelPreferences.SQL_FORMAT_KEYWORD_CASE);
        if (CommonUtils.isEmpty(caseName)) {
            keywordCaseCombo.select(0);
        } else {
            UIUtils.setComboSelection(keywordCaseCombo, capitalizeCaseName(caseName));
        }

        externalCmdText.setText(store.getString(ModelPreferences.SQL_FORMAT_EXTERNAL_CMD));
        externalUseFile.setSelection(store.getBoolean(ModelPreferences.SQL_FORMAT_EXTERNAL_FILE));
        externalTimeout.setSelection(store.getInt(ModelPreferences.SQL_FORMAT_EXTERNAL_TIMEOUT));

        formatSQL();
        showFormatterSettings();
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        store.setValue(ModelPreferences.SQL_FORMAT_FORMATTER, formatterSelector.getText().toUpperCase(Locale.ENGLISH));

        final String caseName;
        if (keywordCaseCombo.getSelectionIndex() == 0) {
            caseName = "";
        } else {
            caseName = keywordCaseCombo.getText().toUpperCase(Locale.ENGLISH);
        }
        store.setValue(ModelPreferences.SQL_FORMAT_KEYWORD_CASE, caseName);

        store.setValue(ModelPreferences.SQL_FORMAT_EXTERNAL_CMD, externalCmdText.getText());
        store.setValue(ModelPreferences.SQL_FORMAT_EXTERNAL_FILE, externalUseFile.getSelection());
        store.setValue(ModelPreferences.SQL_FORMAT_EXTERNAL_TIMEOUT, externalTimeout.getSelection());

        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(ModelPreferences.SQL_FORMAT_FORMATTER);
        store.setToDefault(ModelPreferences.SQL_FORMAT_KEYWORD_CASE);
        store.setToDefault(ModelPreferences.SQL_FORMAT_EXTERNAL_CMD);
    }

    @Override
    protected void performApply() {
        super.performApply();
        formatSQL();
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

    private void showFormatterSettings() {
        final boolean isDefFormatter = formatterSelector.getSelectionIndex() == 0;
        defaultGroup.setVisible(isDefFormatter);
        externalGroup.setVisible(!isDefFormatter);
        ((GridData)defaultGroup.getLayoutData()).exclude = !isDefFormatter;
        ((GridData)externalGroup.getLayoutData()).exclude = isDefFormatter;
        defaultGroup.getParent().layout();
    }

    private static String capitalizeCaseName(String name) {
        return CommonUtils.capitalizeWord(name.toLowerCase(Locale.ENGLISH));
    }

    private void formatSQL() {
        try {
            final String sqlText = ContentUtils.readToString(getClass().getResourceAsStream(FORMAT_FILE_NAME), ContentUtils.DEFAULT_CHARSET);
            sqlViewer.setInput(new StringEditorInput("SQL preview", sqlText, true, GeneralUtils.getDefaultFileEncoding()));
        } catch (Exception e) {
            log.error(e);
        }
        sqlViewer.getTextViewer().doOperation(ISourceViewer.FORMAT);
        sqlViewer.reloadSyntaxRules();
    }

}