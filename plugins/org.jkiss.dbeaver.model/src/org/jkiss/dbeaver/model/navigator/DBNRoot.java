/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.registry.DBNRegistry;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * DBNRoot
 */
public class DBNRoot extends DBNNode implements DBNContainer, DBNNodeExtendable, DBPProjectListener {
    private final DBNModel model;
    private DBNProject[] projects = new DBNProject[0];
    private List<DBNNode> extraNodes = new ArrayList<>();

    public DBNRoot(DBNModel model) {
        super();
        this.model = model;
        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            addProject(project, false);
        }
        if (model.isGlobal()) {
            model.getPlatform().getWorkspace().addProjectListener(this);
        }
        DBNRegistry.getInstance().extendNode(this);
    }

    @Override
    protected void dispose(boolean reflect) {
        for (DBNProject project : projects) {
            project.dispose(reflect);
        }
        projects = new DBNProject[0];
        for (DBNNode node : extraNodes) {
            node.dispose(reflect);
        }
        extraNodes.clear();

        if (model.isGlobal()) {
            model.getPlatform().getWorkspace().removeProjectListener(this);
        }
    }

    @Override
    public DBNModel getModel() {
        return model;
    }

    @Override
    public String getNodeType() {
        return ModelMessages.model_navigator_Root;
    }

    @Override
    public Object getValueObject() {
        return this;
    }

    @Override
    public String getChildrenType() {
        return ModelMessages.model_navigator_Project;
    }

    @Override
    public Class<IProject> getChildrenClass() {
        return IProject.class;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return super.getName();
    }

    @Override
    public String getNodeName() {
        return "#root"; //$NON-NLS-1$
    }

    @Override
    public String getNodeDescription() {
        return ModelMessages.model_navigator_Model_root;
    }

    @Override
    public DBPImage getNodeIcon() {
        return null;
    }

    @Override
    public boolean allowsChildren() {
        return projects.length > 0 || !extraNodes.isEmpty();
    }

    @Override
    public DBNNode[] getChildren(DBRProgressMonitor monitor) {
        if (extraNodes.isEmpty()) {
            return projects;
        } else if (projects.length == 0) {
            return extraNodes.toArray(new DBNNode[0]);
        } else {
            DBNNode[] children = new DBNNode[extraNodes.size() + projects.length];
            System.arraycopy(projects, 0, children, 0, projects.length);
            for (int i = 0; i < extraNodes.size(); i++) {
                children[projects.length + i] = extraNodes.get(i);
            }
            return children;
        }
    }

    public DBNProject[] getProjects() {
        return projects;
    }

    public List<DBNNode> getExtraNodes() {
        return extraNodes;
    }

    @Override
    public boolean allowsOpen() {
        return true;
    }

    @Override
    public String getNodeItemPath() {
        return "";
    }

    public DBNProject getProjectNode(IProject project) {
        for (DBNProject node : projects) {
            if (node.getProject().getEclipseProject() == project) {
                return node;
            }
        }
        return null;
    }

    public DBNProject getProjectNode(DBPProject project) {
        for (DBNProject node : projects) {
            if (node.getProject() == project) {
                return node;
            }
        }
        return null;
    }

    public DBNProject addProject(DBPProject project, boolean reflect) {
        DBNProject projectNode = new DBNProject(
            this,
            project,
            project.getWorkspace().getResourceHandler(project.getEclipseProject()));
        projects = ArrayUtils.add(DBNProject.class, projects, projectNode);
        Arrays.sort(projects, Comparator.comparing(DBNResource::getNodeName));
        model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, projectNode));

        return projectNode;
    }

    public void removeProject(DBPProject project) {
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
    public void addExtraNode(@NotNull DBNNode node) {
        extraNodes.add(node);
        extraNodes.sort(Comparator.comparing(DBNNode::getNodeName));
        model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, node));
    }

    @Override
    public void removeExtraNode(@NotNull DBNNode node) {
        if (extraNodes.remove(node)) {
            model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, node));
        }
    }

    @Override
    public void handleProjectAdd(DBPProject project) {
        addProject(project, true);
    }

    @Override
    public void handleProjectRemove(DBPProject project) {
        removeProject(project);
    }

    @Override
    public void handleActiveProjectChange(DBPProject oldValue, DBPProject newValue) {
        DBNProject projectNode = getProjectNode(newValue);
        DBNProject oldProjectNode = getProjectNode(oldValue);
        if (projectNode != null) {
            model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.UPDATE, projectNode));
        }
        if (oldProjectNode != null) {
            model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.UPDATE, oldProjectNode));
        }
    }
}
