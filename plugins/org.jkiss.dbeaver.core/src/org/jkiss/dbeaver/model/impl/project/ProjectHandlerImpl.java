/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;

/**
 * Project handler
 */
public class ProjectHandlerImpl extends AbstractResourceHandler {

    public static final String RES_TYPE_PROJECT = "project";

    @Override
    public String getTypeName(IResource resource)
    {
        return "project";
    }

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource != DBeaverCore.getInstance().getProjectRegistry().getActiveProject()) {
            return FEATURE_DELETE | FEATURE_RENAME;
        }
        return FEATURE_RENAME;
    }

    @Override
    public void initializeProject(IProject project, IProgressMonitor monitor) throws CoreException, DBException
    {
        if (!ProjectHandlerImpl.RES_TYPE_PROJECT.equals(project.getPersistentProperty(DBPResourceHandler.PROP_RESOURCE_TYPE))) {
            project.setPersistentProperty(DBPResourceHandler.PROP_RESOURCE_TYPE, ProjectHandlerImpl.RES_TYPE_PROJECT);
        }
    }

    @Override
    public DBNProject makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        return new DBNProject(parentNode, (IProject)resource, this);
    }

    @Override
    public void openResource(IResource resource, IWorkbenchWindow window) throws CoreException, DBException
    {
        // do nothing
    }
}
