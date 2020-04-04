/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.controls.ScriptSelectorPanel;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.INonPersistentEditorInput;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDataSourceDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.List;

public class SQLEditorHandlerOpenEditor extends AbstractDataSourceHandler {

    public static void openResource(IResource resource) {
        openResource(resource, new SQLNavigatorContext());
    }

    public static void openResource(IResource resource, @NotNull SQLNavigatorContext navigatorContext) {
        try {
            DBPResourceHandler handler = DBWorkbench.getPlatform().getWorkspace().getResourceHandler(resource);
            if (handler != null) {
                if (resource instanceof IFile && navigatorContext.getDataSourceContainer() != null) {
                    EditorUtils.setFileDataSource((IFile) resource, navigatorContext);
                }
                handler.openResource(resource);
            }
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError(UINavigatorMessages.actions_navigator_error_dialog_open_resource_title, "Can't open resource '" + resource.getName() + "'", e); //$NON-NLS-3$
        }
    }

    public static void openResourceEditor(IWorkbenchWindow workbenchWindow, SQLEditorUtils.ResourceInfo resourceInfo, SQLNavigatorContext context) {
        if (resourceInfo.getResource() != null) {
            openResource(resourceInfo.getResource(), context);
        } else if (resourceInfo.getLocalFile() != null) {
            EditorUtils.setFileDataSource(resourceInfo.getLocalFile(), context);
            EditorUtils.openExternalFileEditor(resourceInfo.getLocalFile(), workbenchWindow);
        }
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        String actionId = event.getCommand().getId();
        try {
            switch (actionId) {
                case SQLEditorCommands.CMD_SQL_EDITOR_OPEN:
                    openEditor(event);
                    break;
                case SQLEditorCommands.CMD_SQL_EDITOR_NEW:
                    openNewEditor(event);
                    break;
                case SQLEditorCommands.CMD_SQL_EDITOR_RECENT:
                    openRecentEditor(event);
                    break;
            }
        } catch (InterruptedException e) {
            return null;
        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showError("Open editor", "Can execute command '" + actionId + "'", e);
        }
        return null;
    }

    private static void openEditor(ExecutionEvent event) throws ExecutionException, CoreException, InterruptedException {
        SQLNavigatorContext editorContext = getCurrentContext(event);

        DBPProject project = editorContext.getProject();
        checkProjectIsOpen(project);

        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        final IFolder rootFolder = SQLEditorUtils.getScriptsFolder(project, true);
        final List<SQLEditorUtils.ResourceInfo> scriptTree = SQLEditorUtils.findScriptTree(project, rootFolder, editorContext.getDataSourceContainer());
        if (scriptTree.isEmpty()) {
            // Create new script
            final IFile newScript = SQLEditorUtils.createNewScript(project, rootFolder, editorContext);
            openResource(newScript, editorContext);
        } else {
            // Show script chooser
            ScriptSelectorPanel.showTree(workbenchWindow, editorContext, rootFolder, scriptTree);
        }
    }

    public static IFile openNewEditor(@NotNull SQLNavigatorContext editorContext, ISelection selection) throws CoreException {
        DBPProject project = editorContext.getProject();
        checkProjectIsOpen(project);
        IFolder folder = getCurrentScriptFolder(selection);
        IFile scriptFile = SQLEditorUtils.createNewScript(project, folder, editorContext);

        openResource(scriptFile, editorContext);

        return scriptFile;
    }

