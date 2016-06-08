/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.HashMap;
import java.util.Map;

public class NavigatorHandlerFilterObjects extends NavigatorHandlerObjectCreateBase implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            Map<DBNDatabaseFolder, DBSObjectFilter> folders = new HashMap<>();
            for (Object item : ((IStructuredSelection)selection).toArray()) {
                if (item instanceof DBNNode) {
                    final DBNNode node = (DBNNode) item;
                    DBNDatabaseFolder folder = (DBNDatabaseFolder) node.getParentNode();
                    DBSObjectFilter nodeFilter = folders.get(folder);
                    if (nodeFilter == null) {
                        nodeFilter = folder.getNodeFilter(folder.getItemsMeta(), true);
                        if (nodeFilter == null) {
                            nodeFilter = new DBSObjectFilter();
                        }
                        folders.put(folder, nodeFilter);
                    }
                    nodeFilter.addExclude(node.getNodeName());
                    nodeFilter.setEnabled(true);
                }
            }
            // Save folders
            for (Map.Entry<DBNDatabaseFolder, DBSObjectFilter> entry : folders.entrySet()) {
                entry.getKey().setNodeFilter(entry.getKey().getItemsMeta(), entry.getValue());
            }
            // Refresh all folders
            NavigatorHandlerRefresh.refreshNavigator(folders.keySet());
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        if (!updateUI) {
            return;
        }
        ISelectionProvider selectionProvider = UIUtils.getSelectionProvider(element.getServiceLocator());
        if (selectionProvider != null) {
            final ISelection selection = selectionProvider.getSelection();
            if (selection instanceof IStructuredSelection) {
                final int objectCount = ((IStructuredSelection) selection).size();
                if (objectCount > 1) {
                    element.setText("Filter " + objectCount + " objects");
                } else {
                    DBNNode node = NavigatorUtils.getSelectedNode(selection);
                    if (node != null) {
                        element.setText("Filter " + node.getNodeType());
                    }
                }
            }
        }
    }

}