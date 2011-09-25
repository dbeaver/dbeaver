/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.scripts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.project.ScriptsHandlerImpl;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

/**
 * Utils
 */
class ScriptsExportUtils {

    static final Log log = LogFactory.getLog(ScriptsExportUtils.class);

    static DBNNode getScriptsNode()
    {
        final IProject activeProject = DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        final DBNProject projectNode = DBeaverCore.getInstance().getNavigatorModel().getRoot().getProject(activeProject);
        DBNNode scriptsNode = projectNode;
        final IFolder scriptsFolder;
        try {
            scriptsFolder = ScriptsHandlerImpl.getScriptsFolder(activeProject, false);
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
