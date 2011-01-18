package org.jkiss.dbeaver.model.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;

/**
 * Project handler
 */
public interface DBPProjectHandler {

    void initializeProject(IProject project) throws CoreException, DBException;
    
}
