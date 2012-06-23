/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.DBException;
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

    @Override
    public String getTypeName(IResource resource)
    {
        return "databases";
    }

    @Override
    public void initializeProject(IProject project, IProgressMonitor monitor) throws CoreException, DBException
    {
        final IFile configFile = project.getFile(CONFIG_FILE);
        if (!configFile.exists()) {
            configFile.create(new ByteArrayInputStream(CONFIG_EMPTY.getBytes()), true, monitor);
            configFile.setHidden(true);
        }
        configFile.setPersistentProperty(PROP_RESOURCE_TYPE, RES_TYPE_DATABASES);
    }

    @Override
    public DBNProjectDatabases makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        //DataSourceRegistry registry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(resource.getProject());
        return new DBNProjectDatabases(parentNode, resource, this);
    }

}
