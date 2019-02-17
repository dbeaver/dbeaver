/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.actions.common;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectBase;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.ui.resources.bookmarks.BookmarkStorage;
import org.jkiss.dbeaver.ui.resources.bookmarks.BookmarksHandlerImpl;
import org.jkiss.dbeaver.ui.resources.bookmarks.DBNBookmark;

public class NavigateBookmarkHandler extends NavigatorHandlerObjectBase {

    private static final Log log = Log.getLog(NavigateBookmarkHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        final IWorkbenchPage activePage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        IViewPart activePart = activePage.findView(DatabaseNavigatorView.VIEW_ID);
        if (activePart instanceof NavigatorViewBase) {
            NavigatorViewBase navigatorView = (NavigatorViewBase) activePart;
            DBNNode selectedNode = NavigatorUtils.getSelectedNode(HandlerUtil.getCurrentSelection(event));
            if (selectedNode instanceof DBNBookmark) {
                BookmarkStorage storage = ((DBNBookmark) selectedNode).getStorage();

                final DBPDataSourceContainer dataSourceContainer = DBUtils.findDataSource(storage.getDataSourceId());
                if (dataSourceContainer == null) {
                    log.debug("Can't find datasource '" + storage.getDataSourceId() + "'"); //$NON-NLS-2$
                    return null;
                }
                final DBNDataSource dsNode = (DBNDataSource) NavigatorUtils.getNodeByObject(dataSourceContainer);
                if (dsNode == null) {
                    log.error("Can't find datasource node for '" + dataSourceContainer.getName() + "'"); //$NON-NLS-2$
                    return null;
                }
                dsNode.initializeNode(null, status -> {
                    if (status.isOK()) {
                        UIUtils.syncExec(() -> BookmarksHandlerImpl.navigateNodeByPath(navigatorView, dsNode, storage));
                    }
                });
            }
        }

        return null;
    }

}