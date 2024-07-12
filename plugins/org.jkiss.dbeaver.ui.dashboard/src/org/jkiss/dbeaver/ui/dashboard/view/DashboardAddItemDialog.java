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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConfiguration;
import org.jkiss.dbeaver.ui.dashboard.view.catalogpanel.DashboardCatalogPanel;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

/**
 * Dashboard add dialog
 */
public class DashboardAddItemDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.DashboardAddDialog";//$NON-NLS-1$

    private final DashboardConfiguration viewConfiguration;
    private DashboardCatalogPanel catalogPanel;

    public DashboardAddItemDialog(Shell parentShell, DashboardConfiguration viewConfiguration) {
        super(parentShell, UIDashboardMessages.dialog_add_dashboard_dialog_title, null);

        this.viewConfiguration = viewConfiguration;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getSettingsSection(UIDashboardActivator.getDefault().getDialogSettings(), DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        catalogPanel = new DashboardCatalogPanel(
            dialogArea,
            viewConfiguration.getProject(),
            viewConfiguration.getDataSourceContainer(),
            item -> viewConfiguration.getItemConfig(item.getId()) != null,
            false) {
            @Override
            protected void handleChartSelected() {
                enableButton(IDialogConstants.OK_ID, getSelectedDashboard() != null);
            }

            @Override
            protected void handleChartSelectedFinal() {
                okPressed();
            }
        };

        return dialogArea;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        ((GridData)parent.getLayoutData()).grabExcessHorizontalSpace = true;

        final Button managerButton = createButton(parent, IDialogConstants.CANCEL_ID, UIDashboardMessages.dialog_add_dashboard_button_manage, false);
        ((GridData) managerButton.getLayoutData()).horizontalAlignment = GridData.BEGINNING;
        ((GridData) managerButton.getLayoutData()).grabExcessHorizontalSpace = true;
        managerButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new DashboardManagerDialog(UIUtils.getActiveWorkbenchShell()).open();
            }
        });

        createButton(parent, IDialogConstants.OK_ID, UIDashboardMessages.dialog_add_dashboard_button_add, true).setEnabled(false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    public DashboardItemConfiguration getSelectedDashboard() {
        return catalogPanel.getSelectedDashboard();
    }
}