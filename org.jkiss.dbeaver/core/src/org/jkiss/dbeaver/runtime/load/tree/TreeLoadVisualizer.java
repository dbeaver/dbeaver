/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load.tree;

import org.jkiss.dbeaver.runtime.load.*;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.core.resources.IWorkspaceRoot;

/**
 * TreeLoadVisualizer
 */
public class TreeLoadVisualizer implements ILoadVisualizer<Object[]> {

    public static final Object[] EMPTY_ELEMENT_ARRAY = new Object[0];

    private Object parent;
    private TreeLoadNode placeHolder;
    private AbstractTreeViewer viewer;

    public TreeLoadVisualizer(AbstractTreeViewer viewer, TreeLoadNode placeHolder, Object parent)
    {
        this.viewer = viewer;
        this.placeHolder = placeHolder;
        this.parent = parent;
    }

    public boolean isCompleted()
    {
        return placeHolder.isDisposed() || viewer.testFindItem(parent) == null;
    }

    public void visualizeLoading()
    {
        viewer.refresh(placeHolder, true);
    }

    public void completeLoading(Object[] children)
    {
        try {
            viewer.getControl().setRedraw(false);
            //parent = parent instanceof ConnectionInfo ? ((ConnectionInfo)parent).getConnectionProfile() : parent;
            Widget widget = viewer.testFindItem(parent);
            if (widget != null && !widget.isDisposed()) {
                TreeItem item = (TreeItem) viewer.testFindItem(placeHolder);
                if (children != null && children.length != 0) {
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
        Object parent = service.getParent();
        if (!(parent instanceof TreeLoadNode)) {
            TreeLoadNode placeHolder = TreeLoadNode.createPlaceHolder(parent);
            if (placeHolder != null && TreeLoadNode.canBeginLoading(parent)) {
                TreeLoadVisualizer visualizer = new TreeLoadVisualizer(viewer, placeHolder, parent);
                LoadingUtils.executeService(service, visualizer);
                return new Object[]{placeHolder};
            }
        }
        return EMPTY_ELEMENT_ARRAY;
    }

}
