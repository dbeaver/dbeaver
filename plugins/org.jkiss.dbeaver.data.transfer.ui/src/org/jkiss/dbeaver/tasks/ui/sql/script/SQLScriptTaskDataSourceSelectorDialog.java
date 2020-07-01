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
package org.jkiss.dbeaver.tasks.ui.sql.script;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IToolTipProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class SQLScriptTaskDataSourceSelectorDialog extends BaseDialog {

    private DBNProject projectNode;
    private DatabaseNavigatorTree dataSourceTree;
    private List<DBNDataSource> selectedDataSources = new ArrayList<>();

    SQLScriptTaskDataSourceSelectorDialog(Shell parentShell, DBNProject projectNode) {
        super(parentShell, DTMessages.sql_script_task_page_settings_group_connections, null);
        this.projectNode = projectNode;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        INavigatorFilter dsFilter = new INavigatorFilter() {
            @Override
            public boolean filterFolders() {
                return true;
            }
            @Override
            public boolean isLeafObject(Object object) {
                return object instanceof DBNDataSource;
            }
            @Override
            public boolean select(Object element) {
                return element instanceof DBNProject || element instanceof DBNProjectDatabases || element instanceof DBNLocalFolder || element instanceof DBNDataSource || element instanceof TreeNodeSpecial;
            }
        };

        dataSourceTree = new DatabaseNavigatorTree(
            dialogArea,
            projectNode.getDatabases(),
            SWT.SINGLE | SWT.BORDER | SWT.CHECK,
            false,
            dsFilter);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.widthHint = 400;
        dataSourceTree.setLayoutData(gd);
        dataSourceTree.getViewer().addSelectionChangedListener(event -> {
            updateSelectedDataSources();
        });

        return dialogArea;
    }

    private void updateSelectedDataSources() {
        selectedDataSources.clear();
        for (Object element : dataSourceTree.getCheckboxViewer().getCheckedElements()) {
            if (element instanceof DBNDataSource) {
                selectedDataSources.add((DBNDataSource) element);
            }
        }
        getButton(IDialogConstants.OK_ID).setEnabled(!selectedDataSources.isEmpty());
    }

    public List<DBNDataSource> getSelectedDataSources() {
        return selectedDataSources;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    static void createScriptColumns(ColumnViewer viewer) {
        final ILabelProvider mainLabelProvider = (ILabelProvider) viewer.getLabelProvider();
        ViewerColumnController columnController = new ViewerColumnController("sqlTaskScriptViewer", viewer);
        columnController.setForceAutoSize(true);
        columnController.addColumn(ModelMessages.model_navigator_Name, DTUIMessages.sql_script_task_data_source_selection_dialog_column_description_script, SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return mainLabelProvider.getText(element);
            }
            @Override
            public Image getImage(Object element) {
                return mainLabelProvider.getImage(element);
            }
            @Override
            public String getToolTipText(Object element) {
                if (mainLabelProvider instanceof IToolTipProvider) {
                    return ((IToolTipProvider) mainLabelProvider).getToolTipText(element);
                }
                return null;
            }
        });

        columnController.addColumn(ModelMessages.model_navigator_Connection, DTUIMessages.sql_script_task_data_source_selection_dialog_column_description_script_data_source, SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof DBNResource) {
                    Collection<DBPDataSourceContainer> containers = ((DBNResource) element).getAssociatedDataSources();
                    if (!CommonUtils.isEmpty(containers)) {
                        StringBuilder text = new StringBuilder();
                        for (DBPDataSourceContainer container : containers) {
                            if (text.length() > 0) {
                                text.append(", ");
                            }
                            text.append(container.getName());
                        }
                        return text.toString();
                    }
                }
                return "";
            }

            @Override
            public Image getImage(Object element) {
                return null;
            }
        });
        columnController.createColumns(true);
    }

}
