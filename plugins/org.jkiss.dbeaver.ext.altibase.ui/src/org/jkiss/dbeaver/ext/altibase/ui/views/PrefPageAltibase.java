/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.ui.internal.AltibaseUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

public class PrefPageAltibase extends TargetPrefPage {

    static final Log log = Log.getLog(PrefPageAltibase.class);
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.altibase.general";
    
    private int planTypeIdx;
    private Button[] planTypeBtns;
    private Button enableDbmsOutputCheck;
    private Button enablePlanPrefixDepthCheck;
    
    public PrefPageAltibase() {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }
    
    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor) {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return (store.contains(AltibaseConstants.PREF_EXPLAIN_PLAN_TYPE) ||
                store.contains(AltibaseConstants.PREF_DBMS_OUTPUT) ||
                store.contains(AltibaseConstants.PREF_PLAN_PREFIX));
    }

    @Override
    protected Control createPreferenceContent(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        // Explain plan
        Group planGroup = UIUtils.createControlGroup(composite, 
                AltibaseUIMessages.pref_page_altibase_explain_plan_legend, 1, GridData.FILL_HORIZONTAL, 0);

        /* Description */
        Label descLabel = new Label(planGroup, SWT.WRAP);
        descLabel.setText(AltibaseUIMessages.pref_page_altibase_explain_plan_content);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        descLabel.setLayoutData(gd);
        
        /* Buttons */
        int i = 0;
        int size = AltibaseConstants.ExplainPlan.values().length;
        planTypeBtns = new Button[size];
        
        SelectionListener selectionListener = new SelectionAdapter() {
            
            @Override
            public void widgetSelected(final SelectionEvent e) {
                super.widgetSelected(e);
                for (int i = 0; i < size; i++) {
                    if (planTypeBtns[i] != null && planTypeBtns[i].getSelection()) {
                        planTypeIdx = i;
                    }
                }
            }
        };

        /*
         * Explain plains (Execution plan)
         */
        for (AltibaseConstants.ExplainPlan explainplan : AltibaseConstants.ExplainPlan.values()) {
            planTypeBtns[i++] = UIUtils.createRadioButton(
                    planGroup, explainplan.getTitle(), null, selectionListener);
        }
        
        // Misc.
        {
            Group miscGroup = UIUtils.createControlGroup(
                    composite, AltibaseUIMessages.pref_page_altibase_legend_misc, 1, GridData.FILL_HORIZONTAL, 0);
            enableDbmsOutputCheck = UIUtils.createCheckbox(
                    miscGroup, AltibaseUIMessages.pref_page_altibase_checkbox_enable_dbms_output, true);
            enablePlanPrefixDepthCheck = UIUtils.createCheckbox(
                    miscGroup, AltibaseUIMessages.pref_page_altibase_checkbox_plan_prefix_depth, true);
        }
        
        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store) {
        planTypeIdx = store.getInt(AltibaseConstants.PREF_EXPLAIN_PLAN_TYPE);
        if (planTypeBtns[planTypeIdx] != null) {
            selectButtonEvent(planTypeIdx);
        }
        
        enableDbmsOutputCheck.setSelection(store.getBoolean(AltibaseConstants.PREF_DBMS_OUTPUT));
        enablePlanPrefixDepthCheck.setSelection(store.getBoolean(AltibaseConstants.PREF_PLAN_PREFIX));
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store) {
        store.setValue(AltibaseConstants.PREF_EXPLAIN_PLAN_TYPE, planTypeIdx);
        store.setValue(AltibaseConstants.PREF_DBMS_OUTPUT, enableDbmsOutputCheck.getSelection());
        store.setValue(AltibaseConstants.PREF_PLAN_PREFIX, enablePlanPrefixDepthCheck.getSelection());
        
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store) {
        store.setToDefault(AltibaseConstants.PREF_EXPLAIN_PLAN_TYPE);
        store.setToDefault(AltibaseConstants.PREF_DBMS_OUTPUT);
        store.setToDefault(AltibaseConstants.PREF_PLAN_PREFIX);
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return true;
    }
    
    @Override
    protected String getPropertyPageID() {
        return PAGE_ID;
    }
    
    private void selectButtonEvent(int idx) {
        if (planTypeBtns[planTypeIdx] != null) {
            planTypeBtns[idx].setSelection(true);
            planTypeBtns[idx].notifyListeners(SWT.Selection, new Event());
        }
    }
}
