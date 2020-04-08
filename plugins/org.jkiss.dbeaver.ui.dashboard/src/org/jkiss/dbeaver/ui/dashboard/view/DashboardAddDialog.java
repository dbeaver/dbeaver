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
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewConfiguration;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;

/**
 * Dashboard add dialog
 */
public class DashboardAddDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.DashboardAddDialog";//$NON-NLS-1$

    private final DashboardViewConfiguration viewConfiguration;
    private DashboardDescriptor selectedDashboard;

    public DashboardAddDialog(Shell parentShell, DashboardViewConfiguration viewConfiguration) {
        super(parentShell, "Add Dashboard", null);

        this.viewConfiguration = viewConfiguration;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getSettingsSection(UIDashboardActivator.getDefault().getDialogSettings(), DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

//        AdvancedListViewer listViewer = new AdvancedListViewer(dialogArea, SWT.NONE);
//        listViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        TableViewer dashboardTable = new TableViewer(dialogArea, SWT.BORDER | SWT.FULL_SELECTION);

        dashboardTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        Table table = dashboardTable.getTable();
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 200;
        table.setLayoutData(gd);
        table.setHeaderVisible(true);
        UIUtils.createTableColumn(table, SWT.LEFT, "Name");
        UIUtils.createTableColumn(table, SWT.LEFT, "Description");

        dashboardTable.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                DashboardDescriptor dashboardDescriptor = (DashboardDescriptor) cell.getElement();
                if (cell.getColumnIndex() == 0) {
                    cell.setText(dashboardDescriptor.getName());
                } else {
                    cell.setText(CommonUtils.notEmpty(dashboardDescriptor.getDescription()));
                }
            }
        });
        dashboardTable.addDoubleClickListener(event -> {
            if (!dashboardTable.getSelection().isEmpty()) {
                okPressed();
            }
        });
        dashboardTable.addSelectionChangedListener(event -> {
            ISelection selection = dashboardTable.getSelection();
            getButton(IDialogConstants.OK_ID).setEnabled(!selection.isEmpty());
            if (selection instanceof IStructuredSelection) {
                selectedDashboard = (DashboardDescriptor) ((IStructuredSelection) selection).getFirstElement();
            }
            getButton(IDialogConstants.OK_ID).setEnabled(selectedDashboard != null);
        });
        table.addPaintListener(e -> {
            if (table.getItemCount() == 0) {
                UIUtils.drawMessageOverControl(table, e, "No more dashboards for " + viewConfiguration.getDataSourceContainer().getDriver().getName(), 0);
            }
        });
        dashboardTable.setContentProvider(new ListContentProvider());

        java.util.List<DashboardDescriptor> dashboards = new ArrayList<>(DashboardRegistry.getInstance().getDashboards(
            viewConfiguration.getDataSourceContainer(), false));
        dashboards.removeIf(descriptor -> viewConfiguration.getDashboardConfig(descriptor.getId()) != null);
        dashboardTable.setInput(dashboards);

        UIUtils.asyncExec(() -> UIUtils.packColumns(table, true));

        return dialogArea;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        ((GridData)parent.getLayoutData()).grabExcessHorizontalSpace = true;

        final Button managerButton = createButton(parent, IDialogConstants.CANCEL_ID, "Manage ...", false);
        ((GridData) managerButton.getLayoutData()).horizontalAlignment = GridData.BEGINNING;
        ((GridData) managerButton.getLayoutData()).grabExcessHorizontalSpace = true;
        managerButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new DashboardManagerDialog(UIUtils.getActiveWorkbenchShell()).open();
            }
        });

        createButton(parent, IDialogConstants.OK_ID, "Add", true).setEnabled(false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    public DashboardDescriptor getSelectedDashboard() {
        return selectedDashboard;
    }

}