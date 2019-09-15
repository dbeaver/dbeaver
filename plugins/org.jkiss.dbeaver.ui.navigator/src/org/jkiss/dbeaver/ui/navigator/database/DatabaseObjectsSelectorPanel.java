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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DatabaseObjectsSelectorPanel extends Composite {

    private DatabaseNavigatorTree dataSourceTree;
    private DatabaseObjectsTreeManager checkboxTreeManager;
    private DBRRunnableContext runnableContext;

    public DatabaseObjectsSelectorPanel(Composite parent, DBRRunnableContext runnableContext) {
        super(parent, SWT.NONE);
        if (parent.getLayout() instanceof GridLayout) {
            setLayoutData(new GridData(GridData.FILL_BOTH));
        }
        GridLayout gl = new GridLayout(1, true);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        setLayout(gl);
        this.runnableContext = runnableContext;

        DBPPlatform platform = DBWorkbench.getPlatform();
        final DBNProject projectNode = platform.getNavigatorModel().getRoot().getProjectNode(NavigatorUtils.getSelectedProject());
        DBNNode rootNode = projectNode == null ? platform.getNavigatorModel().getRoot() : projectNode.getDatabases();
        dataSourceTree = new DatabaseNavigatorTree(this, rootNode, SWT.SINGLE | SWT.CHECK | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        dataSourceTree.setLayoutData(gd);
        final CheckboxTreeViewer viewer = (CheckboxTreeViewer) dataSourceTree.getViewer();
        viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof TreeNodeSpecial) {
                    return true;
                }
                if (element instanceof DBNNode) {
                    if (element instanceof DBNDatabaseFolder) {
                        DBNDatabaseFolder folder = (DBNDatabaseFolder) element;
                        Class<? extends DBSObject> folderItemsClass = folder.getChildrenClass();
                        return folderItemsClass != null &&
                            (DBSObjectContainer.class.isAssignableFrom(folderItemsClass) ||
                                DBSEntity.class.isAssignableFrom(folderItemsClass));
                    }
                    if (element instanceof DBNProjectDatabases) {
                        return true;
                    }
                    if (element instanceof DBNLocalFolder) {
                        return isFolderVisible((DBNLocalFolder)element);
                    } else if (element instanceof DBNDataSource) {
                        return isDataSourceVisible((DBNDataSource)element);
                    }
                    if (element instanceof DBSWrapper) {
                        return isObjectVisible(((DBSWrapper) element).getObject());
                    }
                }
                return false;
            }
        });
        checkboxTreeManager = new DatabaseObjectsTreeManager(runnableContext, viewer,
            new Class[]{DBSDataContainer.class});
        viewer.addCheckStateListener(event -> onSelectionChange());
    }

    public void setSelection(Collection<DBNNode> nodes) {
        dataSourceTree.getViewer().setSelection(
            new StructuredSelection(nodes), true);
    }

    public void checkNodes(Collection<DBNNode> nodes) {
        boolean first = true;
        for (DBNNode node : nodes) {
            ((CheckboxTreeViewer) dataSourceTree.getViewer()).setChecked(node, true);
            if (first) {
                DBNDataSource dsNode = DBNDataSource.getDataSourceNode(node);
                if (dsNode != null) {
                    dataSourceTree.getViewer().reveal(dsNode);
                }
                first = false;
            }
        }
        checkboxTreeManager.updateCheckStates();
    }

    public boolean hasCheckedNodes() {
        for (Object element : ((CheckboxTreeViewer) dataSourceTree.getViewer()).getCheckedElements()) {
            if (element instanceof DBNNode) {
                return true;
            }
        }
        return false;
    }

    public List<DBNNode> getCheckedNodes() {
        Object[] checkedElements = ((CheckboxTreeViewer) dataSourceTree.getViewer()).getCheckedElements();
        List<DBNNode> result = new ArrayList<>(checkedElements.length);
        for (Object element : checkedElements) {
            if (element instanceof DBNNode) {
                result.add((DBNNode) element);
            }
        }
        return result;
    }

    protected boolean isObjectVisible(DBSObject obj) {
        return obj instanceof DBSObjectContainer || obj instanceof DBSDataContainer && obj instanceof DBSEntity;
    }

    protected boolean isDataSourceVisible(DBNDataSource dataSource) {
        return true;
    }

    protected boolean isFolderVisible(DBNLocalFolder folder) {
        return true;
    }

    protected void onSelectionChange() {

    }


}
