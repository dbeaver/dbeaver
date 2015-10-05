/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeLoadNode;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeLoadService;
import org.jkiss.dbeaver.ui.navigator.database.load.TreeLoadVisualizer;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * DatabaseNavigatorContentProvider
*/
class DatabaseNavigatorContentProvider implements IStructuredContentProvider, ITreeContentProvider {
    static final Log log = Log.getLog(DatabaseNavigatorContentProvider.class);

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
            if (showRoot) {
                return new Object[] { ((DatabaseNavigatorContent) parent).getRootNode() };
            } else {
                return getChildren(((DatabaseNavigatorContent) parent).getRootNode());
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
        } else if (child instanceof TreeLoadNode) {
            return ((TreeLoadNode)child).getParent();
        } else {
            log.warn("Unknown node type: " + child);
            return null;
        }
    }

    @Override
    public Object[] getChildren(final Object parent)
    {
        if (parent instanceof TreeLoadNode) {
            return null;
        }
        if (!(parent instanceof DBNNode)) {
            log.error("Bad parent type: " + parent);
            return null;
        }
        final DBNNode parentNode = (DBNNode)parent;//view.getNavigatorModel().findNode(parent);
/*
        if (parentNode == null) {
            log.error("Can't find parent node '" + ((DBSObject) parent).getName() + "' in model");
            return EMPTY_CHILDREN;
        }
*/
        if (!parentNode.allowsNavigableChildren()) {
            return EMPTY_CHILDREN;
        }
        if (parentNode instanceof DBNDatabaseNode && ((DBNDatabaseNode)parentNode).needsInitialization()) {
            if (navigatorTree.isFiltering()) {
                return EMPTY_CHILDREN;
            }
            return TreeLoadVisualizer.expandChildren(
                navigatorTree.getViewer(),
                new TreeLoadService("Loading", ((DBNDatabaseNode) parentNode)));
        } else {
            try {
                // Read children with null monitor cos' it's not a lazy node
                // and no blocking process will occur
                List<? extends DBNNode> children = TreeLoadService.filterNavigableChildren(
                    parentNode.getChildren(VoidProgressMonitor.INSTANCE));
                if (CommonUtils.isEmpty(children)) {
                    return EMPTY_CHILDREN;
                } else {
                    return children.toArray(new Object[children.size()]);
                }
            }
            catch (Throwable ex) {
                UIUtils.showErrorDialog(
                    navigatorTree.getViewer().getControl().getShell(),
                    "Navigator error",
                    ex.getMessage(),
                    ex);
                // Collapse this item
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run()
                    {
                        navigatorTree.getViewer().collapseToLevel(parent, 1);
                        navigatorTree.getViewer().refresh(parent);
                    }
                });
                return EMPTY_CHILDREN;
            }
        }
    }

    @Override
    public boolean hasChildren(Object parent)
    {
        return parent instanceof DBNNode && ((DBNNode) parent).allowsNavigableChildren();
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
