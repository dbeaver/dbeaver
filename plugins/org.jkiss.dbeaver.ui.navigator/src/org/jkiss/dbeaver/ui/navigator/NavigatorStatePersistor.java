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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IMemento;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;

public class NavigatorStatePersistor {

    private static final Log log = Log.getLog(NavigatorStatePersistor.class);
    private static final String KEY_PREFIX = "element";

    public void saveState(Object[] expandedElements, IMemento memento) {
        for (int i = 0; i < expandedElements.length; i++)
            memento.putString(KEY_PREFIX + i, createNodeIdentifier((DBNNode) expandedElements[i]));
    }

    public void restoreState(final TreeViewer navigatorViewer, final DBNNode rootNode, final IMemento memento) {
        DBRRunnableWithProgress runnable = (monitor) -> {
            try {
                if (memento != null) {
                    int maxDepth = DBWorkbench.getPlatform().getPreferenceStore().getInt(NavigatorPreferences.NAVIGATOR_RESTORE_STATE_DEPTH);
                    for (int i = 0; i < memento.getAttributeKeys().length; i++) {
                        String nodeIdentifier = memento.getString(KEY_PREFIX + i);
                        DBNNode node = findNode(nodeIdentifier, rootNode, 1, maxDepth, monitor);
                        if (node != null && !node.isDisposed()) {
                            navigatorViewer.setExpandedState(node, true);
                        }
                    }
                }
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        };
        try {
            UIUtils.runInUI(runnable);
        } catch (Exception e) {
            // this operation should be fail-safe
            log.error("Can't restore navigator tree state", e);
        }
    }

    private DBNNode findNode(String nodeIdentifier, DBNNode rootNode, int currentDepth, int maxDepth, DBRProgressMonitor monitor) throws DBException {
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

    private void initializeNode(DBNNode node, DBRProgressMonitor monitor) throws DBException {
        if (node instanceof DBNDataSource) {
            DBPDataSourceContainer dsContainer = ((DBNDataSource) node).getDataSourceContainer();
            if (!dsContainer.isConnected())
                dsContainer.connect(monitor, true, false);
        }
        node.getChildren(monitor);
    }

    private String createNodeIdentifier(DBNNode node) {
        StringBuilder identifier = new StringBuilder();
        for (DBNNode currentNode = node; currentNode != null; currentNode = currentNode.getParentNode())
            identifier.append(currentNode.getNodeName()).append("/");
        return identifier.toString();
    }

}
