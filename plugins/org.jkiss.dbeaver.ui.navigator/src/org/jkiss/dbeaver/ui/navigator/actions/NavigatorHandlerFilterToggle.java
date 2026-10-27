/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.UIServiceFilterConfig;

import java.util.Collections;

public class NavigatorHandlerFilterToggle extends AbstractHandler {

    private static final Log log = Log.getLog(NavigatorHandlerFilterToggle.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node instanceof DBNDatabaseItem) {
            node = node.getParentNode();
        }
        if (node instanceof DBNDatabaseFolder) {
            final DBNDatabaseFolder folder = (DBNDatabaseFolder) node;
            DBXTreeItem itemsMeta = folder.getItemsMeta();
            if (itemsMeta != null) {
                UIServiceFilterConfig service = DBWorkbench.findService(UIServiceFilterConfig.class);
                DBSObjectFilter nodeFilter = null;
                if (service != null && service.isUseUserFilter(folder)) {
                    nodeFilter = folder.getUserNodeFilter(itemsMeta, true);
                }
                boolean isUserFilter = nodeFilter != null;
                nodeFilter = nodeFilter == null ? folder.getNodeFilter(itemsMeta, true) : nodeFilter;
                if (nodeFilter != null) {
                    nodeFilter.setEnabled(!nodeFilter.isEnabled());
                    if (isUserFilter && service != null) {
                        try {
                            service.setParentNodeFilter(folder, itemsMeta, nodeFilter);
                        } catch (DBException e) {
                            log.error(e);
                        }
                    } else {
                        NavigatorHandlerRefresh.refreshNavigator(Collections.singleton(folder));
                    }
                }
            }

        }
        return null;
    }

}
