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
package org.jkiss.dbeaver.ui.controls.autorefresh;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class AutoRefreshConfigDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.AutoRefreshConfigDialog";//$NON-NLS-1$

    private RefreshSettings refreshSettings;

    AutoRefreshConfigDialog(Shell parentShell, RefreshSettings settings) {
        super(parentShell, "Auto-refresh configuration", UIIcon.RS_SCHED_START);
        this.refreshSettings = new RefreshSettings(settings);
    }

    public RefreshSettings getRefreshSettings() {
        return refreshSettings;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);

        Group settingsGroup = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_BOTH, 0);
        final Spinner intervalSpinner = UIUtils.createLabelSpinner(settingsGroup, "Interval (sec)", "Auto-refresh interval in seconds", refreshSettings.getRefreshInterval(), 0, Integer.MAX_VALUE);
        intervalSpinner.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshSettings.setRefreshInterval(intervalSpinner.getSelection());
            }
        });
        final Button stopOnErrorCheck = UIUtils.createCheckbox(settingsGroup, "Stop on error", "Stop auto-refresh if error happens", refreshSettings.isStopOnError(), 2);
        stopOnErrorCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshSettings.setStopOnError(stopOnErrorCheck.getSelection());
            }
        });

        return composite;
    }

}
