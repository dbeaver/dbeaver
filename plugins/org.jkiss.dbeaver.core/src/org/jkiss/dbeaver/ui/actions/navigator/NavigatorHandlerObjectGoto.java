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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.ui.dialogs.GotoObjectDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;

public class NavigatorHandlerObjectGoto extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBCExecutionContext context = null;
        DBSObject container = null;
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof DBPContextProvider) {
            context = ((DBPContextProvider) activePart).getExecutionContext();
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
                        DBPDataSource dataSource = object.getDataSource();
                        if (dataSource != null) {
                            context = dataSource.getDefaultContext(true);
                        }
                    }
                }
            }
        }
        if (context == null) {
            DBUserInterface.getInstance().showError(
                "Go to object",
                "No active datasource");
            return null;
        }
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        GotoObjectDialog dialog = new GotoObjectDialog(HandlerUtil.getActiveShell(event), context, container);
        dialog.open();
        Object[] objectsToOpen = dialog.getResult();
        if (!ArrayUtils.isEmpty(objectsToOpen)) {
            Collection<DBNDatabaseNode> nodes = NavigatorHandlerObjectBase.getNodesByObjects(Arrays.asList(objectsToOpen));
            for (DBNDatabaseNode node : nodes) {
                NavigatorUtils.openNavigatorNode(node, workbenchWindow);
            }
        }

        return null;
    }


}