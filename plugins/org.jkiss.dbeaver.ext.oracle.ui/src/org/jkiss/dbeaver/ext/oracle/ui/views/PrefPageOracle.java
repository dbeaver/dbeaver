/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageOracle
 */
public class PrefPageOracle extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.oracle.general"; //$NON-NLS-1$

    private Text explainTableText;
    private Button rowidSupportCheck;
    private Button enableDbmsOuputCheck;
    private Button readAllSynonymsCheck;
    private Button disableScriptEscapeProcessingCheck;

    public PrefPageOracle()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(OracleConstants.PREF_EXPLAIN_TABLE_NAME) ||
            store.contains(OracleConstants.PREF_SUPPORT_ROWID) ||
            store.contains(OracleConstants.PREF_DBMS_OUTPUT) ||
            store.contains(OracleConstants.PREF_DBMS_READ_ALL_SYNONYMS) ||
            store.contains(OracleConstants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING)
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

        {
            Group planGroup = UIUtils.createControlGroup(composite, OracleUIMessages.pref_page_oracle_legend_executin_plan, 2, GridData.FILL_HORIZONTAL, 0);

            Label descLabel = new Label(planGroup, SWT.WRAP);
            descLabel.setText(OracleUIMessages.pref_page_oracle_label_by_default_plan_table);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
            descLabel.setLayoutData(gd);

            explainTableText = UIUtils.createLabelText(planGroup, OracleUIMessages.pref_page_oracle_label_plan_table, "", SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL)); //$NON-NLS-2$
        }

        {
            Group miscGroup = UIUtils.createControlGroup(composite, OracleUIMessages.pref_page_oracle_legend_misc, 1, GridData.FILL_HORIZONTAL, 0);
            rowidSupportCheck = UIUtils.createCheckbox(miscGroup, OracleUIMessages.pref_page_oracle_checkbox_use_rowid_to_identify_rows, true);
            enableDbmsOuputCheck = UIUtils.createCheckbox(miscGroup, OracleUIMessages.pref_page_oracle_checkbox_enable_dbms_output, true);
            readAllSynonymsCheck = UIUtils.createCheckbox(miscGroup, OracleUIMessages.pref_page_oracle_checkbox_read_all_synonyms, OracleUIMessages.pref_page_oracle_label_if_unchecked_java_classes, true, 1);
            disableScriptEscapeProcessingCheck = UIUtils.createCheckbox(miscGroup, OracleUIMessages.pref_page_oracle_checkbox_disable_escape_processing, OracleUIMessages.pref_page_oracle_label_disable_client_side_parser, true, 1);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        explainTableText.setText(store.getString(OracleConstants.PREF_EXPLAIN_TABLE_NAME));
        rowidSupportCheck.setSelection(store.getBoolean(OracleConstants.PREF_SUPPORT_ROWID));
        enableDbmsOuputCheck.setSelection(store.getBoolean(OracleConstants.PREF_DBMS_OUTPUT));
        readAllSynonymsCheck.setSelection(store.getBoolean(OracleConstants.PREF_DBMS_READ_ALL_SYNONYMS));
        disableScriptEscapeProcessingCheck.setSelection(store.getBoolean(OracleConstants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING));
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        store.setValue(OracleConstants.PREF_EXPLAIN_TABLE_NAME, explainTableText.getText());
        store.setValue(OracleConstants.PREF_SUPPORT_ROWID, rowidSupportCheck.getSelection());
        store.setValue(OracleConstants.PREF_DBMS_OUTPUT, enableDbmsOuputCheck.getSelection());
        store.setValue(OracleConstants.PREF_DBMS_READ_ALL_SYNONYMS, readAllSynonymsCheck.getSelection());
        store.setValue(OracleConstants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING, disableScriptEscapeProcessingCheck.getSelection());
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(OracleConstants.PREF_EXPLAIN_TABLE_NAME);
        store.setToDefault(OracleConstants.PREF_SUPPORT_ROWID);
        store.setToDefault(OracleConstants.PREF_DBMS_OUTPUT);
        store.setToDefault(OracleConstants.PREF_DBMS_READ_ALL_SYNONYMS);
        store.setToDefault(OracleConstants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}