    public static IFolder getCurrentScriptFolder(ISelection selection) {
        IFolder folder = null;
        if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
            final Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof IFolder) {
                folder = (IFolder) element;
            } else if (element instanceof DBNResource && ((DBNResource) element).getResource() instanceof IFolder) {
                folder = (IFolder) ((DBNResource) element).getResource();
            }
        }
        return folder;
    }

    private static void openNewEditor(ExecutionEvent event) throws CoreException, InterruptedException {
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        SQLNavigatorContext context = getCurrentContext(event);

        openNewEditor(context, HandlerUtil.getCurrentSelection(event));
    }

    private static void openRecentEditor(ExecutionEvent event) throws CoreException, InterruptedException {
        SQLNavigatorContext context = getCurrentContext(event);
        openRecentScript(HandlerUtil.getActiveWorkbenchWindow(event), context, null);
    }

    @NotNull
    private static SQLNavigatorContext getCurrentContext(ExecutionEvent event) throws InterruptedException {
        SQLNavigatorContext context = new SQLNavigatorContext(event);

        if (context.getDataSourceContainer() == null) {
            DBPProject project = NavigatorUtils.getSelectedProject();
            if (project != null) {
                final DBPDataSourceRegistry dataSourceRegistry = project.getDataSourceRegistry();
                if (dataSourceRegistry.getDataSources().size() == 1) {
                    context.setDataSourceContainer(dataSourceRegistry.getDataSources().get(0));
                } else if (!dataSourceRegistry.getDataSources().isEmpty()) {
                    SelectDataSourceDialog dialog = new SelectDataSourceDialog(HandlerUtil.getActiveShell(event), project, null);
                    dialog.setModeless(true);
                    if (dialog.open() == IDialogConstants.CANCEL_ID) {
                        throw new InterruptedException();
                    }
                    context.setDataSourceContainer(dialog.getDataSource());
                }
            }
        }
        return context;
    }

    private static List<DBPDataSourceContainer> getDataSourceContainers(ExecutionEvent event) {
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

    private static DBPDataSourceContainer getDataSourceContainers(IWorkbenchPart activePart) {
        if (activePart instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider) activePart).getDataSourceContainer();
        }
        if (activePart instanceof DBPContextProvider) {
            DBCExecutionContext context = ((DBPContextProvider) activePart).getExecutionContext();
            return context == null ? null : context.getDataSource().getContainer();
        }
        return null;
    }

    public static void openRecentScript(@NotNull IWorkbenchWindow workbenchWindow, @NotNull SQLNavigatorContext editorContext, @Nullable IFolder scriptFolder) throws CoreException {
        final DBPProject project = editorContext.getProject();
        checkProjectIsOpen(project);
        SQLEditorUtils.ResourceInfo res = SQLEditorUtils.findRecentScript(project, editorContext);
        if (res != null) {
            openResourceEditor(workbenchWindow, res, editorContext);
        } else {
            IFile scriptFile = SQLEditorUtils.createNewScript(project, scriptFolder, editorContext);
            openResource(scriptFile, editorContext);
        }
    }

    static void checkProjectIsOpen(final DBPProject project) throws CoreException {
        if (project == null) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(new IllegalStateException("No active project.")));
        }
        project.ensureOpen();
    }

    public static SQLEditor openSQLConsole(
        IWorkbenchWindow workbenchWindow,
        SQLNavigatorContext context,
        String name,
        String sqlText) {
        StringEditorInput sqlInput = new StringEditorInput(name, sqlText, false, GeneralUtils.DEFAULT_ENCODING);
        return openSQLConsole(workbenchWindow, context, sqlInput);
    }

    public static SQLEditor openSQLConsole(IWorkbenchWindow workbenchWindow, SQLNavigatorContext context, IEditorInput sqlInput) {
        EditorUtils.setInputDataSource(sqlInput, context);
        return openSQLEditor(workbenchWindow, sqlInput);
    }

    private static SQLEditor openSQLEditor(
        IWorkbenchWindow workbenchWindow,
        IEditorInput sqlInput) {
        try {
            boolean isConsole = sqlInput instanceof INonPersistentEditorInput;
            return (SQLEditor) workbenchWindow.getActivePage().openEditor(
                sqlInput,
                SQLEditor.class.getName(),
                true,
                isConsole ? IWorkbenchPage.MATCH_NONE : IWorkbenchPage.MATCH_INPUT);
        } catch (PartInitException e) {
            DBWorkbench.getPlatformUI().showError("Can't open editor", null, e);
        }
        return null;
    }

}