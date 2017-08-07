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

package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ScriptSelectorPanel;
import org.jkiss.dbeaver.ui.dialogs.connection.SelectDataSourceDialog;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.resources.ResourceUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class OpenHandler extends AbstractDataSourceHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        String actionId = event.getCommand().getId();
        try {
            switch (actionId) {
                case CoreCommands.CMD_SQL_EDITOR_OPEN:
                    openEditor(event);
                    break;
                case CoreCommands.CMD_SQL_EDITOR_NEW:
                    openNewEditor(event);
                    break;
                case CoreCommands.CMD_SQL_EDITOR_RECENT:
                    openRecentEditor(event);
                    break;
            }
        } catch (CoreException e) {
            DBUserInterface.getInstance().showError("Open editor", "Can execute command '" + actionId + "'", e);
        }
        return null;
    }

    private static void openEditor(ExecutionEvent event) throws ExecutionException, CoreException {
        List<DBPDataSourceContainer> containers = getDataSourceContainers(event);
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);

        IProject project = !containers.isEmpty() ?
            containers.get(0).getRegistry().getProject() :
            DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        checkProjectIsOpen(project);
        final DBPDataSourceContainer[] containerList = containers.toArray(new DBPDataSourceContainer[containers.size()]);

        final IFolder rootFolder = ResourceUtils.getScriptsFolder(project, true);
        final List<ResourceUtils.ResourceInfo> scriptTree = ResourceUtils.findScriptTree(rootFolder, containerList.length == 0 ? null : containerList);
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

    private static boolean openNewEditor(ExecutionEvent event) throws CoreException {
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        DBPDataSourceContainer dataSourceContainer = getCurrentConnection(event);
        IFolder scriptFolder = getCurrentFolder(event);

        IProject project = dataSourceContainer != null ? dataSourceContainer.getRegistry().getProject() : DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        checkProjectIsOpen(project);
        IFile scriptFile = ResourceUtils.createNewScript(project, scriptFolder, dataSourceContainer);

        NavigatorHandlerObjectOpen.openResource(scriptFile, workbenchWindow);
        return true;
    }

    private static void openRecentEditor(ExecutionEvent event) throws ExecutionException, CoreException {
        DBPDataSourceContainer dataSourceContainer = getCurrentConnection(event);
        if (dataSourceContainer == null) {
            return;
        }
        openRecentScript(HandlerUtil.getActiveWorkbenchWindow(event), dataSourceContainer, null);
    }

    @Nullable
    private static DBPDataSourceContainer getCurrentConnection(ExecutionEvent event)
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
                    HandlerUtil.getActiveShell(event), project);
            }
        }
        return dataSourceContainer;
    }

    @Nullable
    private static IFolder getCurrentFolder(ExecutionEvent event)
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

    private static List<DBPDataSourceContainer> getDataSourceContainers(ExecutionEvent event)
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

    private static DBPDataSourceContainer getDataSourceContainers(IWorkbenchPart activePart)
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

    public static void openRecentScript(@NotNull IWorkbenchWindow workbenchWindow, @Nullable DBPDataSourceContainer dataSourceContainer, @Nullable IFolder scriptFolder) throws CoreException {
        final IProject project = dataSourceContainer != null ? dataSourceContainer.getRegistry().getProject() : DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        checkProjectIsOpen(project);
        ResourceUtils.ResourceInfo res = ResourceUtils.findRecentScript(project, dataSourceContainer);
        if (res != null) {
            NavigatorHandlerObjectOpen.openResourceEditor(workbenchWindow, res);
        } else {
            IFile scriptFile = ResourceUtils.createNewScript(project, scriptFolder, dataSourceContainer);
            NavigatorHandlerObjectOpen.openResource(scriptFile, workbenchWindow);
        }
    }

    private static void checkProjectIsOpen(final IProject project) throws CoreException {
        if (project == null) {
        	throw new CoreException(GeneralUtils.makeExceptionStatus(new IllegalStateException("No active project.")));
        }
        if (!project.isOpen()) {
            try {
                DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            project.open(monitor.getNestedMonitor());
                        } catch (CoreException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                throw (CoreException)e.getTargetException();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public static SQLEditor openSQLConsole(
        IWorkbenchWindow workbenchWindow,
        DBPDataSourceContainer dataSourceContainer,
        String name,
        String sqlText)
    {
        StringEditorInput sqlInput = new StringEditorInput(name, sqlText, false, GeneralUtils.DEFAULT_ENCODING);
        return openSQLConsole(workbenchWindow, dataSourceContainer, sqlInput);
    }

    public static SQLEditor openSQLConsole(IWorkbenchWindow workbenchWindow, DBPDataSourceContainer dataSourceContainer, IEditorInput sqlInput) {
        EditorUtils.setInputDataSource(sqlInput, dataSourceContainer, false);
        return openSQLEditor(workbenchWindow, sqlInput);
    }

    public static SQLEditor openSQLEditor(
        IWorkbenchWindow workbenchWindow,
        IEditorInput sqlInput)
    {
        try {
            return (SQLEditor)workbenchWindow.getActivePage().openEditor(
                sqlInput,
                SQLEditor.class.getName());
        } catch (PartInitException e) {
            DBUserInterface.getInstance().showError("Can't open editor", null, e);
        }
        return null;
    }

}