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

package org.jkiss.dbeaver.ui.actions;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.NavigatorUtils;

public abstract class DataSourceHandler extends AbstractHandler {

    static protected final Log log = Log.getLog(DataSourceHandler.class);

    protected DBSDataSourceContainer getDataSourceContainer(ExecutionEvent event, boolean useEditor)
    {
        if (useEditor) {
            IEditorPart editor = HandlerUtil.getActiveEditor(event);
            if (editor != null) {
                DBSDataSourceContainer container = getDataSourceContainer(editor);
                if (container != null) {
                    return container;
                }
            }
            return null;
        }
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        DBSDataSourceContainer container = getDataSourceContainer(activePart);
        if (container != null) {
            return container;
        }
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            DBSObject selectedObject = NavigatorUtils.getSelectedObject((IStructuredSelection) selection);
            if (selectedObject instanceof DBSDataSourceContainer) {
                return (DBSDataSourceContainer)selectedObject;
            } else if (selectedObject != null) {
                DBPDataSource dataSource = selectedObject.getDataSource();
                return dataSource == null ? null : dataSource.getContainer();
            }
        }
        return null;
    }

    public static DBSDataSourceContainer getDataSourceContainer(IWorkbenchPart activePart)
    {
        if (activePart instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider) activePart).getDataSourceContainer();
        }
        if (activePart instanceof IDataSourceProvider) {
            DBPDataSource dataSource = ((IDataSourceProvider) activePart).getDataSource();
            return dataSource == null ? null : dataSource.getContainer();
        }
        return null;
    }
}