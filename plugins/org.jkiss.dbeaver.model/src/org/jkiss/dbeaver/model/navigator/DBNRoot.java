/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IProject;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Comparator;

/**
 * DBNRoot
 */
public class DBNRoot extends DBNNode implements DBNContainer, DBPProjectListener
{
    private final DBNModel model;
    private DBNProject[] projects = new DBNProject[0];

    public DBNRoot(DBNModel model)
    {
        super();
        this.model = model;
        model.getPlatform().getProjectManager().addProjectListener(this);
    }

    @Override
    void dispose(boolean reflect)
    {
        for (DBNProject project : projects) {
            project.dispose(reflect);
        }
        projects = new DBNProject[0];
        model.getPlatform().getProjectManager().removeProjectListener(this);
    }

    @Override
    public DBNModel getModel() {
        return model;
    }

    @Override
    public String getNodeType()
    {
        return ModelMessages.model_navigator_Root;
    }

    @Override
    public Object getValueObject()
    {
        return this;
    }

    @Override
    public String getChildrenType()
    {
        return ModelMessages.model_navigator_Project;
    }

    @Override
    public Class<IProject> getChildrenClass()
    {
        return IProject.class;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return super.getName();
    }

    @Override
    public String getNodeName()
    {
        return "#root"; //$NON-NLS-1$
    }

    @Override
    public String getNodeDescription()
    {
        return ModelMessages.model_navigator_Model_root;
    }

    @Override
    public DBPImage getNodeIcon()
    {
        return null;
    }

    @Override
    public boolean allowsChildren()
    {
        return projects.length > 0;
    }

    @Override
    public DBNProject[] getChildren(DBRProgressMonitor monitor)
    {
        return projects;
    }

    public DBNProject[] getProjects() {
        return projects;
    }

    @Override
    public boolean allowsOpen()
    {
        return true;
    }

    @Override
    public String getNodeItemPath() {
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

    DBNProject addProject(IProject project, boolean reflect)
    {
        DBNProject projectNode = new DBNProject(
            this,
            project,
            model.getPlatform().getProjectManager().getResourceHandler(project));
        projects = ArrayUtils.add(DBNProject.class, projects, projectNode);
        Arrays.sort(projects, new Comparator<DBNProject>() {
            @Override
            public int compare(DBNProject o1, DBNProject o2)
            {
                return o1.getNodeName().compareTo(o2.getNodeName());
            }
        });
        model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, projectNode));

        return projectNode;
    }

    void removeProject(IProject project)
    {
        for (int i = 0; i < projects.length; i++) {
            DBNProject projectNode = projects[i];
            if (projectNode.getProject() == project) {
                projects = ArrayUtils.remove(DBNProject.class, projects, i);
                model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, projectNode));
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
            model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.UPDATE, projectNode));
        }
        if (oldProjectNode != null) {
            model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.UPDATE, oldProjectNode));
        }
    }
}
