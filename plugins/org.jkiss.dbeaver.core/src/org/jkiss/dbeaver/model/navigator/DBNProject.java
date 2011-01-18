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
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DBNProject
 */
public class DBNProject extends DBNResource implements IAdaptable
{
    //private DBNProjectDatabases databases;
    //private DBNProjectScripts scripts;
    //private DBNProjectBookmarks bookmarks;

    public DBNProject(DBNNode parentNode, IProject project)
    {
        super(parentNode, project);
        //this.databases = new DBNProjectDatabases(this, project);
        //this.scripts = new DBNProjectScripts(this, project);
        //this.bookmarks = new DBNProjectBookmarks(this, project);

        //this.children = new ArrayList<DBNNode>();
        //this.children.add(databases);
        //this.children.add(scripts);
        //this.children.add(bookmarks);
    }

    protected void dispose(boolean reflect)
    {
        //this.databases = null;
        super.dispose(reflect);
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
    protected void filterChildren(List<DBNNode> list)
    {
        Collections.sort(list, new Comparator<DBNNode>() {
            public int compare(DBNNode o1, DBNNode o2)
            {
                if (o1 instanceof DBNProjectDatabases) {
                    return -1;
                } else if (o2 instanceof DBNProjectDatabases) {
                    return 1;
                } else {
                    return o1.getNodeName().compareTo(o2.getNodeName());
                }
            }
        });
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
