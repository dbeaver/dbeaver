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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.resources.ResourceUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;

public class OpenNewSQLEditorHandler extends BaseSQLEditorHandler {

    private static final Log log = Log.getLog(OpenNewSQLEditorHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBPDataSourceContainer dataSourceContainer = getCurrentConnection(event);
        IProject project = dataSourceContainer != null ? dataSourceContainer.getRegistry().getProject() : DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        IFolder scriptFolder = getCurrentFolder(event);
        IFile scriptFile;
        try {
            scriptFile = ResourceUtils.createNewScript(project, scriptFolder, dataSourceContainer);
        }
        catch (CoreException e) {
            log.error(e);
            return null;
        }

        NavigatorHandlerObjectOpen.openResource(scriptFile, HandlerUtil.getActiveWorkbenchWindow(event));

        return null;
    }

}