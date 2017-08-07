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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ViewerFilter;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CheckboxTreeManager implements ICheckStateListener {

    private static final Log log = Log.getLog(CheckboxTreeManager.class);

    private final CheckboxTreeViewer viewer;
    private final Class<?>[] targetTypes;
    private Object[] checkedElements;
    private final ViewerFilter[] filters;

    public CheckboxTreeManager(CheckboxTreeViewer viewer, Class<?>[] targetTypes) {
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
        checkedElements = viewer.getCheckedElements();
        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Load sources tree", 100 * elements.length);
                    try {
                        for (Object element : elements) {
                            updateElementHierarchy(monitor, element, checked, change);

                            if (change) {
                                // Update parent state
                                if (element instanceof DBNDatabaseNode) {
                                    for (DBNNode node = ((DBNDatabaseNode) element).getParentNode(); node != null; node = node.getParentNode()) {
                                        if (node instanceof DBNDatabaseNode) {
                                            updateElementHierarchy(monitor, node, checked, false);
                                        }
                                        if (node instanceof DBNDataSource) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            log.error("Error updating checkbox state", e.getTargetException());
            //UIUtils.showErrorDialog(viewer.getControl().getShell(), "Error", "Can't collect child nodes", e.getTargetException());
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private void updateElementHierarchy(final DBRProgressMonitor monitor, final Object element, final boolean checked, final boolean change) throws DBException {
        final List<DBNDatabaseNode> targetChildren = new ArrayList<>();
        final List<DBNDatabaseNode> targetContainers = new ArrayList<>();
        try {
            collectChildren(monitor, element, targetChildren, targetContainers, !change);
        } catch (DBException e) {
            log.warn("Error collecting child elements", e);
        }

        // Run ui
        DBeaverUI.syncExec(new Runnable() {
            @Override
            public void run() {
                if (change) {
                    for (DBNDatabaseNode child : targetChildren) {
                        viewer.setChecked(child, checked);
                    }
                }
                for (DBNDatabaseNode container : change ? targetContainers : Collections.singletonList((DBNDatabaseNode) element)) {
                    try {
                        DBNDatabaseNode[] directChildren = container.getChildren(new VoidProgressMonitor());
                        if (directChildren != null) {
                            boolean missingOne = false, missingAll = true;
                            for (DBNDatabaseNode node : directChildren) {
                                if (!viewer.getChecked(node)) {
                                    missingOne = true;
                                } else {
                                    missingAll = false;
                                }
                            }

                            viewer.setChecked(container, change ? checked : !missingAll);
                            viewer.setGrayed(container, missingOne);
                        }
                    } catch (DBException e) {
                        // shouldn't be here
                    }
                }
            }
        });
    }

    private boolean collectChildren(DBRProgressMonitor monitor, final Object element, List<DBNDatabaseNode> targetChildren, List<DBNDatabaseNode> targetContainers, boolean onlyChecked) throws DBException {
        if (element instanceof DBNDatabaseNode) {
            for (ViewerFilter filter : filters) {
                if (!filter.select(viewer, ((DBNDatabaseNode) element).getParentNode(), element)) {
                    return false;
                }
            }

            boolean isChecked = ArrayUtils.contains(checkedElements, element);
            for (Class<?> type : targetTypes) {
                if (type.isInstance(((DBNDatabaseNode) element).getObject())) {
                    if (!onlyChecked || isChecked) {
                        targetChildren.add((DBNDatabaseNode) element);
                    }
                    return true;
                }
            }
            ((DBNDatabaseNode) element).initializeNode(monitor, null);
            DBNDatabaseNode[] children = ((DBNDatabaseNode) element).getChildren(monitor);
            if (!ArrayUtils.isEmpty(children)) {
                boolean foundChild = false;
                for (DBNDatabaseNode child : children) {
                    if (collectChildren(monitor, child, targetChildren, targetContainers, onlyChecked)) {
                        foundChild = true;
                    }
                }
                if (foundChild) {
                    if (!onlyChecked || isChecked) {
                        targetContainers.add((DBNDatabaseNode) element);
                    }
                }
                return foundChild;
            }
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
