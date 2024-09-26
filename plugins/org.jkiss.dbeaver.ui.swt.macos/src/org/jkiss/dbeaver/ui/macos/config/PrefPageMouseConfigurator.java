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
package org.jkiss.dbeaver.ui.macos.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.macos.CocoaUIService;
import org.jkiss.dbeaver.ui.preferences.PrefPageMiscellaneousAbstract;

/**
 * PrefPageMouseConfigurator
 */
public class PrefPageMouseConfigurator implements
    IObjectPropertyConfigurator<PrefPageMiscellaneousAbstract, PrefPageMiscellaneousAbstract> {


    private Button tooltipDelayCheck;
    private Text tooltipDelayText;

    @Override
    public void createControl(@NotNull Composite parent, PrefPageMiscellaneousAbstract object, @NotNull Runnable propertyChangeListener) {
        final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        {
            final Group group = UIUtils.createControlGroup(parent, "Mouse pointer", 2, GridData.FILL_HORIZONTAL, 0);

            tooltipDelayCheck = UIUtils.createCheckbox(group, "Set tooltip delay (ms)", false);
            tooltipDelayCheck.setLayoutData(new GridData());
            tooltipDelayCheck.setSelection(store.getInt(CocoaUIService.PREF_TOOLTIP_DELAY) > 0);
            tooltipDelayCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    tooltipDelayText.setEnabled(tooltipDelayCheck.getSelection());
                }
            });

            tooltipDelayText = new Text(group, SWT.BORDER);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = UIUtils.getFontHeight(tooltipDelayText) * 5;
            tooltipDelayText.setLayoutData(gd);
            tooltipDelayText.setText(String.valueOf(store.getInt(CocoaUIService.PREF_TOOLTIP_DELAY)));
            tooltipDelayText.setEnabled(false);
        }
    }

    @Override
    public void loadSettings(@NotNull PrefPageMiscellaneousAbstract prefPageMiscellaneousAbstract) {
        DBPPreferenceStore store = ModelPreferences.getPreferences();

        tooltipDelayCheck.setSelection(store.getBoolean(CocoaUIService.PREF_TOOLTIP_DELAY_ENABLED));
        tooltipDelayText.setText(store.getString(CocoaUIService.PREF_TOOLTIP_DELAY));

        tooltipDelayText.setEnabled(tooltipDelayCheck.getSelection());
    }

    @Override
    public void saveSettings(@NotNull PrefPageMiscellaneousAbstract prefPageMiscellaneousAbstract) {
        DBPPreferenceStore store = ModelPreferences.getPreferences();

        store.setValue(CocoaUIService.PREF_TOOLTIP_DELAY_ENABLED, tooltipDelayCheck.getSelection());
        store.setValue(CocoaUIService.PREF_TOOLTIP_DELAY, tooltipDelayText.getText());

        CocoaUIService.updateTooltipDefaults();
    }

    @Override
    public void resetSettings(@NotNull PrefPageMiscellaneousAbstract prefPageMiscellaneousAbstract) {

    }

    @Override
    public boolean isComplete() {
        return true;
    }

}
