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
package org.jkiss.dbeaver.ext.cubrid.ui.views;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.ui.internal.CubridMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

public class PrefPageCubrid extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.cubrid.general"; //$NON-NLS-1$
    private Button trace;
    private Button info;
    private Button allInfo;

    public PrefPageCubrid() {
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dsContainer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return true;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store) {
        trace.setSelection(store.getBoolean(CubridConstants.STATISTIC_TRACE));

        info.setSelection(store.getString(CubridConstants.STATISTIC).equals(CubridConstants.STATISTIC_INFO));
        allInfo.setSelection(store.getString(CubridConstants.STATISTIC).equals(CubridConstants.STATISTIC_ALL_INFO));
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store) {

        PrefUtils.savePreferenceStore(store);

    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        this.clearPreferences(getTargetPreferenceStore());
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store) {
        store.setValue(CubridConstants.STATISTIC_TRACE, false);
        store.setValue(CubridConstants.STATISTIC, "");
        trace.setSelection(false);
        info.setSelection(false);
        allInfo.setSelection(false);
    }

    @Override
    protected String getPropertyPageID() {

        return PAGE_ID;
    }

    @Override
    protected Control createPreferenceContent(Composite parent) {
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        Composite composite = UIUtils.createPlaceholder(parent, 1);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {

            Group planGroup = UIUtils.createControlGroup(composite, CubridMessages.statistic_group_editor_title, 1, GridData.FILL_HORIZONTAL, 0);
            trace = UIUtils.createCheckbox(planGroup, CubridMessages.statistic_trace_info, false);

            trace.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    preferenceStore.setValue(CubridConstants.STATISTIC_TRACE, ((Button) e.widget).getSelection());
                }
            });

            SelectionListener radioListener = new SelectionAdapter()
            {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    if (e.widget.getData().equals(preferenceStore.getString(CubridConstants.STATISTIC))) {
                        ((Button) e.widget).setSelection(false);
                        preferenceStore.setValue(CubridConstants.STATISTIC, "");
                    } else {
                        preferenceStore.setValue(CubridConstants.STATISTIC, e.widget.getData().toString());
                    }

                }
            };
            info = UIUtils.createRadioButton(planGroup, CubridMessages.statistic_info, CubridConstants.STATISTIC_INFO, radioListener);
            allInfo = UIUtils.createRadioButton(planGroup, CubridMessages.statistic_all_info, CubridConstants.STATISTIC_ALL_INFO, radioListener);

        }
        return composite;
    }

}