/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.dialects.SQLDialectDescriptorTransformer;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.dbeaver.model.sql.dialects.SQLDialectDescriptor;
import org.jkiss.dbeaver.model.sql.dialects.SQLDialectRegistry;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PrefPageSQLDialects
 */
public class PrefPageSQLDialects extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.dialects"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PrefPageSQLDialects.class);
    private IAdaptable element;

    private SQLDialectMetadata curDialect;
    private Text reservedWordsText;
    private Text dataTypesText;
    private Text functionNamesText;
    private Text transactionKeywordsText;
    private Text ddlKeywordsText;
    private Text dmlKeywordsText;
    private Text executeKeywordsText;
//    private Text blockStatementsText;
//    private Text dualTableNameText;
//    private Text testQueryText;
    private Text dialectText;
    @Nullable
    private Tree dialectTable;
    private Font boldFont;
    private Font normalFont;

    public PrefPageSQLDialects() {
        super();
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        boolean isPrefPage = element == null;

        Composite composite = UIUtils.createComposite(parent, isPrefPage ? 2 : 1);
        if (isPrefPage) {
            // Create dialect selector
            Composite dialectsGroup = UIUtils.createComposite(composite, 1);
            dialectsGroup.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
            UIUtils.createControlLabel(dialectsGroup, "Dialects", 2);

            dialectTable = new Tree(dialectsGroup, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 200;
            dialectTable.setLayoutData(gd);
            boldFont = UIUtils.makeBoldFont(dialectTable.getFont());
            normalFont = dialectTable.getFont();
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

            if (!isPrefPage) {
                Composite nameComp = UIUtils.createComposite(settingsGroup, 2);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 2;
                nameComp.setLayoutData(gd);
                dialectText = UIUtils.createLabelText(nameComp, "Dialect", "", SWT.READ_ONLY);
            }
            //UIUtils.createControlLabel(settingsGroup, SQLEditorMessages.pref_page_sql_format_label_settings, 2);

            Group kwGroup = UIUtils.createControlGroup(settingsGroup, "Keywords", 2, GridData.FILL_HORIZONTAL, 0);
            ((GridData) kwGroup.getLayoutData()).horizontalSpan = 2;
            reservedWordsText = UIUtils.createLabelTextAdvanced(kwGroup, "Reserved words", "", SWT.BORDER);
            dataTypesText = UIUtils.createLabelTextAdvanced(kwGroup, "Data Types", "", SWT.BORDER);
            functionNamesText = UIUtils.createLabelTextAdvanced(kwGroup, "Function names", "", SWT.BORDER);
            ddlKeywordsText = UIUtils.createLabelTextAdvanced(kwGroup, "DDL keywords", "", SWT.BORDER);
            dmlKeywordsText = UIUtils.createLabelTextAdvanced(kwGroup, "Data modify keywords", "", SWT.BORDER);
            executeKeywordsText = UIUtils.createLabelTextAdvanced(kwGroup, "Execute keywords", "", SWT.BORDER);
            transactionKeywordsText = UIUtils.createLabelTextAdvanced(kwGroup, "Transaction keywords", "", SWT.BORDER);
//            blockStatementsText = UIUtils.createLabelTextAdvanced(kwGroup, "Block statements", "", SWT.BORDER);

//            Group miscGroup = UIUtils.createControlGroup(settingsGroup, "Miscellaneous", 2, GridData.FILL_HORIZONTAL, 0);
//            statementDelimiterText = UIUtils.createLabelText(miscGroup, "Statement delimiter", "", SWT.BORDER);
//            dualTableNameText = UIUtils.createLabelText(miscGroup, "Dual table name", "", SWT.BORDER);
//            testQueryText = UIUtils.createLabelText(miscGroup, "Test query", "", SWT.BORDER);

        }
        setFieldsToEmpty();
        return composite;
    }

    private void createDialectItem(Tree dialectTable, TreeItem parentItem, SQLDialectDescriptor dialect) {
        TreeItem di;
        di = parentItem == null ? new TreeItem(dialectTable, SWT.NONE) : new TreeItem(parentItem, SWT.NONE);
        if (SQLDialectRegistry.getInstance().getDialect(dialect.getId()).getTransformer() != null) {
            di.setFont(boldFont);
        }
        di.setText(dialect.getLabel());
        di.setImage(DBeaverIcons.getImage(dialect.getIcon()));
        di.setData(dialect);
        // Dialect already has an existing customization
        Set<SQLDialectMetadata> subDialects = dialect.getSubDialects(true);
        ArrayList<SQLDialectMetadata> dialects = new ArrayList<>(subDialects);
        dialects.sort(Comparator.comparing(SQLDialectMetadata::getLabel));
        for (SQLDialectMetadata dm : dialects) {
            createDialectItem(dialectTable, di, (SQLDialectDescriptor) dm);
        }
        if (di != null) {
            di.setExpanded(true);
        }
    }

    @Override
    protected void performDefaults() {
        if (curDialect != null) {
            SQLDialectRegistry.getInstance().getDialect(curDialect.getId()).setTransformer(null);
        }
        if (dialectTable != null && dialectTable.getSelection().length != 0) {
            dialectTable.getSelection()[0].setFont(normalFont);
        }
        SQLDialectRegistry.getInstance().saveDialectModifiers();
        SQLDialectRegistry.getInstance().reloadDialects();
        loadDialectSettings();
        super.performDefaults();
    }

    private void loadDialectSettings() {
        if (curDialect == null) {
            return;
        }
        SQLDialectDescriptor descriptor = SQLDialectRegistry.getInstance().getDialect(curDialect.getId());
        if (dialectText != null) {
            dialectText.setText(curDialect.getLabel());
        }
        reservedWordsText.setText(String.join(",", descriptor.getReservedWords(true)));
        dataTypesText.setText(String.join(",", descriptor.getDataTypes(true)));
        functionNamesText.setText(String.join(",", descriptor.getFunctions(true)));
        ddlKeywordsText.setText(String.join(",", descriptor.getDDLKeywords(true)));
        dmlKeywordsText.setText(String.join(",", descriptor.getDMLKeywords(true)));
        executeKeywordsText.setText(String.join(",", descriptor.getExecuteKeywords(true)));
        transactionKeywordsText.setText(String.join(",", descriptor.getTransactionKeywords(true)));
        //blockStatementsText.setText(String.join(",", curDialect.getBlockBoundStrings()));

    }

    @NotNull
    private SQLDialectDescriptorTransformer createTransformer(@NotNull SQLDialectDescriptor descriptor) {
        descriptor.setTransformer(null);
        SQLDialectDescriptorTransformer transformer = new SQLDialectDescriptorTransformer(curDialect.getId());
        transformer.putWords(
            SQLDialectDescriptor.WordType.ATTR_DIALECTS_VALUE_KEYWORDS.getTypeName(),
            generateSetDiff(descriptor.getReservedWords(true), getSetFromText(reservedWordsText))
        );
        transformer.putWords(
            SQLDialectDescriptor.WordType.ATTR_DIALECTS_VALUE_TYPES.getTypeName(),
            generateSetDiff(descriptor.getDataTypes(true), getSetFromText(dataTypesText))
        );
        transformer.putWords(
            SQLDialectDescriptor.WordType.ATTR_DIALECTS_VALUE_KEYWORDS_DDL.getTypeName(),
            generateSetDiff(descriptor.getDDLKeywords(true), getSetFromText(ddlKeywordsText))
        );
        transformer.putWords(
            SQLDialectDescriptor.WordType.ATTR_DIALECTS_VALUE_KEYWORDS_DML.getTypeName(),
            generateSetDiff(descriptor.getDMLKeywords(true), getSetFromText(dmlKeywordsText))
        );
        transformer.putWords(
            SQLDialectDescriptor.WordType.ATTR_DIALECTS_VALUE_FUNCTIONS.getTypeName(),
            generateSetDiff(descriptor.getFunctions(true), getSetFromText(functionNamesText))
        );
        transformer.putWords(
            SQLDialectDescriptor.WordType.ATTR_DIALECTS_VALUE_KEYWORDS_EXEC.getTypeName(),
            generateSetDiff(descriptor.getExecuteKeywords(true), getSetFromText(executeKeywordsText))
        );
        transformer.putWords(
            SQLDialectDescriptor.WordType.ATTR_DIALECTS_TXN_KEYWORDS.getTypeName(),
            generateSetDiff(descriptor.getTransactionKeywords(true), getSetFromText(transactionKeywordsText))
        );
        return transformer;
    }

    private Set<String> getSetFromText(@NotNull Text text) {
        return Set.of(text.getText().trim().split(","));
    }

    private Map<SQLDialectDescriptorTransformer.FilterType, Set<String>> generateSetDiff(Set<String> original,
        Set<String> newSet) {
        Set<String> exclude = original.stream().filter(it -> !newSet.contains(it)).collect(Collectors.toSet());
        Set<String> include = newSet.stream().filter(it -> !original.contains(it)).collect(Collectors.toSet());
        Map<SQLDialectDescriptorTransformer.FilterType, Set<String>> diff = new HashMap<>();
        diff.put(SQLDialectDescriptorTransformer.FilterType.EXCLUDES, exclude);
        diff.put(SQLDialectDescriptorTransformer.FilterType.INCLUDES, include);
        return diff;
    }

    private void setFieldsToEmpty() {
        reservedWordsText.setText(String.join(""));
        dataTypesText.setText(String.join(""));
        functionNamesText.setText("");
        ddlKeywordsText.setText("");
        dmlKeywordsText.setText("");
        executeKeywordsText.setText("");
        transactionKeywordsText.setText("");
    }

    public boolean isNotChanged(@NotNull SQLDialectDescriptor descriptor) {
        return getSetFromText(reservedWordsText).equals(descriptor.getReservedWords(true))
            && getSetFromText(executeKeywordsText).equals(descriptor.getExecuteKeywords(true))
            && getSetFromText(dmlKeywordsText).equals(descriptor.getDMLKeywords(true))
            && getSetFromText(transactionKeywordsText).equals(descriptor.getTransactionKeywords(true))
            && getSetFromText(ddlKeywordsText).equals(descriptor.getDDLKeywords(true))
            && getSetFromText(dataTypesText).equals(descriptor.getDataTypes(true))
            && getSetFromText(functionNamesText).equals(descriptor.getFunctions(true));

    }

    @Override
    public boolean performOk() {
        SQLDialectDescriptor descriptor = SQLDialectRegistry.getInstance().getDialect(curDialect.getId());
        if (isNotChanged(descriptor)) {
            return super.performOk();
        }
        descriptor.setTransformer(null);
        descriptor.setTransformer(createTransformer(descriptor));
        //store.setValue(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_READ, bindEmbeddedReadCheck.getSelection());
        SQLDialectRegistry.getInstance().saveDialectModifiers();
        if (dialectTable != null && dialectTable.getSelection().length != 0) {
            dialectTable.getSelection()[0].setFont(boldFont);
        }
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