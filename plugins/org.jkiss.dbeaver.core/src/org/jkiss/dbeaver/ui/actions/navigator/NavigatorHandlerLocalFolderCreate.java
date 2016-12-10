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
