/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProjectDatabases;
import org.jkiss.dbeaver.registry.DataSourceRegistry;

import java.io.ByteArrayInputStream;

/**
 * Databases handler
 */
public class DatabasesHandlerImpl extends AbstractResourceHandler {

    public static final String RES_TYPE_DATABASES = "databases";

    private static final String CONFIG_FILE = DataSourceRegistry.CONFIG_FILE_NAME;

    private static final String CONFIG_EMPTY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
        "<data-sources></data-sources>";

    public void initializeProject(IProject project, IProgressMonitor monitor) throws CoreException, DBException
    {
        final IFile configFile = project.getFile(CONFIG_FILE);
        if (!configFile.exists()) {
            configFile.create(new ByteArrayInputStream(CONFIG_EMPTY.getBytes()), true, monitor);
        }
        configFile.setPersistentProperty(PROP_RESOURCE_TYPE, RES_TYPE_DATABASES);
    }

    public DBNProjectDatabases makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        //DataSourceRegistry registry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(resource.getProject());
        return new DBNProjectDatabases(parentNode, resource, this);
    }

}
