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

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ViewerFilter;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DatabaseObjectsTreeManager implements ICheckStateListener {

    private static final Log log = Log.getLog(DatabaseObjectsTreeManager.class);

    private final DBRRunnableContext runnableContext;
    private final CheckboxTreeViewer viewer;
    private final Class<?>[] targetTypes;
    private IdentityHashMap<Object, Boolean> checkedElements = new IdentityHashMap<>();
    private final ViewerFilter[] filters;

    private static class CollectInfo {
        DBNDatabaseNode rootElement;
        boolean wasChecked;
        final List<DBNDatabaseNode> targetChildren = new ArrayList<>();
        final List<DBNDatabaseNode> targetContainers = new ArrayList<>();
    }

    public DatabaseObjectsTreeManager(DBRRunnableContext runnableContext, CheckboxTreeViewer viewer, Class<?>[] targetTypes) {
        this.runnableContext = runnableContext;
        this.viewer = viewer;
        this.targetTypes = targetTypes;
        this.filters = viewer.getFilters();
        viewer.addCheckStateListener(this);
    }

    @Override
    public void checkStateChanged(final CheckStateChangedEvent event) {
        updateElementsCheck(
            new Object[] {event.getElement()},
            event.getChecked(),
            true);
    }

    private void updateElementsCheck(final Object[] elements, final boolean checked, final boolean change) {
        checkedElements.clear();
        try {
            runnableContext.run(false, true, (monitor -> {
                monitor.beginTask("Load sources tree", 100 * elements.length);
                try {
                    for (Object element : elements) {
                        if (!(element instanceof DBNDatabaseNode)) {
                            continue;
                        }
                        DBNDatabaseNode node = (DBNDatabaseNode)element;
                        monitor.subTask("Search in '" + node.getName() + "'");
                        CollectInfo collectInfo = new CollectInfo();
                        collectInfo.rootElement = node;
                        collectInfo.wasChecked = checked;
                        updateElementHierarchy(monitor, node, collectInfo, change);

                        if (change) {
                            // Update parent state
                            for (DBNNode parent = ((DBNDatabaseNode) element).getParentNode(); parent != null; parent = parent.getParentNode()) {
                                if (parent instanceof DBNDatabaseNode) {
                                    updateElementHierarchy(monitor, (DBNDatabaseNode) parent, collectInfo, false);
                                }
                                if (parent instanceof DBNDataSource) {
                                    break;
                                }
                            }
                        }
                        monitor.worked(1);
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }));
        } catch (InvocationTargetException e) {
            log.error("Error updating checkbox state", e.getTargetException());
            //UIUtils.showErrorDialog(viewer.getControl().getShell(), "Error", "Can't collect child nodes", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }

        for (Object ce : viewer.getCheckedElements()) {
            checkedElements.put(ce, Boolean.TRUE);
        }
    }

    private void updateElementHierarchy(final DBRProgressMonitor monitor, final DBNDatabaseNode element, final CollectInfo collectInfo, final boolean change) throws DBException {
        try {
            collectChildren(monitor, element, collectInfo, !change);
        } catch (DBException e) {
            log.debug("Error collecting child elements", e);
        }

        // Run ui
        UIUtils.syncExec(() -> {
            if (change) {
                for (DBNDatabaseNode child : collectInfo.targetChildren) {
                    viewer.setChecked(child, collectInfo.wasChecked);
                }
            }
            for (DBNDatabaseNode container : change ? collectInfo.targetContainers : Collections.singletonList(element)) {
                try {
                    DBNDatabaseNode[] directChildren = container.getChildren(monitor);
                    if (directChildren != null) {
                        boolean missingOne = false, missingAll = true;
                        for (DBNDatabaseNode node : directChildren) {
                            if (!viewer.getChecked(node)) {
                                missingOne = true;
                            } else {
                                missingAll = false;
                            }
                        }

                        viewer.setChecked(container, change ? collectInfo.wasChecked : !missingAll);
                        viewer.setGrayed(container, missingOne);
                    }
                } catch (DBException e) {
                    // shouldn't be here
                }
            }
        });
    }

    private boolean collectChildren(DBRProgressMonitor monitor, DBNDatabaseNode element, final CollectInfo collectInfo, boolean onlyChecked) throws DBException {
        if (monitor.isCanceled()) {
            return false;
        }

        for (ViewerFilter filter : filters) {
            if (!filter.select(viewer, element.getParentNode(), element)) {
                return false;
            }
        }

        boolean isChecked = checkedElements.containsKey(element) || element == collectInfo.rootElement;
        if (onlyChecked && !collectInfo.wasChecked && !isChecked) {
            // Uncheck event - this element never was checked so just skip it with all ts children
            return false;
        }
        if (!onlyChecked || isChecked) {
            for (Class<?> type : targetTypes) {
                if (!type.isInstance(element.getObject())) {
                    continue;
                }
                collectInfo.targetChildren.add(element);
                return true;
            }
        }
        element.initializeNode(monitor, null);
        DBNDatabaseNode[] children = element.getChildren(monitor);
        if (!ArrayUtils.isEmpty(children)) {
            boolean foundChild = false;
            for (DBNDatabaseNode child : children) {
                if (onlyChecked) {
                    if (checkedElements.containsKey(child)) {
                        foundChild = true;
                        break;
                    }
                } else {
                    try {
                        if (collectChildren(monitor, child, collectInfo, false)) {
                            foundChild = true;
                        }
                    } catch (DBException e) {
                        log.debug("Error reading child nodes of '" + child.getName() + "'", e);
                    }
                }
            }
            if (foundChild) {
                if (!collectInfo.targetContainers.contains(element)) {
                    collectInfo.targetContainers.add(element);
                    if (onlyChecked) {
                        checkedElements.put(element, Boolean.TRUE);
                    }
                }
            }
            return foundChild;
        }

        return false;
    }

    public void updateCheckStates() {
        Set<DBNDatabaseNode> parentList = new LinkedHashSet<>();
        for (Object element : viewer.getCheckedElements()) {
            for (DBNNode node = ((DBNDatabaseNode)element).getParentNode(); node != null; node = node.getParentNode()) {
                if (node instanceof DBNDatabaseNode) {
                    parentList.add((DBNDatabaseNode) node);
                    viewer.setChecked(node, true);
                }
            }
        }
        updateElementsCheck(parentList.toArray(), true, false);
    }
}
