/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLFormatterConfigurationRegistry;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLFormatterConfigurator;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLFormatterDescriptor;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * PrefPageSQLFormat
 */
public class PrefPageSQLFormat extends TargetPrefPage
{
    private static final Log log = Log.getLog(PrefPageSQLFormat.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.format"; //$NON-NLS-1$

    private final static String FORMAT_FILE_NAME = "format_preview.sql";

    private Button styleBoldKeywords;

    // Formatter
    private Combo formatterSelector;

    private SQLEditorBase sqlViewer;
    private Composite formatterConfigPlaceholder;
    private List<SQLFormatterDescriptor> formatters;

    private SQLFormatterConfigurator curConfigurator;

    public PrefPageSQLFormat()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(SQLPreferenceConstants.SQL_FORMAT_BOLD_KEYWORDS) ||

            store.contains(ModelPreferences.SQL_FORMAT_FORMATTER);
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

        formatterSelector = UIUtils.createLabelCombo(composite, SQLEditorMessages.pref_page_sql_format_label_formatter, SWT.DROP_DOWN | SWT.READ_ONLY);
        formatterSelector.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        formatters = SQLFormatterConfigurationRegistry.getInstance().getFormatters();
        for (SQLFormatterDescriptor formatterDesc : formatters) {
            formatterSelector.add(DBPIdentifierCase.capitalizeCaseName(formatterDesc.getLabel()));
        }
        formatterSelector.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                showFormatterSettings();
                performApply();
            }
        });
        formatterSelector.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        Composite formatterGroup = UIUtils.createPlaceholder(composite, 1, 5);
        formatterGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData)formatterGroup.getLayoutData()).horizontalSpan = 2;

/*
        {
            Composite formatterPanel = UIUtils.createPlaceholder(formatterGroup, 4, 5);
            formatterPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            keywordCaseCombo = UIUtils.createLabelCombo(formatterPanel, CoreMessages.pref_page_sql_format_label_keyword_case, SWT.DROP_DOWN | SWT.READ_ONLY);
            keywordCaseCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            keywordCaseCombo.add("Database");
            for (DBPIdentifierCase c :DBPIdentifierCase.values()) {
                keywordCaseCombo.add(DBPIdentifierCase.capitalizeCaseName(c.name()));
            }
            keywordCaseCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    performApply();
                }
            });
        }
*/

        // External formatter
        {
            formatterConfigPlaceholder = UIUtils.createPlaceholder(formatterGroup, 2, 5);
            formatterConfigPlaceholder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
            formatterConfigPlaceholder.setLayout(new FillLayout());
        }

        {
            // SQL preview
            Composite previewGroup = new Composite(composite, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            previewGroup.setLayoutData(gd);
            previewGroup.setLayout(new FillLayout());

            sqlViewer = new SQLEditorBase() {
                @Override
                public DBCExecutionContext getExecutionContext() {
                    final DBPDataSourceContainer container = getDataSourceContainer();
                    if (container != null) {
                        final DBPDataSource dataSource = container.getDataSource();
                        if (dataSource != null) {
                            return dataSource.getDefaultInstance().getDefaultContext(false);
                        }
                    }
                    return null;
                }
            };
            try {
                try (final InputStream sqlStream = getClass().getResourceAsStream(FORMAT_FILE_NAME)) {
                    final String sqlText = ContentUtils.readToString(sqlStream, GeneralUtils.DEFAULT_ENCODING);
                    IEditorSite subSite = new SubEditorSite(UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite());
                    StringEditorInput sqlInput = new StringEditorInput("SQL preview", sqlText, true, GeneralUtils.getDefaultFileEncoding());
                    sqlViewer.init(subSite, sqlInput);
                }
            } catch (Exception e) {
                log.error(e);
            }

            sqlViewer.createPartControl(previewGroup);
            Object text = sqlViewer.getAdapter(Control.class);
            if (text instanceof StyledText) {
                ((StyledText) text).setWordWrap(true);
            }
            sqlViewer.reloadSyntaxRules();

            previewGroup.addDisposeListener(e -> sqlViewer.dispose());

            {
                // Styles
//            Composite afGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_sql_format_group_style, 1, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);
//            ((GridData)afGroup.getLayoutData()).horizontalSpan = 2;
                styleBoldKeywords = UIUtils.createCheckbox(
                    composite,
                    SQLEditorMessages.pref_page_sql_format_label_bold_keywords,
                    SQLEditorMessages.pref_page_sql_format_label_bold_keywords_tip,
                    false, 2);
                styleBoldKeywords.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        performApply();
                    }
                });

            }
        }


        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        styleBoldKeywords.setSelection(store.getBoolean(SQLPreferenceConstants.SQL_FORMAT_BOLD_KEYWORDS));

        String formatterId = store.getString(ModelPreferences.SQL_FORMAT_FORMATTER);
        for (int i = 0; i < formatters.size(); i++) {
            if (formatters.get(i).getId().equalsIgnoreCase(formatterId)) {
                formatterSelector.select(i);
                break;
            }
        }
        if (formatterSelector.getSelectionIndex() < 0) formatterSelector.select(0);

        formatSQL();
        showFormatterSettings();
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        if (curConfigurator != null) {
            curConfigurator.saveSettings(getTargetPreferenceStore());
        }
        store.setValue(SQLPreferenceConstants.SQL_FORMAT_BOLD_KEYWORDS, styleBoldKeywords.getSelection());

        store.setValue(ModelPreferences.SQL_FORMAT_FORMATTER,
            formatters.get(formatterSelector.getSelectionIndex()).getId().toUpperCase(Locale.ENGLISH));

        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(SQLPreferenceConstants.SQL_FORMAT_BOLD_KEYWORDS);

        store.setToDefault(ModelPreferences.SQL_FORMAT_FORMATTER);
        if (curConfigurator != null) {
            curConfigurator.resetSettings(store);
        }
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
        if (curConfigurator != null) {
            curConfigurator.saveSettings(getTargetPreferenceStore());
        }
        for (Control child : formatterConfigPlaceholder.getChildren()) {
            child.dispose();
        }
        SQLFormatterDescriptor selFormatter = formatters.get(formatterSelector.getSelectionIndex());

        try {
            curConfigurator = selFormatter.createConfigurer();
            if (curConfigurator != null) {
                curConfigurator.configure(selFormatter);

                curConfigurator.createControl(formatterConfigPlaceholder);
                curConfigurator.loadSettings(getTargetPreferenceStore());
            }
        } catch (DBException e) {
            log.error("Error creating formatter configurator", e);
            setMessage(CommonUtils.toString(e.getMessage()), SWT.ICON_ERROR);
            return;
        }

        ((Composite)getControl()).layout(true, true);
        if (isDataSourcePreferencePage()) {
            enablePreferenceContent(useDataSourceSettings());
        }
    }

    private void formatSQL() {
        try {
            try (final InputStream sqlStream = getClass().getResourceAsStream(FORMAT_FILE_NAME)) {
                final String sqlText = ContentUtils.readToString(sqlStream, GeneralUtils.DEFAULT_ENCODING);
                sqlViewer.setInput(new StringEditorInput("SQL preview", sqlText, true, GeneralUtils.getDefaultFileEncoding()));
            }
        } catch (Exception e) {
            log.error(e);
        }
        sqlViewer.getTextViewer().doOperation(ISourceViewer.FORMAT);
        sqlViewer.reloadSyntaxRules();
    }

}