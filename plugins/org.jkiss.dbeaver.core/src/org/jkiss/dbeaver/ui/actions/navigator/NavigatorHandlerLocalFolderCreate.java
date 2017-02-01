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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class NavigatorHandlerLocalFolderCreate extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection) selection;
            List<DBNDataSource> dataSources = new ArrayList<>();
            DBNProjectDatabases databasesNode = null;
            DBNLocalFolder parentFolder = null;
            for (Iterator iter = structSelection.iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                if (element instanceof DBNDataSource) {
                    dataSources.add((DBNDataSource) element);
                    element = ((DBNDataSource) element).getParentNode();
                }
                if (element instanceof DBNLocalFolder) {
                    parentFolder = (DBNLocalFolder) element;
                    databasesNode = (DBNProjectDatabases) parentFolder.getParentNode();
                } else if (element instanceof DBNProjectDatabases) {
                    databasesNode = (DBNProjectDatabases) element;
                }
            }
            if (databasesNode != null) {
                createFolder(HandlerUtil.getActiveWorkbenchWindow(event), databasesNode, parentFolder, dataSources, null);
            }
        }
        return null;
    }

    public static boolean createFolder(IWorkbenchWindow workbenchWindow, DBNProjectDatabases databases, DBNLocalFolder parentFolder, final Collection<DBNDataSource> nodes, String newName)
    {
        if (newName == null) {
            newName = EnterNameDialog.chooseName(workbenchWindow.getShell(), "Folder name");
        }
        if (CommonUtils.isEmpty(newName)) {
            return false;
        }
        // Create folder and refresh databases root
        // DS container will reload folders on refresh
        final DBPDataSourceRegistry dsRegistry = databases.getDataSourceRegistry();
        DBPDataSourceFolder folder = dsRegistry.addFolder(parentFolder == null ? null : parentFolder.getFolder(), newName);
        for (DBNDataSource node : nodes) {
            node.setFolder(folder);
        }
        DBNModel.updateConfigAndRefreshDatabases(databases);

        return true;
    }
}
