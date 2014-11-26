/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.views.navigator.database;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class CheckboxTreeManager implements ICheckStateListener {

    private final CheckboxTreeViewer viewer;
    private final Class<?>[] targetTypes;

    public CheckboxTreeManager(CheckboxTreeViewer viewer, Class<?>[] targetTypes) {
        this.viewer = viewer;
        this.targetTypes = targetTypes;
    }

    @Override
    public void checkStateChanged(CheckStateChangedEvent event) {
        try {
            VoidProgressMonitor monitor = VoidProgressMonitor.INSTANCE;

            List<DBNDatabaseNode> targetChildren = new ArrayList<DBNDatabaseNode>();
            List<DBNDatabaseNode> targetContainers = new ArrayList<DBNDatabaseNode>();
            collectChildren(monitor, event.getElement(), targetChildren, targetContainers);
            for (DBNDatabaseNode child : targetChildren) {
                viewer.setChecked(child, event.getChecked());
            }
            for (DBNDatabaseNode container : targetContainers) {
                viewer.setChecked(container, event.getChecked());
                if (event.getChecked()) {
                    boolean missing = false;
                    List<DBNDatabaseNode> directChildren = container.getChildren(monitor);
                    if (directChildren != null) {
                        for (DBNDatabaseNode node : directChildren) {
                            if (!targetChildren.contains(node)) {
                                missing = true;
                                break;
                            }
                        }
                    }
                    viewer.setGrayed(container, missing);
                } else {
                    viewer.setGrayed(container, false);
                }
            }
        } catch (DBException e) {
            UIUtils.showErrorDialog(viewer.getControl().getShell(), "Error", "Can't collect child nodes", e);
        }
    }

    private boolean collectChildren(DBRProgressMonitor monitor, final Object element, List<DBNDatabaseNode> targetChildren, List<DBNDatabaseNode> targetContainers) throws DBException {
        if (element instanceof DBNDatabaseNode) {
            for (Class<?> type : targetTypes) {
                if (type.isInstance(((DBNDatabaseNode) element).getObject())) {
                    targetChildren.add((DBNDatabaseNode) element);
                    return true;
                }
            }
            ((DBNDatabaseNode) element).initializeNode(monitor, null);
            List<DBNDatabaseNode> children = ((DBNDatabaseNode) element).getChildren(monitor);
            if (!CommonUtils.isEmpty(children)) {
                boolean foundChild = false;
                for (DBNDatabaseNode child : children) {
                    if (collectChildren(monitor, child, targetChildren, targetContainers)) {
                        foundChild = true;
                    }
                }
                if (foundChild) {
                    targetContainers.add((DBNDatabaseNode) element);
                }
                return foundChild;
            }
        }
        return false;
    }


}
