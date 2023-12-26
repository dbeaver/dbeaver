/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Lorant Oroszlany (github.com/loro2)
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
package org.jkiss.dbeaver.ui.navigator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTreeFilterObjectType;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class NavigatorStatePersister {
    private static final Log log = Log.getLog(NavigatorStatePersister.class);
    private static final String KEY_PREFIX = "element";

    private static final String PROP_FILTER_TYPE = "filterType";
    private static final String PROP_FILTER_TEXT = "filterText";

    private NavigatorStatePersister() {
        // avoid instantiation of utility class
    }

    public static void saveExpandedState(Object[] expandedElements, IMemento memento) {
        for (int i = 0; i < expandedElements.length; i++)
            memento.putString(KEY_PREFIX + i, createNodeIdentifier((DBNNode) expandedElements[i]));
    }

    public static void restoreExpandedState(final TreeViewer navigatorViewer, final DBNNode rootNode, int maxDepth, final IMemento memento) {
        if (memento == null) {
            return;
        }
        final String[] nodeIdentifiers = Arrays.stream(memento.getAttributeKeys())
            .filter(x -> x.startsWith(KEY_PREFIX))
            .map(memento::getString)
            .toArray(String[]::new);
        if (nodeIdentifiers.length == 0) {
            return;
        }
        DBRRunnableWithProgress runnable = (monitor) -> {
            try {
                monitor.beginTask("Expand navigator nodes", nodeIdentifiers.length);
                for (String nodeIdentifier : nodeIdentifiers) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    monitor.subTask("Expand node " + nodeIdentifier);
                    DBNNode node = findNode(nodeIdentifier, rootNode, 1, maxDepth, monitor);
                    if (node != null && !node.isDisposed()) {
                        UIUtils.syncExec(() -> navigatorViewer.setExpandedState(node, true));
                    }
                }
                monitor.done();
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        };

        AbstractJob expandJob = new AbstractJob("Expand navigator nodes") {
            {
                setSystem(false);
                setUser(true);
            }

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    runnable.run(monitor);
                } catch (InvocationTargetException e) {
                    log.error(e);
                } catch (InterruptedException e) {
                    // ignore
                }
                return Status.OK_STATUS;
            }
        };
        UIUtils.asyncExec(expandJob::schedule);
    }

    public static void saveFilterState(@NotNull DatabaseNavigatorTree tree, @NotNull IMemento memento) {
        memento.putString(PROP_FILTER_TYPE, tree.getFilterObjectType().name());
        final Text filterControl = tree.getFilterControl();
        if (filterControl != null && !filterControl.isDisposed() && CommonUtils.isNotEmpty(filterControl.getText())) {
            memento.putString(PROP_FILTER_TEXT, filterControl.getText());
        }
    }

    public static void restoreFilterState(@NotNull DatabaseNavigatorTree tree, @NotNull IMemento memento) {
        UIUtils.syncExec(() -> {
            final DatabaseNavigatorTreeFilterObjectType type = CommonUtils.valueOf(
                DatabaseNavigatorTreeFilterObjectType.class,
                memento.getString(PROP_FILTER_TYPE)
            );
            if (type != null && tree.getFilterObjectType() != type) {
                tree.setFilterObjectType(type);
                tree.getViewer().getControl().setRedraw(false);
                try {
                    tree.getViewer().refresh();
                } finally {
                    tree.getViewer().getControl().setRedraw(true);
                }
            }

            final String text = memento.getString(PROP_FILTER_TEXT);
            final Text filterControl = tree.getFilterControl();
            if (CommonUtils.isNotEmpty(text) && filterControl != null && !filterControl.isDisposed()) {
                filterControl.setText(text);
                filterControl.notifyListeners(SWT.Modify, new Event());
            }
        });
    }

    private static DBNNode findNode(String nodeIdentifier, DBNNode rootNode, int currentDepth, int maxDepth, DBRProgressMonitor monitor) throws DBException {
        if (currentDepth <= maxDepth) {
            initializeNode(rootNode, monitor);
            if (nodeIdentifier.equals(createNodeIdentifier(rootNode)))
                return rootNode;
            if (currentDepth < maxDepth) {
                DBNNode[] childNodes = rootNode.getChildren(monitor);
                if (childNodes != null)
                    for (DBNNode newRootNode : childNodes)
                        if (nodeIdentifier.contains(createNodeIdentifier(newRootNode)))
                            return findNode(nodeIdentifier, newRootNode, currentDepth + 1, maxDepth, monitor);
            }
        }
        return null;
    }

    private static void initializeNode(DBNNode node, DBRProgressMonitor monitor) throws DBException {
        if (node instanceof DBNDataSource) {
            DBPDataSourceContainer dsContainer = ((DBNDataSource) node).getDataSourceContainer();
            long connectionTimeout = dsContainer.getPreferenceStore().getInt(ModelPreferences.CONNECTION_VALIDATION_TIMEOUT);
            long connectionStart = System.currentTimeMillis();
            while (!dsContainer.isConnected()) {
                dsContainer.connect(monitor, true, false);
                if (connectionTimeout > 0 && connectionStart + connectionTimeout <= System.currentTimeMillis()) {
                    break;
                }
                // Wait a few seconds to let in-progress connection initialize
                RuntimeUtils.pause(100);
            }
        }
        node.getChildren(monitor);
    }

    private static String createNodeIdentifier(DBNNode node) {
        StringBuilder identifier = new StringBuilder();
        for (DBNNode currentNode = node; currentNode != null; currentNode = currentNode.getParentNode()) {
            identifier.append(currentNode.getNodeDisplayName()).append("/");
        }
        return identifier.toString();
    }

}
