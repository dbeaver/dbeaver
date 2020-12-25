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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardMessages;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DashboardManagerDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.DashboardManagerDialog";//$NON-NLS-1$

    private DashboardDescriptor selectedDashboard;

    private Button newButton;
    private Button copyButton;
    private Button editButton;
    private Button deleteButton;
    private TreeViewer treeViewer;

    public DashboardManagerDialog(Shell shell) {
        super(shell, UIDashboardMessages.dialog_dashboard_manager_title, null);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getSettingsSection(UIDashboardActivator.getDefault().getDialogSettings(), DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        getShell().setMinimumSize(300, 300);

        Composite dialogArea = super.createDialogArea(parent);

        Composite group = UIUtils.createPlaceholder(dialogArea, 2);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            treeViewer = new TreeViewer(group, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            gd.widthHint = 300;
            treeViewer.getControl().setLayoutData(gd);
            treeViewer.getTree().setHeaderVisible(true);
            UIUtils.createTreeColumn(treeViewer.getTree(), SWT.LEFT, UIDashboardMessages.dialog_dashboard_manager_treecolumn_name);
            //UIUtils.createTreeColumn(treeViewer.getTree(), SWT.LEFT, "Description");

            treeViewer.setContentProvider(new TreeContentProvider() {
                @Override
                public Object[] getChildren(Object parentElement) {
                    List<? extends DBPNamedObject> result = null;
                    if (parentElement instanceof List) {
                        result = (List) parentElement;
                    } else if (parentElement instanceof DBPDataSourceProviderDescriptor) {
                        result = DashboardRegistry.getInstance().getDashboards((DBPDataSourceProviderDescriptor)parentElement, false);
                    } else if (parentElement instanceof DBPDriver) {
                        result = DashboardRegistry.getInstance().getDashboards((DBPDriver)parentElement, false);
                    }
                    if (result == null) {
                        return new Object[0];
                    }
                    result.sort(DBUtils.nameComparator());
                    return result.toArray();
                }

                @Override
                public boolean hasChildren(Object element) {
                    if (element instanceof DashboardDescriptor) {
                        return false;
                    }
                    return true;
                }
            });
            treeViewer.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    DBPNamedObject element = (DBPNamedObject) cell.getElement();
                    if (cell.getColumnIndex() == 0) {
                        cell.setText(element.getName());
                        if (element instanceof DBPDriver) {
                            cell.setImage(DBeaverIcons.getImage(((DBPDriver) element).getIcon()));
                        } else if (element instanceof DBPDataSourceProviderDescriptor) {
                            cell.setImage(DBeaverIcons.getImage(((DBPDataSourceProviderDescriptor) element).getIcon()));
                        } else if (element instanceof DashboardDescriptor) {
                            DashboardDescriptor dashboardDescriptor = (DashboardDescriptor) element;
                            DBPImage icon;
                            if (dashboardDescriptor.isCustom()) {
                                icon = DBIcon.TYPE_OBJECT;
                            } else {
                                icon = dashboardDescriptor.getDefaultViewType().getIcon();
                            }
                            if (icon != null) {
                                cell.setImage(DBeaverIcons.getImage(icon));
                            }
                        }
                    } else {
                        if (element instanceof DBPDriver) {
                            cell.setText(CommonUtils.notEmpty(((DBPDriver) element).getDescription()));
                        } else if (element instanceof DBPDataSourceProviderDescriptor) {
                            cell.setText(((DBPDataSourceProviderDescriptor) element).getDescription());
                        }
                    }
                }
            });
            treeViewer.setInput(DashboardRegistry.getInstance().getAllSupportedSources());

            treeViewer.addDoubleClickListener(event -> {
                if (selectedDashboard != null) {
                    editDashboard();
                }
            });
            treeViewer.addSelectionChangedListener(event -> {
                this.selectedDashboard = null;
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
                    if (selectedObject instanceof DashboardDescriptor) {
                        this.selectedDashboard = (DashboardDescriptor) selectedObject;
                    }
                }
                this.updateButtons();
            });

            UIUtils.asyncExec(() -> {
                treeViewer.expandAll();
                UIUtils.packColumns(treeViewer.getTree(), true, null);
            });
        }

        {
            Composite buttonBar = new Composite(group, SWT.TOP);
            buttonBar.setLayout(new GridLayout(1, false));
            GridData gd = new GridData(GridData.FILL_VERTICAL);
            buttonBar.setLayoutData(gd);

            newButton = UIUtils.createPushButton(buttonBar, UIDashboardMessages.dialog_dashboard_manager_button_new, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    createDashboard();
                }
            });
            newButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            copyButton = UIUtils.createPushButton(buttonBar, UIDashboardMessages.dialog_dashboard_manager_button_copy, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    copyDashboard();
                }
            });
            copyButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            editButton = UIUtils.createPushButton(buttonBar, UIDashboardMessages.dialog_dashboard_manager_button_edit, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    editDashboard();
                }
            });
            editButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            deleteButton = UIUtils.createPushButton(buttonBar, UIDashboardMessages.dialog_dashboard_manager_button_delete, null, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    deleteDashboard();
                }
            });
            deleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        UIUtils.createInfoLabel(dialogArea, UIDashboardMessages.dialog_dashboard_manager_infolabel_predifined_dashboard);

        this.updateButtons();
        return group;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(
            parent,
            IDialogConstants.CLOSE_ID,
            IDialogConstants.CLOSE_LABEL,
            true);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.CLOSE_ID) {
            setReturnCode(OK);
            close();
        }
    }

    private void updateButtons() {
        newButton.setEnabled(true);
        copyButton.setEnabled(selectedDashboard != null);
        editButton.setEnabled(selectedDashboard != null);
        deleteButton.setEnabled(selectedDashboard != null && selectedDashboard.isCustom());
    }

    private void createDashboard() {
        DashboardDescriptor newDashboard = new DashboardDescriptor("", "", "", "");
        DashboardEditDialog editDialog = new DashboardEditDialog(getShell(), newDashboard);
        if (editDialog.open() == IDialogConstants.OK_ID) {
            DashboardRegistry.getInstance().createDashboard(newDashboard);
            refreshDashboards();
        }
    }

    private void copyDashboard() {
        DashboardDescriptor newDashboard = new DashboardDescriptor(selectedDashboard);
        newDashboard.setCustom(true);
        String origId = newDashboard.getId();
        for (int i = 2; ; i++) {
            if (DashboardRegistry.getInstance().getDashboard(newDashboard.getId()) != null) {
                newDashboard.setId(origId + " " + i);
            } else {
                break;
            }
        }
        DashboardEditDialog editDialog = new DashboardEditDialog(getShell(), newDashboard);
        if (editDialog.open() == IDialogConstants.OK_ID) {
            DashboardRegistry.getInstance().createDashboard(newDashboard);
            refreshDashboards();
        }
    }

    private void editDashboard() {
        DashboardEditDialog editDialog = new DashboardEditDialog(getShell(), selectedDashboard);
        if (editDialog.open() == IDialogConstants.OK_ID) {
            DashboardRegistry.getInstance().saveSettings();
            refreshDashboards();
        }
    }

    private void deleteDashboard() {
        if (selectedDashboard == null || !selectedDashboard.isCustom()) {
            return;
        }
        if (UIUtils.confirmAction(
            getShell(),
            UIDashboardMessages.dialog_dashboard_manager_shell_delete_title,
            NLS.bind(UIDashboardMessages.dialog_dashboard_manager_shell_delete_question, selectedDashboard.getName())))
        {
            DashboardRegistry.getInstance().removeDashboard(selectedDashboard);
            selectedDashboard = null;
            refreshDashboards();
        }
    }

    private void refreshDashboards() {
        treeViewer.setInput(DashboardRegistry.getInstance().getAllSupportedSources());
        treeViewer.expandAll();
        updateButtons();
    }
}
