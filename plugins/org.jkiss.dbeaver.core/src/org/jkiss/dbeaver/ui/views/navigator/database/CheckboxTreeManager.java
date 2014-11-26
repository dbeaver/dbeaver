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
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
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
            Object element = event.getElement();
            boolean checked = event.getChecked();

            updateElementHierarchy(monitor, element, checked, true);

            if (element instanceof DBNDatabaseNode) {
                for (DBNNode node = ((DBNDatabaseNode)element).getParentNode(); node != null; node = node.getParentNode()) {
                    if (node instanceof DBNDatabaseNode) {
                        updateElementHierarchy(monitor, node, checked, false);
                    }
                    if (node instanceof DBNDataSource) {
                        break;
                    }
                }
            }

        } catch (DBException e) {
            UIUtils.showErrorDialog(viewer.getControl().getShell(), "Error", "Can't collect child nodes", e);
        }
    }

    private void updateElementHierarchy(VoidProgressMonitor monitor, Object element, boolean checked, boolean change) throws DBException {
        List<DBNDatabaseNode> targetChildren = new ArrayList<DBNDatabaseNode>();
        List<DBNDatabaseNode> targetContainers = new ArrayList<DBNDatabaseNode>();
        collectChildren(monitor, element, targetChildren, targetContainers, !change);
        if (change) {
            for (DBNDatabaseNode child : targetChildren) {
                viewer.setChecked(child, checked);
            }
        }
        for (DBNDatabaseNode container : change ? targetContainers : Collections.singletonList((DBNDatabaseNode) element)) {
            List<DBNDatabaseNode> directChildren = CommonUtils.safeList(container.getChildren(monitor));
            boolean missing = Collections.disjoint(directChildren, targetChildren);

            viewer.setChecked(container, change ? checked : !missing || !Collections.disjoint(directChildren, targetContainers));
            viewer.setGrayed(container, missing);
        }
    }

    private boolean collectChildren(DBRProgressMonitor monitor, final Object element, List<DBNDatabaseNode> targetChildren, List<DBNDatabaseNode> targetContainers, boolean onlyChecked) throws DBException {
        if (element instanceof DBNDatabaseNode) {
            for (Class<?> type : targetTypes) {
                if (type.isInstance(((DBNDatabaseNode) element).getObject())) {
                    if (!onlyChecked || viewer.getChecked(element)) {
                        targetChildren.add((DBNDatabaseNode) element);
                    }
                    return true;
                }
            }
            ((DBNDatabaseNode) element).initializeNode(monitor, null);
            List<DBNDatabaseNode> children = ((DBNDatabaseNode) element).getChildren(monitor);
            if (!CommonUtils.isEmpty(children)) {
                boolean foundChild = false;
                for (DBNDatabaseNode child : children) {
                    if (collectChildren(monitor, child, targetChildren, targetContainers, onlyChecked)) {
                        foundChild = true;
                    }
                }
                if (foundChild) {
                    if (!onlyChecked || viewer.getChecked(element)) {
                        targetContainers.add((DBNDatabaseNode) element);
                    }
                }
                return foundChild;
            }
        }
        return false;
    }


}
