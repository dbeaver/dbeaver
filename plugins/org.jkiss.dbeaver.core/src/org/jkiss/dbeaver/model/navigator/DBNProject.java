/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * DBNProject
 */
public class DBNProject extends DBNNode implements IAdaptable, DBNResource
{
    private IProject project;
    private List<DBNNode> children;
    private DBNProjectDatabases databases;

    public DBNProject(DBNNode parentNode, IProject project)
    {
        super(parentNode);
        this.project = project;
        this.databases = new DBNProjectDatabases(this, project);
        this.children = new ArrayList<DBNNode>();
        this.children.add(databases);
    }

    protected void dispose(boolean reflect)
    {
        this.project = null;
        if (children != null) {
            for (DBNNode child : children) {
                child.dispose(reflect);
            }
            children = null;
        }
        this.databases = null;
        super.dispose(reflect);
    }

    public IProject getProject()
    {
        return project;
    }

    public DBNProjectDatabases getDatabases()
    {
        return databases;
    }

    public String getNodeName()
    {
        return project.getName();
    }

    public String getNodeDescription()
    {
        try {
            return project.getDescription().getComment();
        } catch (CoreException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public Image getNodeIcon()
    {
        return DBIcon.PROJECT.getImage();
    }

    @Override
    public boolean hasChildren()
    {
        return true;
    }

    @Override
    public boolean hasNavigableChildren()
    {
        return true;
    }

    @Override
    public List<? extends DBNNode> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return children;
    }

    public String getDefaultCommandId()
    {
        return null;
    }

    public Object getAdapter(Class adapter) {
        if (adapter == DBNProject.class) {
            return this;
        } else if (adapter == IProject.class) {
            return project;
        }
        return null;
    }

    public boolean supportsRename()
    {
        return true;
    }

    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        try {
            final IProjectDescription description = project.getDescription();
            description.setName(newName);
            project.move(description, true, monitor.getNestedMonitor());
        } catch (CoreException e) {
            throw new DBException(e);
        }
    }

    public IResource getResource()
    {
        return project;
    }
}
