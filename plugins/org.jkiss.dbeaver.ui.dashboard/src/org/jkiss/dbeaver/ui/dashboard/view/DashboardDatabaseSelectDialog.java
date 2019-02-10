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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * Database select dialog
 */
public class DashboardDatabaseSelectDialog extends BaseDialog {

    private static final String DIALOG_ID = "DBeaver.DashboardDatabaseSelectDialog";//$NON-NLS-1$

    private DBPNamedObject target;
    private TreeViewer treeViewer;

    public DashboardDatabaseSelectDialog(Shell shell) {
        super(shell, "Select dashboard database", null);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getSettingsSection(UIDashboardActivator.getDefault().getDialogSettings(), DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        {
            treeViewer = new FilteredTree(dialogArea, SWT.BORDER, new PatternFilter() {
                protected boolean isLeafMatch(Viewer viewer, Object element) {
                    if (element instanceof DBPNamedObject) {
                        return wordMatches(((DBPNamedObject) element).getName());
                    }
                    return false;
                }

            }, true).getViewer();
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            gd.widthHint = 300;
            treeViewer.getControl().setLayoutData(gd);
            treeViewer.getTree().setHeaderVisible(true);
            UIUtils.createTreeColumn(treeViewer.getTree(), SWT.LEFT, "Name");
            UIUtils.createTreeColumn(treeViewer.getTree(), SWT.LEFT, "Description");

            treeViewer.setContentProvider(new TreeContentProvider() {
                @Override
                public Object[] getChildren(Object parentElement) {
                    List<? extends DBPNamedObject> result = null;
                    if (parentElement instanceof List) {
                        result = (List) parentElement;
                    } else if (parentElement instanceof DBPDataSourceProviderDescriptor) {
                        result = ((DBPDataSourceProviderDescriptor) parentElement).getEnabledDrivers();
                        if (result.size() <= 1) {
                            result = null;
                        }
                    }
                    if (result == null) {
                        return new Object[0];
                    }
                    result.sort(DBUtils.nameComparator());
                    return result.toArray();
                }

                @Override
                public boolean hasChildren(Object element) {
                    if (element instanceof DBPDriver) {
                        return false;
                    }
                    return ((DBPDataSourceProviderDescriptor) element).getEnabledDrivers().size() > 1;
                }
            });
            treeViewer.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    DBPNamedObject element = (DBPNamedObject) cell.getElement();
                    if (cell.getColumnIndex() == 0) {
                        cell.setText(element.getName());
                        DBPImage icon = null;
                        if (element instanceof DBPDriver) {
                            icon = ((DBPDriver) element).getIcon();
                        } else if (element instanceof DBPDataSourceProviderDescriptor) {
                            icon = ((DBPDataSourceProviderDescriptor) element).getIcon();
                        }
                        if (icon != null) {
                            cell.setImage(DBeaverIcons.getImage(icon));
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
            treeViewer.setInput(DBWorkbench.getPlatform().getDataSourceProviderRegistry().getDataSourceProviders());

            treeViewer.addDoubleClickListener(event -> {
                if (target != null) {
                    okPressed();
                }
            });
            treeViewer.addSelectionChangedListener(event -> {
                this.target = null;
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    Object selectedObject = ((IStructuredSelection) selection).getFirstElement();
                    if (selectedObject instanceof DBPNamedObject) {
                        this.target = (DBPNamedObject) selectedObject;
                    }
                }
                this.updateButtons();
            });

            UIUtils.asyncExec(() -> {
                treeViewer.expandAll();
                UIUtils.packColumns(treeViewer.getTree(), true, null);
            });
        }

        return dialogArea;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        updateButtons();
        return contents;
    }

    private void updateButtons() {
        getButton(IDialogConstants.OK_ID).setEnabled(target != null);
    }

    public DBPNamedObject getTarget() {
        return target;
    }
}