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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ScriptSelectorPanel;
import org.jkiss.dbeaver.ui.resources.ResourceUtils;
import org.jkiss.dbeaver.ui.resources.ResourceUtils.ResourceInfo;

import java.util.ArrayList;
import java.util.List;

public class OpenSQLEditorHandler extends BaseSQLEditorHandler {

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        List<DBPDataSourceContainer> containers = getDataSourceContainers(event);
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);

        IProject project = !containers.isEmpty() ?
            containers.get(0).getRegistry().getProject() :
            DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        final DBPDataSourceContainer[] containerList = containers.toArray(new DBPDataSourceContainer[containers.size()]);
        try {
            final IFolder rootFolder = ResourceUtils.getScriptsFolder(project, false);
            final List<ResourceInfo> scriptTree = ResourceUtils.findScriptTree(rootFolder, containerList.length == 0 ? null : containerList);
            if (scriptTree.isEmpty() && containerList.length == 1) {
                // Create new script
                final IFile newScript = ResourceUtils.createNewScript(project, rootFolder, containers.isEmpty() ? null : containers.get(0));
                NavigatorHandlerObjectOpen.openResource(newScript, workbenchWindow);
            } else {
                // Show script chooser
                ScriptSelectorPanel selector = new ScriptSelectorPanel(workbenchWindow, containerList, rootFolder);
                selector.showTree(scriptTree);
            }
        }
        catch (CoreException e) {
            log.error(e);
        }


        return null;
    }

    protected List<DBPDataSourceContainer> getDataSourceContainers(ExecutionEvent event)
    {
        List<DBPDataSourceContainer> containers = new ArrayList<>();
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            for (Object obj : ((IStructuredSelection) selection).toArray()) {
                if (obj instanceof DBNLocalFolder) {
                    for (DBNDataSource ds : ((DBNLocalFolder) obj).getDataSources()) {
                        containers.add(ds.getDataSourceContainer());
                    }
                } else {
                    DBSObject selectedObject = DBUtils.getFromObject(obj);
                    if (selectedObject != null) {
                        if (selectedObject instanceof DBPDataSourceContainer) {
                            containers.add((DBPDataSourceContainer) selectedObject);
                        } else {
                            containers.add(selectedObject.getDataSource().getContainer());
                        }
                    }
                }
            }
        }

        if (containers.isEmpty()) {
            IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
            DBPDataSourceContainer partContainer = getDataSourceContainers(activePart);
            if (partContainer != null) {
                containers.add(partContainer);
            }
        }

        return containers;
    }

    public static DBPDataSourceContainer getDataSourceContainers(IWorkbenchPart activePart)
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