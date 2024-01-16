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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

public class PrefPageMiscellaneous extends AbstractPrefPage implements IWorkbenchPreferencePage {
    private Button holidayDecorationsCheck;

    public PrefPageMiscellaneous() {
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    public void init(IWorkbench workbench) {
        // nothing to init
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        final Composite composite = UIUtils.createPlaceholder(parent, 1, 5);
        final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        {
            final Group group = UIUtils.createControlGroup(composite, "Holiday decorations", 1, GridData.FILL_HORIZONTAL, 0);

            holidayDecorationsCheck = UIUtils.createCheckbox(group, "Show holiday decorations", false);
            holidayDecorationsCheck.setLayoutData(new GridData());
            holidayDecorationsCheck.setSelection(store.getBoolean(DBeaverPreferences.UI_SHOW_HOLIDAY_DECORATIONS));
        }

        UIUtils.createInfoLabel(composite, CoreMessages.pref_page_ui_general_label_options_take_effect_after_restart);

        return composite;
    }

    @Override
    protected void performDefaults() {
        final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        holidayDecorationsCheck.setSelection(store.getDefaultBoolean(DBeaverPreferences.UI_SHOW_HOLIDAY_DECORATIONS));

        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        final DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        store.setValue(DBeaverPreferences.UI_SHOW_HOLIDAY_DECORATIONS, holidayDecorationsCheck.getSelection());

        return super.performOk();
    }
}
