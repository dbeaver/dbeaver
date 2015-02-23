/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.dialogs.GotoObjectDialog;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;

public class NavigatorHandlerObjectGoto extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBPDataSource dataSource = null;
        DBSObject container = null;
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof IDataSourceProvider) {
            dataSource = ((IDataSourceProvider) activePart).getDataSource();
        } else if (activePart instanceof INavigatorModelView) {
            final ISelection selection = HandlerUtil.getCurrentSelection(event);
            if (selection instanceof IStructuredSelection) {
                Object element = ((IStructuredSelection) selection).getFirstElement();
                if (element instanceof DBSWrapper) {
                    DBSObject object = ((DBSWrapper) element).getObject();
                    if (object != null) {
                        container = object;
                        while (container instanceof DBSFolder) {
                            container = container.getParentObject();
                        }
                        dataSource = object.getDataSource();
                    }
                }
            }
        }
        if (dataSource == null) {
            return null;
        }
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        GotoObjectDialog dialog = new GotoObjectDialog(HandlerUtil.getActiveShell(event), dataSource, container);
        dialog.open();
        Object[] objectsToOpen = dialog.getResult();
        if (!ArrayUtils.isEmpty(objectsToOpen)) {
            Collection<DBNDatabaseNode> nodes = NavigatorHandlerObjectBase.getNodesByObjects(Arrays.asList(objectsToOpen));
            for (DBNDatabaseNode node : nodes) {
                NavigatorHandlerObjectOpen.openEntityEditor(node, null, workbenchWindow);
            }
        }

        return null;
    }


}