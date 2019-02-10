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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewConfiguration;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class DashboardViewConfigDialog extends BaseDialog {

    private static final int MANAGER_BUTTON_ID = 1000;
    private DashboardViewConfiguration viewConfiguration;

    public DashboardViewConfigDialog(Shell shell, DashboardViewConfiguration viewConfiguration)
    {
        super(shell, "Dashboard [" + viewConfiguration.getDataSourceContainer().getName() + " / " + viewConfiguration.getViewId() + "]", null);

        this.viewConfiguration = viewConfiguration;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return null;//UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);

        {
            Group viewGroup = UIUtils.createControlGroup(composite, "View configuration", 2, GridData.FILL_HORIZONTAL, 0);

            UIUtils.createCheckbox(viewGroup, "Connect to on activation", "Open database connection on view activation", viewConfiguration.isOpenConnectionOnActivate(), 2)
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        viewConfiguration.setOpenConnectionOnActivate(((Button)e.widget).getSelection());
                    }
                });
            UIUtils.createCheckbox(viewGroup, "Use separate connection", "Open special connection for charts data reading. Otherwise use main datasource connection", viewConfiguration.isUseSeparateConnection(), 2)
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        viewConfiguration.setUseSeparateConnection(((Button)e.widget).getSelection());
                    }
                });
        }

        return parent;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        return contents;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        final Button createButton = createButton(parent, MANAGER_BUTTON_ID, "Manage ...", false);
        ((GridData) createButton.getLayoutData()).horizontalAlignment = GridData.BEGINNING;
        ((GridData) createButton.getLayoutData()).grabExcessHorizontalSpace = true;

        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void okPressed() {
        super.okPressed();
        viewConfiguration.saveSettings();
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == MANAGER_BUTTON_ID) {
            new DashboardManagerDialog(getShell()).open();
        } else {
            super.buttonPressed(buttonId);
        }
    }
}
