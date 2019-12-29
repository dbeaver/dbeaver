/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.util.Comparator;
import java.util.List;

/**
 * PrefPageSQLDialects
 */
public class PrefPageSQLDialects extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.dialects"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PrefPageSQLDialects.class);
    private IAdaptable element;

    private SQLDialect curDialect;
    private Text reservedWordsText;
    private Text dataTypesText;
    private Text functionNamesText;
    private Text transactionKeywordsText;
    private Text ddlKeywordsText;
    private Text blockStatementsText;
    private Text statementDelimiterText;
    private Text dualTableNameText;
    private Text testQueryText;

    public PrefPageSQLDialects() {
        super();
    }

    @Override
    protected Control createContents(Composite parent) {
        boolean isPrefPage = element == null;

        Composite composite = UIUtils.createComposite(parent, isPrefPage ? 2 : 1);

        if (isPrefPage) {
            // Create dialect selector
            Composite dialectsGroup = UIUtils.createComposite(composite, 1);
            dialectsGroup.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
            UIUtils.createControlLabel(dialectsGroup, "Dialects", 2);

            Tree dialectTable = new Tree(dialectsGroup, SWT.BORDER | SWT.SINGLE);
            dialectTable.setLayoutData(new GridData(GridData.FILL_BOTH));

            List<SQLDialectDescriptor> dialects = SQLDialectRegistry.getInstance().getDialects();
            dialects.sort(Comparator.comparing(SQLDialectDescriptor::getLabel));
            for (SQLDialectDescriptor dialect : dialects) {
                TreeItem di = new TreeItem(dialectTable, SWT.NONE);
                di.setText(dialect.getLabel());
                di.setImage(DBeaverIcons.getImage(dialect.getIcon()));
            }
        }

        {
            Composite settingsGroup = UIUtils.createComposite(composite, 2);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(settingsGroup) * 30;
            settingsGroup.setLayoutData(gd);
            UIUtils.createControlLabel(settingsGroup, SQLEditorMessages.pref_page_sql_format_label_settings, 2);

            UIUtils.createControlLabel(settingsGroup, "Keywords", 2);
            reservedWordsText = UIUtils.createLabelTextAdvanced(settingsGroup, "Reserved words", "", SWT.BORDER);
            dataTypesText = UIUtils.createLabelTextAdvanced(settingsGroup, "Data Types", "", SWT.BORDER);
            functionNamesText = UIUtils.createLabelTextAdvanced(settingsGroup, "Function names", "", SWT.BORDER);
            ddlKeywordsText = UIUtils.createLabelTextAdvanced(settingsGroup, "DDL keywords", "", SWT.BORDER);
            transactionKeywordsText = UIUtils.createLabelTextAdvanced(settingsGroup, "Transaction keywords", "", SWT.BORDER);
            blockStatementsText = UIUtils.createLabelTextAdvanced(settingsGroup, "Block statements", "", SWT.BORDER);

            UIUtils.createControlLabel(settingsGroup, "Miscellaneous", 2);
            statementDelimiterText = UIUtils.createLabelText(settingsGroup, "Statement delimiter", "", SWT.BORDER);
            dualTableNameText = UIUtils.createLabelText(settingsGroup, "Dual table name", "", SWT.BORDER);
            testQueryText = UIUtils.createLabelText(settingsGroup, "Test query", "", SWT.BORDER);

        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults() {
        if (element != null) {
            DBPDataSource dataSource = element.getAdapter(DBPDataSource.class);
            curDialect = SQLUtils.getDialectFromDataSource(dataSource);
        }
        loadDialectSettings();

        super.performDefaults();
    }

    private void loadDialectSettings() {
        if (curDialect == null) {
            return;
        }
        reservedWordsText.setText(String.join(",", curDialect.getReservedWords()));
        dataTypesText.setText(String.join(",", curDialect.getDataTypes(null)));
        functionNamesText.setText(String.join(",", curDialect.getFunctions(null)));
        ddlKeywordsText.setText(String.join(",", curDialect.getDDLKeywords()));
        if (curDialect instanceof BasicSQLDialect) {
            transactionKeywordsText.setText(String.join(",", ((BasicSQLDialect)curDialect).getTransactionKeywords()));
        }
        //blockStatementsText.setText(String.join(",", curDialect.getBlockBoundStrings()));

        statementDelimiterText.setText(curDialect.getScriptDelimiter());
    }

    @Override
    public boolean performOk() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        //store.setValue(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_READ, bindEmbeddedReadCheck.getSelection());

        PrefUtils.savePreferenceStore(store);

        return super.performOk();
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    public IAdaptable getElement() {
        return element;
    }

    @Override
    public void setElement(IAdaptable element) {
        this.element = element;
    }

}