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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
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

    private SQLDialectDescriptor curDialect;
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

            List<SQLDialectDescriptor> dialects = SQLDialectRegistry.getInstance().getRootDialects();
            //dialects.sort(Comparator.comparing(SQLDialectDescriptor::getLabel));
            for (SQLDialectDescriptor dialect : dialects) {
                createDialectItem(dialectTable, null, dialect);
            }
            dialectTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    TreeItem[] selection = dialectTable.getSelection();
                    if (selection.length == 1) {
                        curDialect = (SQLDialectDescriptor) selection[0].getData();
                        loadDialectSettings();
                    }
                }
            });
        }

        {
            Composite settingsGroup = UIUtils.createComposite(composite, 2);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(settingsGroup) * 50;
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

    private void createDialectItem(Tree dialectTable, TreeItem parentItem, SQLDialectDescriptor dialect) {
        TreeItem di = null;
        if (!dialect.isHidden()) {
            di = parentItem == null ? new TreeItem(dialectTable, SWT.NONE) : new TreeItem(parentItem, SWT.NONE);
            di.setText(dialect.getLabel());
            di.setImage(DBeaverIcons.getImage(dialect.getIcon()));
            di.setData(dialect);
        } else {
            di = parentItem;
        }

        List<SQLDialectMetadata> subDialects = dialect.getSubDialects(true);
        subDialects.sort(Comparator.comparing(SQLDialectMetadata::getLabel));
        for (SQLDialectMetadata dm : subDialects) {
            createDialectItem(dialectTable, di, (SQLDialectDescriptor) dm);
        }
        if (di != null) {
            di.setExpanded(true);
        }
    }

    @Override
    protected void performDefaults() {
        if (element != null) {
            DBPDataSourceContainer dataSource = element.getAdapter(DBPDataSourceContainer.class);
            if (dataSource != null) {
                SQLDialect scriptDialect = dataSource.getScriptDialect();
                if (scriptDialect != null) {
                    curDialect = SQLDialectRegistry.getInstance().getDialect(scriptDialect.getDialectName());
                }
            }
        }
        loadDialectSettings();

        super.performDefaults();
    }

    private void loadDialectSettings() {
        if (curDialect == null) {
            return;
        }
        reservedWordsText.setText(String.join(",", curDialect.getReservedWords()));
        dataTypesText.setText(String.join(",", curDialect.getDataTypes()));
        functionNamesText.setText(String.join(",", curDialect.getFunctions()));
        ddlKeywordsText.setText(String.join(",", curDialect.getDDLKeywords()));
        transactionKeywordsText.setText(String.join(",", curDialect.getTransactionKeywords()));
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