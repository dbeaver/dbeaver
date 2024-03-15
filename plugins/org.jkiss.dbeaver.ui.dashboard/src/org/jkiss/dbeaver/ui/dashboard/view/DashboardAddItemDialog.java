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
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.dashboard.DBDashboardContext;
import org.jkiss.dbeaver.model.dashboard.DBDashboardFolder;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardProviderDescriptor;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewConfiguration;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRendererDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardUIRegistry;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard add dialog
 */
public class DashboardAddItemDialog extends BaseDialog {

    private static final Log log = Log.getLog(DashboardAddItemDialog.class);

    private static final String DIALOG_ID = "DBeaver.DashboardAddDialog";//$NON-NLS-1$

    private final DashboardViewConfiguration viewConfiguration;
    private DashboardDescriptor selectedDashboard;

    public DashboardAddItemDialog(Shell parentShell, DashboardViewConfiguration viewConfiguration) {
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

//        AdvancedListViewer listViewer = new AdvancedListViewer(dialogArea, SWT.NONE);
//        listViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        TreeViewer dashboardTable = new TreeViewer(dialogArea, SWT.BORDER | SWT.FULL_SELECTION);

        dashboardTable.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        Tree table = dashboardTable.getTree();
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 200;
        table.setLayoutData(gd);
        table.setHeaderVisible(true);
        UIUtils.createTreeColumn(table, SWT.LEFT, UIDashboardMessages.dialog_add_dashboard_column_name);
        UIUtils.createTreeColumn(table, SWT.LEFT, UIDashboardMessages.dialog_add_dashboard_column_description);

        dashboardTable.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                if (cell.getElement() instanceof DashboardProviderDescriptor dpd) {
                    if (cell.getColumnIndex() == 0) {
                        cell.setText(dpd.getName());
                        cell.setImage(DBeaverIcons.getImage(dpd.getIcon()));
                    } else {
                        cell.setText(CommonUtils.notEmpty(dpd.getDescription()));
                    }
                } else if (cell.getElement() instanceof DBDashboardFolder folder) {
                    if (cell.getColumnIndex() == 0) {
                        cell.setText(folder.getName());
                        DBPImage icon = folder.getIcon();
                        if (icon == null) {
                            icon = DBIcon.TREE_FOLDER;
                        }
                        cell.setImage(DBeaverIcons.getImage(icon));
                    } else {
                        cell.setText(CommonUtils.notEmpty(folder.getDescription()));
                    }
                } else if (cell.getElement() instanceof DashboardDescriptor dashboardDescriptor) {
                    if (cell.getColumnIndex() == 0) {
                        cell.setText(dashboardDescriptor.getName());
                        DBPImage icon = null;
                        if (dashboardDescriptor.isCustom()) {
                            icon = DBIcon.TYPE_OBJECT;
                        } else {
                            DashboardRendererDescriptor viewType = DashboardUIRegistry.getInstance().getViewType(dashboardDescriptor.getDashboardRenderer());
                            if (viewType != null) {
                                icon = viewType.getIcon();
                            }
                        }
                        if (icon != null) {
                            cell.setImage(DBeaverIcons.getImage(icon));
                        }

                    } else {
                        cell.setText(CommonUtils.notEmpty(dashboardDescriptor.getDescription()));
                    }
                }
            }
        });
        dashboardTable.addDoubleClickListener(event -> {
            if ((dashboardTable.getStructuredSelection().getFirstElement() instanceof DashboardDescriptor)) {
                okPressed();
            }
        });
        dashboardTable.addSelectionChangedListener(event -> {
            ISelection selection = dashboardTable.getSelection();
            if (selection instanceof IStructuredSelection ss) {
                if (ss.getFirstElement() instanceof DashboardDescriptor dd) {
                    selectedDashboard = dd;
                } else {
                    selectedDashboard = null;
                }
            }
            enableButton(IDialogConstants.OK_ID, selectedDashboard != null);
        });
        table.addPaintListener(e -> {
            if (table.getItemCount() == 0) {
                final String dbmsName = viewConfiguration.getDataSourceContainer().getDriver().getName();
                final String msg = NLS.bind(UIDashboardMessages.dialog_add_dashboard_message_no_more_dashboards_for, dbmsName);
                UIUtils.drawMessageOverControl(table, e, msg, 0);
            }
        });
        dashboardTable.setContentProvider(new TreeContentProvider() {
            @Override
            public Object[] getChildren(Object parentElement) {
                try {
                    DBDashboardContext context = new DBDashboardContext(viewConfiguration.getDataSourceContainer());
                    if (parentElement instanceof DBDashboardFolder df) {
                        List<DBDashboardFolder> subFolders = df.loadSubFolders(new VoidProgressMonitor(), context);
                        List<DashboardDescriptor> dashboards = df.loadDashboards(new VoidProgressMonitor(), context);
                        return ArrayUtils.concatArrays(subFolders.toArray(), dashboards.toArray());
                    } else if (parentElement instanceof DashboardProviderDescriptor dpd) {
                        List<Object> children = new ArrayList<>();
                        if (dpd.isSupportsFolders()) {
                            try {
                                UIUtils.runInProgressDialog(monitor -> children.addAll(dpd.getInstance().loadRootFolders(monitor, dpd, context)));
                            } catch (InvocationTargetException e) {
                                DBWorkbench.getPlatformUI().showError("Folders load error", null, e.getTargetException());
                            }
                            return children.toArray();
                        }
                        List<DashboardDescriptor> dashboards = new ArrayList<>(DashboardRegistry.getInstance().getDashboards(
                            dpd,
                            viewConfiguration.getDataSourceContainer(),
                            false));
                        dashboards.removeIf(descriptor -> viewConfiguration.getDashboardConfig(descriptor.getId()) != null);
                        return dashboards.toArray();
                    }
                } catch (DBException e) {
                    log.error("Error reading dashboard info", e);
                }
                return new Object[0];
            }

            @Override
            public boolean hasChildren(Object element) {
                return element instanceof DashboardProviderDescriptor || element instanceof DBDashboardFolder;
            }
        });

        List<DashboardProviderDescriptor> dbProviders = DashboardRegistry.getInstance().getDashboardProviders(
            viewConfiguration.getDataSourceContainer());

        dashboardTable.setInput(dbProviders);
        dashboardTable.expandToLevel(2);

        UIUtils.asyncExec(() -> UIUtils.packColumns(table, true, null));

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

    public DashboardDescriptor getSelectedDashboard() {
        return selectedDashboard;
    }
}