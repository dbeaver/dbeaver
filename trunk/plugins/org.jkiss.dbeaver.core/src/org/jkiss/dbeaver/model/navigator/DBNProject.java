/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * DBNProject
 */
public class DBNProject extends DBNResource implements IAdaptable
{
    public DBNProject(DBNNode parentNode, IProject project, DBPResourceHandler handler)
    {
        super(parentNode, project, handler);
        DBeaverCore.getInstance().getProjectRegistry().addProject(project);
    }

    @Override
    protected void dispose(boolean reflect)
    {
        IProject project = getProject();
        super.dispose(reflect);
        DBeaverCore.getInstance().getProjectRegistry().removeProject(project);
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
    public Image getNodeIcon()
    {
        return DBIcon.PROJECT.getImage();
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
        return DBeaverCore.getInstance().getProjectRegistry().getActiveProject() != getProject();
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
    protected List<DBNNode> readChildNodes(DBRProgressMonitor monitor) throws DBException
    {
        if (!getProject().isOpen()) {
            try {
                getProject().open(monitor.getNestedMonitor());
            } catch (CoreException e) {
                throw new DBException("Can't open project '" + getProject().getName() + "'", e);
            }
        }
        DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(getProject());
        List<DBNNode> children = super.readChildNodes(monitor);
        if (dataSourceRegistry != null) {
            children.add(0, new DBNProjectDatabases(this, dataSourceRegistry));
        }
        return children;
    }

    public DBNResource findResource(IResource resource)
    {
        List<IResource> path = new ArrayList<IResource>();
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
}
