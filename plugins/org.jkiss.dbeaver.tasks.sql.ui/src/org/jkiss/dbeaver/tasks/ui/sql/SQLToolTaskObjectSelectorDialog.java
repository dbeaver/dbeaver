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
package org.jkiss.dbeaver.tasks.ui.sql;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.registry.task.TaskTypeDescriptor;
import org.jkiss.dbeaver.tasks.ui.sql.internal.TasksSQLUIMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;

import java.util.ArrayList;
import java.util.List;

class SQLToolTaskObjectSelectorDialog extends BaseDialog {

    private DBNProject projectNode;
    private TaskTypeDescriptor taskType;
    private DatabaseNavigatorTree dataSourceTree;
    private List<DBSObject> selectedObjects = new ArrayList<>();
    private static boolean showConnected;

    SQLToolTaskObjectSelectorDialog(Shell parentShell, DBNProject projectNode, TaskTypeDescriptor taskType) {
        super(parentShell, TasksSQLUIMessages.sql_tool_task_object_selector_dialog_title, null);
        this.projectNode = projectNode;
        this.taskType = taskType;
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
                return object instanceof DBNDatabaseItem && taskType.appliesTo(((DBNDatabaseItem) object).getObject());
            }
            @Override
            public boolean select(Object element) {
                if (element instanceof DBNProject || element instanceof DBNProjectDatabases) {
                    return true;
                }
                if (element instanceof DBNLocalFolder) {
                    for (DBNDataSource ds : ((DBNLocalFolder) element).getNestedDataSources()) {
                        if (taskType.isDriverApplicable(ds.getDataSourceContainer().getDriver()) &&
                            (!showConnected || ds.getDataSourceContainer().isConnected())) {
                            return true;
                        }
                    }
                    return false;
                }
                if (element instanceof DBNDataSource) {
                    if (showConnected && !((DBNDataSource) element).getDataSourceContainer().isConnected()) {
                        return false;
                    }
                    return taskType.isDriverApplicable(((DBNDataSource) element).getDataSourceContainer().getDriver());
                }
                if (element instanceof DBNDatabaseItem) {
                    DBSObject object = ((DBNDatabaseItem) element).getObject();
                    return (DBSObjectContainer.class.isAssignableFrom(object.getClass()) ||
                        (taskType.matchesEntityElements() && DBSEntity.class.isAssignableFrom(object.getClass())) ||
                        taskType.appliesTo(object));
                } else if (element instanceof DBNDatabaseFolder) {
                    Class<? extends DBSObject> childrenClass = ((DBNDatabaseFolder) element).getChildrenClass();
                    return childrenClass != null &&
                        (DBSObjectContainer.class.isAssignableFrom(childrenClass) ||
                            (taskType.matchesEntityElements() && DBSEntity.class.isAssignableFrom(childrenClass)) ||
                            taskType.matchesType(childrenClass));
                }
                return element instanceof TreeNodeSpecial;
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
            updateSelectedObjects();
        });

        final Button showConnectedCheck = new Button(dialogArea, SWT.CHECK);
        showConnectedCheck.setText(UINavigatorMessages.label_show_connected);
        showConnectedCheck.setSelection(showConnected);
        showConnectedCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                showConnected = showConnectedCheck.getSelection();
                dataSourceTree.getViewer().refresh();
            }
        });

        return dialogArea;
    }

    private void updateSelectedObjects() {
        selectedObjects.clear();
        for (Object element : dataSourceTree.getCheckboxViewer().getCheckedElements()) {
            if (element instanceof DBNDatabaseItem && taskType.matchesType(((DBNDatabaseItem) element).getObject().getClass())) {
                selectedObjects.add(((DBNDatabaseItem) element).getObject());
            }
        }
        getButton(IDialogConstants.OK_ID).setEnabled(!selectedObjects.isEmpty());
    }

    public List<DBSObject> getSelectedObjects() {
        return selectedObjects;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

}
