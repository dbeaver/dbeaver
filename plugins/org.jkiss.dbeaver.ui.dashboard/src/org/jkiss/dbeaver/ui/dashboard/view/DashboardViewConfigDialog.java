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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewer;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;

public class DashboardViewConfigDialog extends BaseDialog {

    private final DashboardViewer view;
    private Text dashboardNameText;
    private Button initWithDefaultChartsCheck;
    private Button connectOnActivationCheck;

    public DashboardViewConfigDialog(Shell shell, DashboardViewer view) {
        super(shell, NLS.bind(UIDashboardMessages.dialog_dashboard_view_config_title, view.getDataSourceContainer().getName()), null);

        this.view = view;
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

            Text dashboardIdText = UIUtils.createLabelText(viewGroup,
                "ID",
                CommonUtils.notEmpty(view.getConfiguration().getDashboardId()));
            dashboardIdText.setEnabled(false);
            dashboardNameText = UIUtils.createLabelText(viewGroup,
                UIDashboardMessages.dialog_edit_dashboard_maininfo_labels_display_name,
                CommonUtils.notEmpty(view.getConfiguration().getDashboardName()));

            initWithDefaultChartsCheck = UIUtils.createCheckbox(
                viewGroup,
                UIDashboardMessages.dialog_dashboard_view_config_group_viewcfg_checkbox_init_default,
                UIDashboardMessages.dialog_dashboard_view_config_group_viewcfg_checkbox_init_default_tooltip,
                view.getConfiguration().isInitDefaultCharts(),
                2);
            connectOnActivationCheck = UIUtils.createCheckbox(
                viewGroup,
                UIDashboardMessages.dialog_dashboard_view_config_group_viewcfg_checkbox_connect,
                UIDashboardMessages.dialog_dashboard_view_config_group_viewcfg_checkbox_connect_tooltip,
                view.getConfiguration().isOpenConnectionOnActivate(),
                2);

            // #4209 Dashboard: disable separate connection option (too aggressive)
            /*Button separateConnectionCheck = UIUtils.createCheckbox(viewGroup, UIDashboardMessages.dialog_dashboard_view_config_group_viewcfg_checkbox_use_separate_conn, UIDashboardMessages.dialog_dashboard_view_config_group_viewcfg_checkbox_use_separate_conn_tooltip, viewConfiguration.isUseSeparateConnection(), 2);
            separateConnectionCheck
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        viewConfiguration.setUseSeparateConnection(((Button)e.widget).getSelection());
                    }
                });
            separateConnectionCheck.setEnabled(false);*/
        }

        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        if (DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
            final Button managerButton = createButton(
                parent,
                IDialogConstants.CANCEL_ID,
                UIDashboardMessages.dialog_add_dashboard_button_manage,
                false
            );
            ((GridData) managerButton.getLayoutData()).horizontalAlignment = GridData.BEGINNING;
            ((GridData) managerButton.getLayoutData()).grabExcessHorizontalSpace = true;
            managerButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    new DashboardManagerDialog(UIUtils.getActiveWorkbenchShell()).open();
                }
            });
        }
        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void okPressed() {
        try {
            String dbName = dashboardNameText.getText();
            if (CommonUtils.isEmpty(dbName)) {
                throw new IOException("Empty dashboard name");
            }
            view.getConfiguration().setDashboardName(dbName);
            view.getConfiguration().setInitDefaultCharts(initWithDefaultChartsCheck.getSelection());
            view.getConfiguration().setOpenConnectionOnActivate(connectOnActivationCheck.getSelection());
            view.getConfigurationList().saveConfiguration();

            super.okPressed();
        } catch (IOException e) {
            DBWorkbench.getPlatformUI().showError("Error saving dashboard view", null, e);
        }
    }

}
