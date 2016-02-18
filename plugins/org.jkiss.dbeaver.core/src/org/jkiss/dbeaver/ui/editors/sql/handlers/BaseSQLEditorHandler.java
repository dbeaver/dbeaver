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

package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.dialogs.connection.SelectDataSourceDialog;
import org.jkiss.dbeaver.ui.resources.ResourceUtils;

public abstract class BaseSQLEditorHandler extends AbstractDataSourceHandler {

    static final Log log = Log.getLog(BaseSQLEditorHandler.class);

    @Nullable
    protected DBPDataSourceContainer getCurrentConnection(ExecutionEvent event)
    {
        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer(event, false);
        final ProjectRegistry projectRegistry = DBeaverCore.getInstance().getProjectRegistry();
        IProject project = dataSourceContainer != null ? dataSourceContainer.getRegistry().getProject() : projectRegistry.getActiveProject();
        if (dataSourceContainer == null) {
            final DataSourceRegistry dataSourceRegistry = projectRegistry.getDataSourceRegistry(project);
            if (dataSourceRegistry == null) {
                return null;
            }
            if (dataSourceRegistry.getDataSources().size() == 1) {
                dataSourceContainer = dataSourceRegistry.getDataSources().get(0);
            } else if (!dataSourceRegistry.getDataSources().isEmpty()) {
                dataSourceContainer = SelectDataSourceDialog.selectDataSource(
                    HandlerUtil.getActiveShell(event));
            }
        }
        return dataSourceContainer;
    }

    @Nullable
    protected IFolder getCurrentFolder(ExecutionEvent event)
    {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
            final Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof DBNResource && ((DBNResource)element).getResource() instanceof IFolder) {
                return (IFolder) ((DBNResource)element).getResource();
            }
        }
        return null;
    }

    public static void openRecentScript(@NotNull IWorkbenchWindow workbenchWindow, @Nullable DBPDataSourceContainer dataSourceContainer, @Nullable IFolder scriptFolder) {
        IProject project = dataSourceContainer != null ? dataSourceContainer.getRegistry().getProject() : DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        IFile scriptFile;
        try {
            ResourceUtils.ResourceInfo res = ResourceUtils.findRecentScript(project, dataSourceContainer);
            if (res != null) {
                scriptFile = (IFile) res.getResource();
            } else {
                scriptFile = ResourceUtils.createNewScript(project, scriptFolder, dataSourceContainer);
            }
            NavigatorHandlerObjectOpen.openResource(scriptFile, workbenchWindow);
        }
        catch (CoreException e) {
            log.error(e);
        }
    }

}