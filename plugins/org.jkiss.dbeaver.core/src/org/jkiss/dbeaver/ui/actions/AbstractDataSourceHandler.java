/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.actions;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

public abstract class AbstractDataSourceHandler extends AbstractHandler {

    static protected final Log log = Log.getLog(AbstractDataSourceHandler.class);

    protected DBCExecutionContext getExecutionContext(ExecutionEvent event, boolean useEditor)
    {
        if (useEditor) {
            IEditorPart editor = HandlerUtil.getActiveEditor(event);
            if (editor instanceof DBPContextProvider) {
                return ((DBPContextProvider) editor).getExecutionContext();
            }
            return null;
        }
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);

        if (activePart instanceof DBPContextProvider) {
            return ((DBPContextProvider) activePart).getExecutionContext();
        }

        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            DBSObject selectedObject = NavigatorUtils.getSelectedObject((IStructuredSelection) selection);
            if (selectedObject != null) {
                DBPDataSource dataSource = selectedObject.getDataSource();
                if (dataSource != null) {
                    return dataSource.getDefaultContext(false);
                }
            }
        }
        return null;
    }

    protected DBPDataSourceContainer getDataSourceContainer(ExecutionEvent event, boolean useEditor)
    {
        if (useEditor) {
            IEditorPart editor = HandlerUtil.getActiveEditor(event);
            if (editor != null) {
                DBPDataSourceContainer container = getDataSourceContainer(editor);
                if (container != null) {
                    return container;
                }
            }
            return null;
        }
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        DBPDataSourceContainer container = getDataSourceContainer(activePart);
        if (container != null) {
            return container;
        }
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            DBSObject selectedObject = NavigatorUtils.getSelectedObject((IStructuredSelection) selection);
            if (selectedObject instanceof DBPDataSourceContainer) {
                return (DBPDataSourceContainer)selectedObject;
            } else if (selectedObject != null) {
                DBPDataSource dataSource = selectedObject.getDataSource();
                return dataSource == null ? null : dataSource.getContainer();
            }
        }
        return null;
    }

    public static DBCExecutionContext getExecutionContext(IWorkbenchPart activePart)
    {
        if (activePart instanceof DBPContextProvider) {
            return ((DBPContextProvider) activePart).getExecutionContext();
        }
        return null;
    }

    public static DBPDataSourceContainer getDataSourceContainer(IWorkbenchPart activePart)
    {
        if (activePart instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider) activePart).getDataSourceContainer();
        }
        if (activePart instanceof DBPContextProvider) {
            DBCExecutionContext context = ((DBPContextProvider) activePart).getExecutionContext();
            return context == null ? null : context.getDataSource().getContainer();
        }
        return null;
    }
}