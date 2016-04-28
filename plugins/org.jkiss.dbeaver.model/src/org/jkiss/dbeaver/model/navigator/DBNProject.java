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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DBNProject
 */
public class DBNProject extends DBNResource implements IAdaptable
{
    private static final Log log = Log.getLog(DBNProject.class);

    public DBNProject(DBNNode parentNode, IProject project, DBPResourceHandler handler)
    {
        super(parentNode, project, handler);
        getModel().getApplication().getProjectManager().addProject(project);
    }

    @Override
    protected void dispose(boolean reflect)
    {
        IProject project = getProject();
        super.dispose(reflect);
        getModel().getApplication().getProjectManager().removeProject(project);
    }

    public IProject getProject()
    {
        return (IProject)getResource();
    }

    public DBNProjectDatabases getDatabases()
    {
        try {
            for (DBNNode db : getChildren(VoidProgressMonitor.INSTANCE)) {
                if (db instanceof DBNProjectDatabases) {
                    return (DBNProjectDatabases) db;
                }
            }
        } catch (DBException e) {
            throw new IllegalStateException("Can't read project contents", e);
        }
        throw new IllegalStateException("No databases resource in project");
    }

    @Override
    public String getNodeDescription()
    {
        try {
            return getProject().getDescription().getComment();
        } catch (CoreException e) {
            log.debug(e);
            return null;
        }
    }

    @Override
    public DBPImage getNodeIcon()
    {
        return DBIcon.PROJECT;
    }

    @Override
    public boolean allowsOpen()
    {
        return false;
    }

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == DBNProject.class) {
            return this;
        } else if (adapter == IProject.class) {
            return getProject();
        }
        return null;
    }

    @Override
    public boolean supportsRename()
    {
        // Do not rename active projects
        return getModel().getApplication().getProjectManager().getActiveProject() != getProject();
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        try {
            final IProjectDescription description = getProject().getDescription();
            description.setName(newName);
            getProject().move(description, true, monitor.getNestedMonitor());
        } catch (CoreException e) {
            throw new DBException("Can't rename project", e);
        }
    }

    @Override
    protected DBNNode[] readChildNodes(DBRProgressMonitor monitor) throws DBException
    {
        if (!getProject().isOpen()) {
            try {
                getProject().open(monitor.getNestedMonitor());
                getProject().refreshLocal(IFile.DEPTH_ONE, monitor.getNestedMonitor());
            } catch (CoreException e) {
                throw new DBException("Can't open project '" + getProject().getName() + "'", e);
            }
        }
        DBPDataSourceRegistry dataSourceRegistry = getModel().getApplication().getProjectManager().getDataSourceRegistry(getProject());
        DBNNode[] children = super.readChildNodes(monitor);
        if (dataSourceRegistry != null) {
            children = ArrayUtils.add(DBNNode.class, children, new DBNProjectDatabases(this, dataSourceRegistry));
        }
        return children;
    }

    public DBNResource findResource(IResource resource)
    {
        List<IResource> path = new ArrayList<>();
        for (IResource parent = resource; !(parent instanceof IProject); parent = parent.getParent()) {
            path.add(0, parent);
        }

        DBNResource resNode = this;
        for (IResource res : path) {
            try {
                resNode.getChildren(VoidProgressMonitor.INSTANCE);
            } catch (DBException e) {
                log.error(e);
            }
            resNode = resNode.getChild(res);
            if (resNode == null) {
                return null;
            }
        }
        return resNode;
    }

    @Override
    protected void handleChildResourceChange(IResourceDelta delta) {
        final String name = delta.getResource().getName();
        if (name.startsWith(DBPDataSourceRegistry.CONFIG_FILE_PREFIX) && name.endsWith(DBPDataSourceRegistry.CONFIG_FILE_EXT)) {
            // DS registry configuration changed
            getDatabases().getDataSourceRegistry().refreshConfig();
        } else {
            super.handleChildResourceChange(delta);
        }
    }
}
