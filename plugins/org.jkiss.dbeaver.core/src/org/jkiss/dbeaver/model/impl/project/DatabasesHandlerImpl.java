/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProjectDatabases;

/**
 * Databases handler
 */
public class DatabasesHandlerImpl extends AbstractResourceHandler {

    public void initializeProject(IProject project, IProgressMonitor monitor) throws CoreException, DBException
    {
    }

    public DBNProjectDatabases makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        return new DBNProjectDatabases(parentNode, resource.getProject());
    }

}
