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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.database.load.*;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseNavigatorContentProvider
 */
public class DatabaseNavigatorContentProvider implements IStructuredContentProvider, ITreeContentProvider {
    private static final Log log = Log.getLog(DatabaseNavigatorContentProvider.class);

    private static final Object[] EMPTY_CHILDREN = new Object[0];

    private final DatabaseNavigatorTree navigatorTree;
    private final boolean showRoot;

    public DatabaseNavigatorContentProvider(DatabaseNavigatorTree navigatorTree, boolean showRoot) {
        this.navigatorTree = navigatorTree;
        this.showRoot = showRoot;
    }

    @Override
    public Object[] getElements(Object parent) {
        if (parent instanceof DatabaseNavigatorContent content) {
            DBNNode rootNode = content.getRootNode();
            if (rootNode == null) {
                return EMPTY_CHILDREN;
            }
            if (showRoot) {
                return new Object[]{rootNode};
            } else {
                return getChildren(rootNode);
            }
        } else {
            return getChildren(parent);
        }
    }

    @Override
    public Object getParent(Object child) {
        if (child instanceof DBNLocalFolder node) {
            return node.getLogicalParent();
        } else if (child instanceof DBNNode node) {
            return node.getParentNode();
        } else if (child instanceof TreeNodeSpecial node) {
            return node.getParent();
        } else {
            return null;
        }
    }

    @Override
    public Object[] getChildren(final Object parent) {
        if (parent instanceof TreeNodeSpecial) {
            return EMPTY_CHILDREN;
        }
        if (!(parent instanceof DBNNode parentNode)) {
            return EMPTY_CHILDREN;
        }

        if (!parentNode.hasChildren(true)) {
            return EMPTY_CHILDREN;
        }
        if (parentNode instanceof DBNLazyNode && ((DBNLazyNode) parentNode).needsInitialization()) {
            return TreeLoadVisualizer.expandChildren(
                navigatorTree.getViewer(),
                new TreeLoadService("Loading", parentNode));
        } else {
            try {
                // Read children with null monitor cos' it's not a lazy node
                // and no blocking process will occur
                DBNNode[] children = DBNUtils.getNodeChildrenFiltered(
                    new VoidProgressMonitor(), parentNode, true);
                if (children == null) {
                    Throwable lastLoadError = parentNode.getLastLoadError();
                    if (lastLoadError != null) {
                        UIUtils.asyncExec(() -> DBWorkbench.getPlatformUI().showError(
                            "Error during node load",
                            CommonUtils.notEmpty(lastLoadError.getMessage()),
                            lastLoadError));
                    }
                    return EMPTY_CHILDREN;
                }
                return getFinalNodes(parentNode, children);
            } catch (Throwable ex) {
                // Collapse this item
                UIUtils.asyncExec(() -> {
                    DBWorkbench.getPlatformUI().showError(
                        "Navigator error",
                        ex.getMessage(),
                        ex);
                    navigatorTree.getViewer().collapseToLevel(parent, 1);
                    //navigatorTree.getViewer().refresh(parent);
                });
                return EMPTY_CHILDREN;
            }
        }
    }

    @Override
    public boolean hasChildren(Object parent) {
        if (parent instanceof DBNDatabaseNode) {
            if (navigatorTree.getNavigatorFilter() != null && navigatorTree.getNavigatorFilter().isLeafObject(parent)) {
                return false;
            }
            if (((DBNDatabaseNode) parent).getDataSourceContainer().getNavigatorSettings().isShowOnlyEntities()) {
                if (((DBNDatabaseNode) parent).getObject() instanceof DBSEntity) {
                    return false;
                }
            }
        }
        return parent instanceof DBNNode && ((DBNNode) parent).hasChildren(true);
    }

    @NotNull
    private static Object[] getFinalNodes(@NotNull DBNNode parent, @NotNull DBNNode[] children) {
        final int maxFetchSize = Math.max(
            NavigatorPreferences.MIN_LONG_LIST_FETCH_SIZE,
            DBWorkbench.getPlatform().getPreferenceStore().getInt(NavigatorPreferences.NAVIGATOR_LONG_LIST_FETCH_SIZE)
        );

        if (parent.isFiltered() || maxFetchSize < children.length) {
            final List<Object> nodes = new ArrayList<>(maxFetchSize);

            if (parent.isFiltered()) {
                nodes.add(new TreeNodeFilterConfigurator(parent));
            }

            if (maxFetchSize < children.length) {
                nodes.addAll(List.of(children).subList(0, maxFetchSize));
                nodes.add(new TreeNodeLazyExpander(parent, children, maxFetchSize));
            } else {
                nodes.addAll(List.of(children));
            }

            return nodes.toArray();
        } else if (children.length == 0) {
            return EMPTY_CHILDREN;
        } else {
            return children;
        }
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
