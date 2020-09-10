/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewConfiguration;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.internal.UIMessages;

public class DashboardViewConfigDialog extends BaseDialog {

    private DashboardViewConfiguration viewConfiguration;

    public DashboardViewConfigDialog(Shell shell, DashboardViewConfiguration viewConfiguration) {
        super(shell, NLS.bind(UIDashboardMessages.dialog_dashboard_view_config_title, viewConfiguration.getDataSourceContainer().getName()), null);

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
            Group viewGroup = UIUtils.createControlGroup(composite, UIDashboardMessages.dialog_dashboard_view_config_group_viewcfg, 2, GridData.FILL_HORIZONTAL, 0);

            Button connectOnActivationCheck = UIUtils.createCheckbox(viewGroup, UIDashboardMessages.dialog_dashboard_view_config_group_viewcfg_checkbox_connect, UIDashboardMessages.dialog_dashboard_view_config_group_viewcfg_checkbox_connect_tooltip, viewConfiguration.isOpenConnectionOnActivate(), 2);
            connectOnActivationCheck
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        viewConfiguration.setOpenConnectionOnActivate(((Button)e.widget).getSelection());
                    }
                });
            //connectOnActivationCheck.setEnabled(false);
            Button separateConnectionCheck = UIUtils.createCheckbox(viewGroup, UIDashboardMessages.dialog_dashboard_view_config_group_viewcfg_checkbox_use_separate_conn, UIDashboardMessages.dialog_dashboard_view_config_group_viewcfg_checkbox_use_separate_conn_tooltip, viewConfiguration.isUseSeparateConnection(), 2);
            separateConnectionCheck
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        viewConfiguration.setUseSeparateConnection(((Button)e.widget).getSelection());
                    }
                });
            separateConnectionCheck.setEnabled(false);
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
        final Button managerButton = createButton(parent, IDialogConstants.CANCEL_ID, UIDashboardMessages.dialog_dashboard_view_config_button_manage, false);
        ((GridData) managerButton.getLayoutData()).horizontalAlignment = GridData.BEGINNING;
        ((GridData) managerButton.getLayoutData()).grabExcessHorizontalSpace = true;
        managerButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new DashboardManagerDialog(UIUtils.getActiveWorkbenchShell()).open();
            }
        });

        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void okPressed() {
        super.okPressed();
        viewConfiguration.saveSettings();
    }

}
