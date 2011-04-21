package org.jkiss.dbeaver.ui.export.scripts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
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
        final IFolder scriptsFolder = ScriptsHandlerImpl.getScriptsFolder(activeProject);
        final DBNProject projectNode = DBeaverCore.getInstance().getNavigatorModel().getRoot().getProject(activeProject);
        DBNNode scriptsNode = null;
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
        if (scriptsNode == null) {
            log.warn("Can't find scripts navigator node");
            scriptsNode = DBeaverCore.getInstance().getNavigatorModel().getRoot();
        }
        return scriptsNode;
    }

}
