/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.navigator.database.load;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.LoadingJob;

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
        final Control viewerControl = viewer.getControl();
        if (viewerControl.isDisposed()) {
            return;
        }
        try {
            viewerControl.setRedraw(false);

            {
                TreeItem item = (TreeItem) viewer.testFindItem(placeHolder);
                if (children == null) {
                    // Some error occurred. In good case children must be at least an empty array
                    viewer.collapseToLevel(parent, -1);
                } else if (children.length != 0) {
                    viewer.add(parent, children);
                }
                if (item != null && !item.isDisposed()) {
                    if ((item.getParentItem() == null || !item.getParentItem().isDisposed()) || this.parent instanceof IWorkspaceRoot) {
                        viewer.remove(placeHolder);
                    }
                }
            }
        }
        finally {
            placeHolder.dispose(parent);
            if (!viewerControl.isDisposed()) {
                viewerControl.setRedraw(true);
            }
        }
    }

    public static Object[] expandChildren(AbstractTreeViewer viewer, TreeLoadService service)
    {
        DBNNode parent = service.getParentNode();
        TreeLoadNode placeHolder = TreeLoadNode.createPlaceHolder(parent);
        if (placeHolder != null && TreeLoadNode.canBeginLoading(parent)) {
            TreeLoadVisualizer visualizer = new TreeLoadVisualizer(viewer, placeHolder, parent);
            LoadingJob.createService(service, visualizer).schedule();
            return new Object[]{placeHolder};
        }
        return EMPTY_ELEMENT_ARRAY;
    }

}
