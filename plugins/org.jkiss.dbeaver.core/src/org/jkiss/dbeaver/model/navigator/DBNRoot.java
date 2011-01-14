/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * DBNRoot
 */
public class DBNRoot extends DBNNode implements DBNContainer
{
    private List<DBNProject> projects = new ArrayList<DBNProject>();

    public DBNRoot(DBNModel model)
    {
        super(model);
    }

    void dispose(boolean reflect)
    {
        for (DBNProject project : projects) {
            project.dispose(reflect);
        }
        projects.clear();
    }

    public Object getValueObject()
    {
        return this;
    }

    public Class<IProject> getItemsClass()
    {
        return IProject.class;
    }

    public DBNNode addChildItem(DBRProgressMonitor monitor, Object childObject) throws DBException
    {
        if (childObject instanceof IProject) {
            return addProject((IProject)childObject);
        }
        throw new IllegalArgumentException("Only projects could be added to root node");
    }

    public void removeChildItem(DBNNode item) throws DBException
    {
        if (item instanceof DBNProject) {
            removeProject((DBNProject)item);
        } else {
            throw new IllegalArgumentException("Only projects could be removed from root node");
        }
    }

    public String getNodeName()
    {
        return "#root";
    }

    public String getNodeDescription()
    {
        return "Model root";
    }

    public Image getNodeIcon()
    {
        return null;
    }

    public boolean hasChildren()
    {
        return !projects.isEmpty();
    }

    @Override
    public boolean hasNavigableChildren()
    {
        return hasChildren();
    }

    public List<? extends DBNNode> getChildren(DBRProgressMonitor monitor)
    {
        return projects;
    }

    public String getDefaultCommandId()
    {
        return null;
    }

    public DBNProject getProject(IProject project)
    {
        for (DBNProject node : projects) {
            if (node.getProject() == project) {
                return node;
            }
        }
        return null;
    }

    DBNProject addProject(IProject project)
    {
        DBNProject newNode = new DBNProject(this, project);
        projects.add(newNode);
        return newNode;
    }

    void removeProject(DBNProject project)
    {
        for (Iterator<DBNProject> iter = projects.iterator(); iter.hasNext(); ) {
            DBNProject projectNode = iter.next();
            if (projectNode.getProject() == project) {
                iter.remove();
                projectNode.dispose(true);
                break;
            }
        }
    }

}
