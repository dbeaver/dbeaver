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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.registry.task.TaskTypeDescriptor;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;

import java.util.ArrayList;
import java.util.List;

class SQLToolTaskObjectSelectorDialog extends BaseDialog {

    private DBNProject projectNode;
    private TaskTypeDescriptor taskType;
    private DatabaseNavigatorTree dataSourceTree;
    private List<DBSObject> selectedObjects = new ArrayList<>();

    SQLToolTaskObjectSelectorDialog(Shell parentShell, DBNProject projectNode, TaskTypeDescriptor taskType) {
        super(parentShell, "Select input objects", null);
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
                if (element instanceof DBNProject || element instanceof DBNProjectDatabases || element instanceof DBNLocalFolder || element instanceof DBNDataSource) {
                    return true;
                }
                if (element instanceof DBNDatabaseItem) {
                    return (DBSObjectContainer.class.isAssignableFrom(((DBNDatabaseItem) element).getObject().getClass()) ||
                        taskType.appliesTo(((DBNDatabaseItem) element).getObject()));
                } else if (element instanceof DBNDatabaseFolder) {
                    Class<? extends DBSObject> childrenClass = ((DBNDatabaseFolder) element).getChildrenClass();
                    return childrenClass != null && (DBSObjectContainer.class.isAssignableFrom(childrenClass) || taskType.matchesType(childrenClass));
                }
                return false;
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

        return dialogArea;
    }

    private void updateSelectedObjects() {
        selectedObjects.clear();
        for (Object element : dataSourceTree.getCheckboxViewer().getCheckedElements()) {
            if (element instanceof DBNDatabaseItem) {
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
