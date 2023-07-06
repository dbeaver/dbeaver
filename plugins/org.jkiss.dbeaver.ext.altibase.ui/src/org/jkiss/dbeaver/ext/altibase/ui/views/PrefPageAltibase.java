package org.jkiss.dbeaver.ext.altibase.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.ui.internal.AltibaseUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;

public class PrefPageAltibase extends TargetPrefPage {

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.altibase.general";
    
    private int planTypeIdx;
    private Button[] planTypeBtns;
    
    public PrefPageAltibase()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }
    
    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor) {
        return dataSourceDescriptor.getPreferenceStore().contains(AltibaseConstants.PREF_EXPLAIN_PLAN_TYPE);
    }

    @Override
    protected Control createPreferenceContent(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group planGroup = UIUtils.createControlGroup(composite, 
                AltibaseUIMessages.pref_page_altibase_execution_plan_legend, 1, GridData.FILL_HORIZONTAL, 0);

        Label descLabel = new Label(planGroup, SWT.WRAP);
        descLabel.setText(AltibaseUIMessages.pref_page_altibase_execution_plan_content);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        //gd.horizontalSpan = 2;
        descLabel.setLayoutData(gd);
        
        /* Buttons */
        int i = 0;
        planTypeBtns = new Button[AltibaseConstants.EXPLAIN_PLAN_OPTION_TITLES.length];
        SelectionListener selectionListener = new SelectionAdapter(){
            @Override
            public void widgetSelected(final SelectionEvent e){
                super.widgetSelected(e);
                for(int i = 0; i < AltibaseConstants.EXPLAIN_PLAN_OPTION_TITLES.length; i++) {
                    if (planTypeBtns[i] != null && planTypeBtns[i].getSelection()) {
                        planTypeIdx = i;
                    }
                }
            }
        };

        for(String name:AltibaseConstants.EXPLAIN_PLAN_OPTION_TITLES) {
            planTypeBtns[i++] = UIUtils.createRadioButton(planGroup, name, selectionListener, selectionListener);
        }
        
        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store) {
        planTypeIdx = store.getInt(AltibaseConstants.PREF_EXPLAIN_PLAN_TYPE);
        if (planTypeBtns[planTypeIdx] != null) {
            planTypeBtns[planTypeIdx].setSelection(true);
            planTypeBtns[planTypeIdx].notifyListeners(SWT.Selection, new Event());
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store) {
        store.setValue(AltibaseConstants.PREF_EXPLAIN_PLAN_TYPE, planTypeIdx);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store) {
        store.setToDefault(AltibaseConstants.PREF_EXPLAIN_PLAN_TYPE);
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return true;
    }
    
    @Override
    protected String getPropertyPageID() {
        return PAGE_ID;
    }
}
