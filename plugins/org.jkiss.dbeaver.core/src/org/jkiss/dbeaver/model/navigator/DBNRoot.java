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
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPProjectListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.*;

/**
 * DBNRoot
 */
public class DBNRoot extends DBNNode implements DBNContainer, DBPProjectListener
{
    private List<DBNProject> projects = new ArrayList<DBNProject>();

    public DBNRoot()
    {
        super();
        DBeaverCore.getInstance().getProjectRegistry().addProjectListener(this);
    }

    @Override
    void dispose(boolean reflect)
    {
        for (DBNProject project : projects) {
            project.dispose(reflect);
        }
        projects.clear();
        DBeaverCore.getInstance().getProjectRegistry().removeProjectListener(this);
    }

    @Override
    public String getNodeType()
    {
        return CoreMessages.model_navigator_Root;
    }

    @Override
    public Object getValueObject()
    {
        return this;
    }

    @Override
    public String getChildrenType()
    {
        return CoreMessages.model_navigator_Project;
    }

    @Override
    public Class<IProject> getChildrenClass()
    {
        return IProject.class;
    }

    @Override
    public String getNodeName()
    {
        return "#root"; //$NON-NLS-1$
    }

    @Override
    public String getNodeDescription()
    {
        return CoreMessages.model_navigator_Model_root;
    }

    @Override
    public Image getNodeIcon()
    {
        return null;
    }

    @Override
    public boolean allowsChildren()
    {
        return !projects.isEmpty();
    }

    @Override
    public boolean allowsNavigableChildren()
    {
        return allowsChildren();
    }

    @Override
    public List<? extends DBNNode> getChildren(DBRProgressMonitor monitor)
    {
        return projects;
    }

    @Override
    public boolean allowsOpen()
    {
        return false;
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

    DBNProject addProject(IProject project, boolean reflect)
    {
        DBNProject projectNode = new DBNProject(
            this,
            project,
            DBeaverCore.getInstance().getProjectRegistry().getResourceHandler(project));
        projects.add(projectNode);
        Collections.sort(projects, new Comparator<DBNProject>() {
            @Override
            public int compare(DBNProject o1, DBNProject o2)
            {
                return o1.getNodeName().compareTo(o2.getNodeName());
            }
        });
        DBNModel.getInstance().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, projectNode));

        return projectNode;
    }

    void removeProject(IProject project)
    {
        for (Iterator<DBNProject> iter = projects.iterator(); iter.hasNext(); ) {
            DBNProject projectNode = iter.next();
            if (projectNode.getProject() == project) {
                iter.remove();
                DBNModel.getInstance().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, projectNode));
                projectNode.dispose(true);
                break;
            }
        }
    }

    @Override
    public void handleActiveProjectChange(IProject oldValue, IProject newValue)
    {
        DBNProject projectNode = getProject(newValue);
        DBNProject oldProjectNode = getProject(oldValue);
        if (projectNode != null) {
            DBNModel.getInstance().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.UPDATE, projectNode));
        }
        if (oldProjectNode != null) {
            DBNModel.getInstance().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.UPDATE, oldProjectNode));
        }
    }
}
