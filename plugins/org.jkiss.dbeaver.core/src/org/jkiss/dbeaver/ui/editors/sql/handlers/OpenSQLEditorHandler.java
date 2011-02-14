/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.project.ScriptsHandlerImpl;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;

import java.util.HashSet;
import java.util.Set;

public class OpenSQLEditorHandler extends DataSourceHandler {

    static final Log log = LogFactory.getLog(OpenSQLEditorHandler.class);

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(event, false, false);
        final ProjectRegistry projectRegistry = DBeaverCore.getInstance().getProjectRegistry();
        IProject project = dataSourceContainer != null ? dataSourceContainer.getRegistry().getProject() : projectRegistry.getActiveProject();
        if (dataSourceContainer == null) {
            final DataSourceRegistry dataSourceRegistry = projectRegistry.getDataSourceRegistry(project);
            if (dataSourceRegistry.getDataSources().size() == 1) {
                dataSourceContainer = dataSourceRegistry.getDataSources().get(0);
            }
        }
        IFolder scriptFolder = null;
        if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
            final Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof DBNResource && ((DBNResource)element).getResource() instanceof IFolder) {
                scriptFolder = (IFolder) ((DBNResource)element).getResource();
            }
        }
        IFile scriptFile;
        try {
            scriptFile = ScriptsHandlerImpl.createNewScript(project, scriptFolder);
        }
        catch (CoreException e) {
            log.error(e);
            return null;
        }

        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        SQLEditorInput sqlInput = new SQLEditorInput(
            scriptFile,
            dataSourceContainer);
        try {
            workbenchWindow.getActivePage().openEditor(
                sqlInput,
                SQLEditor.class.getName());
        } catch (Exception ex) {
            log.error("Could not  open SQL editor", ex);
        }

        return null;
    }

/*
    private String getNewScriptName(IWorkbench workbench)
    {
        // Collect all open script names
        Set<String> openScripts = new HashSet<String>();
        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference editorReference : page.getEditorReferences()) {
                    try {
                        IEditorInput input = editorReference.getEditorInput();
                        if (input instanceof SQLEditorInput) {
                            openScripts.add( ((SQLEditorInput)input).getScriptName() );
                        }
                    } catch (PartInitException e) {
                        // do nothing
                    }
                }
            }
        }
        for (int i = 1; ; i++) {
            String scriptName = "" + i;
            if (!openScripts.contains(scriptName)) {
                return scriptName;
            }
        }
    }
*/

}