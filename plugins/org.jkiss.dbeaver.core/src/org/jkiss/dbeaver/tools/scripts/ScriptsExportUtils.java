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

package org.jkiss.dbeaver.tools.scripts;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.resources.ResourceUtils;

/**
 * Utils
 */
class ScriptsExportUtils {

    private static final Log log = Log.getLog(ScriptsExportUtils.class);

    static DBNNode getScriptsNode()
    {
        final IProject activeProject = DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        if (activeProject == null) {
			return DBeaverCore.getInstance().getNavigatorModel().getRoot();
		}
        final DBNProject projectNode = DBeaverCore.getInstance().getNavigatorModel().getRoot().getProject(activeProject);
        DBNNode scriptsNode = projectNode;
        final IFolder scriptsFolder;
        try {
            scriptsFolder = ResourceUtils.getScriptsFolder(activeProject, false);
        } catch (CoreException e) {
            log.error(e);
            return scriptsNode;
        }
        if (!scriptsFolder.exists()) {
            return scriptsNode;
        }
        try {
            for (DBNNode projectFolder : projectNode.getChildren(VoidProgressMonitor.INSTANCE)) {
                if (projectFolder instanceof DBNResource && ((DBNResource) projectFolder).getResource().equals(scriptsFolder)) {
                    scriptsNode = projectFolder;
                    break;
                }
            }
        } catch (DBException e) {
            log.error(e);
        }
        return scriptsNode;
    }

}
