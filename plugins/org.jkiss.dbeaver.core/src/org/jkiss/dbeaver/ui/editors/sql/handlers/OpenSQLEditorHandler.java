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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.resources.ScriptsHandlerImpl;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;

public class OpenSQLEditorHandler extends BaseSQLEditorHandler {

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBSDataSourceContainer dataSourceContainer = getCurrentConnection(event);
        if (dataSourceContainer == null) {
            return null;
        }
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        IFolder scriptFolder = getCurrentFolder(event);
        openRecentScript(workbenchWindow, dataSourceContainer, scriptFolder);

        return null;
    }

    public static void openRecentScript(@NotNull IWorkbenchWindow workbenchWindow, @Nullable DBSDataSourceContainer dataSourceContainer, @Nullable IFolder scriptFolder) {
        IProject project = dataSourceContainer != null ? dataSourceContainer.getRegistry().getProject() : DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        IFile scriptFile;
        try {
            scriptFile = ScriptsHandlerImpl.findRecentScript(project, dataSourceContainer);
            if (scriptFile == null) {
                scriptFile = ScriptsHandlerImpl.createNewScript(project, scriptFolder, dataSourceContainer);
            }
            NavigatorHandlerObjectOpen.openResource(scriptFile, workbenchWindow);
        }
        catch (CoreException e) {
            log.error(e);
        }
    }

}