/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.project.ScriptsHandlerImpl;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;

public class CreateSQLEditorHandler extends BaseSQLEditorHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBSDataSourceContainer dataSourceContainer = getCurrentConnection(event);
        IProject project = dataSourceContainer != null ? dataSourceContainer.getRegistry().getProject() : DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        IFolder scriptFolder = getCurrentFolder(event);
        IFile scriptFile;
        try {
            scriptFile = ScriptsHandlerImpl.createNewScript(project, scriptFolder, dataSourceContainer);
        }
        catch (CoreException e) {
            log.error(e);
            return null;
        }

        NavigatorHandlerObjectOpen.openResource(scriptFile, HandlerUtil.getActiveWorkbenchWindow(event));

        return null;
    }

}