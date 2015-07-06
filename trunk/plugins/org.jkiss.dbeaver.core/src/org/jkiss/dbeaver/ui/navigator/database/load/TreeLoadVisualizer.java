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
package org.jkiss.dbeaver.ui.navigator.database.load;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;

/**
 * TreeLoadVisualizer
 */
public class TreeLoadVisualizer implements ILoadVisualizer<Object[]> {

    public static final Object[] EMPTY_ELEMENT_ARRAY = new Object[0];

    private DBNNode parent;
    private TreeLoadNode placeHolder;
    private AbstractTreeViewer viewer;

    public TreeLoadVisualizer(AbstractTreeViewer viewer, TreeLoadNode placeHolder, DBNNode parent)
    {
        this.viewer = viewer;
        this.placeHolder = placeHolder;
        this.parent = parent;
    }

    @Override
    public DBRProgressMonitor overwriteMonitor(DBRProgressMonitor monitor)
    {
        return monitor;
    }

    @Override
    public boolean isCompleted()
    {
        return placeHolder.isDisposed() || viewer.testFindItem(parent) == null;
    }

    @Override
    public void visualizeLoading()
    {
        viewer.refresh(placeHolder, true);
    }

    @Override
    public void completeLoading(Object[] children)
    {
        try {
            viewer.getControl().setRedraw(false);

            Widget widget = viewer.testFindItem(parent);
            if (widget != null && !widget.isDisposed()) {
                TreeItem item = (TreeItem) viewer.testFindItem(placeHolder);
                if (children == null) {
                    // Some error occurred. In good case children must be at least an empty array
                    viewer.collapseToLevel(parent, -1);
                } else if (children.length != 0) {
                    viewer.add(parent, children);
                }
                if (item != null && !item.isDisposed()) {
                    if (item.getParentItem() != null && !item.getParentItem().isDisposed() || this.parent instanceof IWorkspaceRoot) {
                        viewer.remove(placeHolder);
                    }
                }
            }
        }
        finally {
            placeHolder.dispose(parent);
            viewer.getControl().setRedraw(true);
        }
    }

    public static Object[] expandChildren(AbstractTreeViewer viewer, TreeLoadService service)
    {
        DBNNode parent = service.getParentNode();
        TreeLoadNode placeHolder = TreeLoadNode.createPlaceHolder(parent);
        if (placeHolder != null && TreeLoadNode.canBeginLoading(parent)) {
            TreeLoadVisualizer visualizer = new TreeLoadVisualizer(viewer, placeHolder, parent);
            RuntimeUtils.createService(service, visualizer).schedule();
            return new Object[]{placeHolder};
        }
        return EMPTY_ELEMENT_ARRAY;
    }

}
