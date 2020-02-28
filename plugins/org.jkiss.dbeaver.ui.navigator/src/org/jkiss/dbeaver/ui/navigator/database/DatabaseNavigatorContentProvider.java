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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeLoadService;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeLoadVisualizer;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeLazyExpander;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeNodeSpecial;
import org.jkiss.utils.ArrayUtils;

/**
 * DatabaseNavigatorContentProvider
*/
class DatabaseNavigatorContentProvider implements IStructuredContentProvider, ITreeContentProvider {
    private static final Log log = Log.getLog(DatabaseNavigatorContentProvider.class);

    private static final Object[] EMPTY_CHILDREN = new Object[0];

    private DatabaseNavigatorTree navigatorTree;
    private boolean showRoot;

    DatabaseNavigatorContentProvider(DatabaseNavigatorTree navigatorTree, boolean showRoot)
    {
        this.navigatorTree = navigatorTree;
        this.showRoot = showRoot;
    }

    @Override
    public void inputChanged(Viewer v, Object oldInput, Object newInput)
    {
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public Object[] getElements(Object parent)
    {
        if (parent instanceof DatabaseNavigatorContent) {
            DBNNode rootNode = ((DatabaseNavigatorContent) parent).getRootNode();
            if (rootNode == null) {
                return EMPTY_CHILDREN;
            }
            if (showRoot) {
                return new Object[] {rootNode};
            } else {
                return getChildren(rootNode);
            }
        } else {
            return getChildren(parent);
        }
    }

    @Override
    public Object getParent(Object child)
    {
        if (child instanceof DBNNode) {
            return ((DBNNode)child).getParentNode();
        } else if (child instanceof TreeNodeSpecial) {
            return ((TreeNodeSpecial)child).getParent();
        } else {
            return null;
        }
    }

    @Override
    public Object[] getChildren(final Object parent)
    {
        if (parent instanceof TreeNodeSpecial) {
            return EMPTY_CHILDREN;
        }
        if (!(parent instanceof DBNNode)) {
            return EMPTY_CHILDREN;
        }
        final DBNNode parentNode = (DBNNode)parent;//view.getNavigatorModel().findNode(parent);
/*
        if (parentNode == null) {
            log.error("Can't find parent node '" + ((DBSObject) parent).getName() + "' in model");
            return EMPTY_CHILDREN;
        }
*/
        if (!parentNode.hasChildren(true)) {
            return EMPTY_CHILDREN;
        }
        if (parentNode instanceof DBNDatabaseNode && ((DBNDatabaseNode)parentNode).needsInitialization()) {
            return TreeLoadVisualizer.expandChildren(
                navigatorTree.getViewer(),
                new TreeLoadService("Loading", ((DBNDatabaseNode) parentNode)));
        } else {
            try {
                // Read children with null monitor cos' it's not a lazy node
                // and no blocking process will occur
                DBNNode[] children = DBNUtils.getNodeChildrenFiltered(
                    new VoidProgressMonitor(), parentNode, true);
                if (ArrayUtils.isEmpty(children)) {
                    return EMPTY_CHILDREN;
                } else {
                    int longListFetchSize = Math.max(NavigatorPreferences.MIN_LONG_LIST_FETCH_SIZE, DBWorkbench.getPlatform().getPreferenceStore().getInt(NavigatorPreferences.NAVIGATOR_LONG_LIST_FETCH_SIZE));
                    if (children.length > longListFetchSize) {
                        Object[] curChildren = new Object[longListFetchSize + 1];
                        System.arraycopy(children, 0, curChildren, 0, longListFetchSize);
                        curChildren[longListFetchSize] = new TreeNodeLazyExpander(parentNode, children, longListFetchSize);
                        return curChildren;
                    }
                    return children;
                }
            }
            catch (Throwable ex) {
                DBWorkbench.getPlatformUI().showError(
                        "Navigator error",
                    ex.getMessage(),
                    ex);
                // Collapse this item
                UIUtils.asyncExec(() -> {
                    navigatorTree.getViewer().collapseToLevel(parent, 1);
                    navigatorTree.getViewer().refresh(parent);
                });
                return EMPTY_CHILDREN;
            }
        }
    }

    @Override
    public boolean hasChildren(Object parent)
    {
        if (parent instanceof DBNDatabaseNode) {
            if (navigatorTree.getNavigatorFilter() != null && navigatorTree.getNavigatorFilter().isLeafObject(parent)) {
                return false;
            }
        }
        return parent instanceof DBNNode && ((DBNNode) parent).hasChildren(true);
    }

/*
    public void cancelLoading(Object parent)
    {
        if (!(parent instanceof DBSObject)) {
            log.error("Bad parent type: " + parent);
        }
        DBSObject object = (DBSObject)parent;
        object.getDataSource().cancelCurrentOperation();
    }
*/

}
