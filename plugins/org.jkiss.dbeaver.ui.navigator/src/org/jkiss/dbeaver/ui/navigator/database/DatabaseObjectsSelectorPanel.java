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

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DatabaseObjectsSelectorPanel extends Composite {

    private final DBPProject project;
    private DatabaseNavigatorTree dataSourceTree;
    private DatabaseObjectsTreeManager checkboxTreeManager;
    private DBRRunnableContext runnableContext;

    public DatabaseObjectsSelectorPanel(Composite parent, boolean multiSelector, DBRRunnableContext runnableContext) {
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
        project = NavigatorUtils.getSelectedProject();
        final DBNProject projectNode = platform.getNavigatorModel().getRoot().getProjectNode(project);
        DBNNode rootNode = projectNode == null ? platform.getNavigatorModel().getRoot() : projectNode.getDatabases();
        dataSourceTree = new DatabaseNavigatorTree(this, rootNode, SWT.SINGLE | SWT.BORDER | (multiSelector ? SWT.CHECK : SWT.NONE));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        dataSourceTree.setLayoutData(gd);
        dataSourceTree.getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof TreeNodeSpecial) {
                    return true;
                }
                if (element instanceof DBNNode) {
                    if (element instanceof DBNDatabaseFolder) {
                        DBNDatabaseFolder folder = (DBNDatabaseFolder) element;
                        return isDatabaseFolderVisible(folder);
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
                        return isDatabaseObjectVisible(((DBSWrapper) element).getObject());
                    }
                }
                return false;
            }
        });
        if (multiSelector) {
            final CheckboxTreeViewer viewer = dataSourceTree.getCheckboxViewer();

            checkboxTreeManager = new DatabaseObjectsTreeManager(runnableContext, viewer,
                new Class[]{DBSDataContainer.class});
            viewer.addCheckStateListener(event -> onSelectionChange(event.getElement()));
        } else {
            dataSourceTree.getViewer().addSelectionChangedListener(event -> onSelectionChange(
                ((IStructuredSelection)event.getSelection()).getFirstElement()));
        }
    }

    public void setNavigatorFilter(INavigatorFilter navigatorFilter) {
        dataSourceTree.setNavigatorFilter(navigatorFilter);
    }

    public DBPProject getProject() {
        return project;
    }

    public void setSelection(List<DBNNode> nodes) {
//        for (DBNNode node : nodes) {
//            dataSourceTree.getViewer().reveal(node);
//        }
        dataSourceTree.getViewer().setSelection(
            new StructuredSelection(nodes), true);
    }

    public void checkNodes(Collection<DBNNode> nodes, boolean revealAll) {
        TreeViewer treeViewer = dataSourceTree.getViewer();
        boolean first = true;
        for (DBNNode node : nodes) {
            if (revealAll) {
                treeViewer.reveal(node);
            } else if (first) {
                DBNDataSource dsNode = DBNDataSource.getDataSourceNode(node);
                if (dsNode != null) {
                    treeViewer.reveal(dsNode);
                }
                first = false;
            }
            if (treeViewer instanceof CheckboxTreeViewer) {
                ((CheckboxTreeViewer) treeViewer).setChecked(node, true);
            }
        }
        if (treeViewer instanceof CheckboxTreeViewer) {
            checkboxTreeManager.updateCheckStates();
        }
    }

    public boolean hasCheckedNodes() {
        for (Object element : dataSourceTree.getCheckboxViewer().getCheckedElements()) {
            if (element instanceof DBNNode) {
                return true;
            }
        }
        return false;
    }

    public List<DBNNode> getCheckedNodes() {
        Object[] checkedElements = dataSourceTree.getCheckboxViewer().getCheckedElements();
        List<DBNNode> result = new ArrayList<>(checkedElements.length);
        for (Object element : checkedElements) {
            if (element instanceof DBNNode) {
                result.add((DBNNode) element);
            }
        }
        return result;
    }

    protected boolean isDatabaseFolderVisible(DBNDatabaseFolder folder) {
        Class<? extends DBSObject> folderItemsClass = folder.getChildrenClass();
        return folderItemsClass != null &&
            (DBSObjectContainer.class.isAssignableFrom(folderItemsClass) ||
                DBSEntity.class.isAssignableFrom(folderItemsClass));
    }

    protected boolean isDatabaseObjectVisible(DBSObject obj) {
        return obj instanceof DBSObjectContainer || obj instanceof DBSDataContainer && obj instanceof DBSEntity;
    }

    protected boolean isDataSourceVisible(DBNDataSource dataSource) {
        return true;
    }

    protected boolean isFolderVisible(DBNLocalFolder folder) {
        return true;
    }

    protected void onSelectionChange(Object element) {

    }

    public void refreshNodes() {
        dataSourceTree.getViewer().refresh();
    }

    public void addSelectionListener(ISelectionChangedListener listener) {
        dataSourceTree.getViewer().addSelectionChangedListener(listener);
    }

    public ISelection getSelection() {
        return dataSourceTree.getViewer().getSelection();
    }
}
