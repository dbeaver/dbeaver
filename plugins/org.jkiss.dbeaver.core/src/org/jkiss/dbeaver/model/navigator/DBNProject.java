/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.List;

/**
 * DBNProject
 */
public class DBNProject extends DBNResource implements IAdaptable
{
    private DBNProjectDatabases databases;
    //private DBNProjectScripts scripts;
    //private DBNProjectBookmarks bookmarks;

    public DBNProject(DBNNode parentNode, IProject project)
    {
        super(parentNode, project);
        this.databases = new DBNProjectDatabases(this, project);
        //this.scripts = new DBNProjectScripts(this, project);
        //this.bookmarks = new DBNProjectBookmarks(this, project);

        //this.children = new ArrayList<DBNNode>();
        //this.children.add(databases);
        //this.children.add(scripts);
        //this.children.add(bookmarks);
    }

    protected void dispose(boolean reflect)
    {
        this.databases = null;
        super.dispose(reflect);
    }

    public IProject getProject()
    {
        return (IProject)getResource();
    }

    public DBNProjectDatabases getDatabases()
    {
        return databases;
    }

    public String getNodeDescription()
    {
        try {
            return getProject().getDescription().getComment();
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
    protected void addCustomChildren(List<DBNNode> list)
    {
        list.add(0, databases);
    }

    public String getDefaultCommandId()
    {
        return null;
    }

    public Object getAdapter(Class adapter) {
        if (adapter == DBNProject.class) {
            return this;
        } else if (adapter == IProject.class) {
            return getProject();
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
            final IProjectDescription description = getProject().getDescription();
            description.setName(newName);
            getProject().move(description, true, monitor.getNestedMonitor());
        } catch (CoreException e) {
            throw new DBException(e);
        }
    }

}
