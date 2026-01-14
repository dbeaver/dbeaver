/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.app.DBPProjectManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.registry.DBNRegistry;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DBNRoot
 */
public class DBNRoot extends DBNNode implements DBNContainer, DBNNodeExtendable, DBPProjectListener {
    private final DBNModel model;
    private final List<DBNProject> projects = new ArrayList<>();
    private final List<DBNNode> extraNodes = new ArrayList<>();

    public DBNRoot(@NotNull DBNModel model) {
        super();
        this.model = model;
        List<? extends DBPProject> globalProjects = model.getModelProjects();
        if (globalProjects != null) {
            for (DBPProject project : globalProjects) {
                addProject(project, false);
            }
        } else {
            for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
                addProject(project, false);
            }
        }
        if (model.isGlobal()) {
            if (DBWorkbench.getPlatform().getWorkspace() instanceof DBPProjectManager projectManager) {
                projectManager.addProjectListener(this);
            }
        }
        DBNRegistry.getInstance().extendNode(this, false);
    }

    @Override
    protected void dispose(boolean reflect) {
        for (DBNProject project : projects) {
            DBNUtils.disposeNode(project, reflect);
        }
        projects.clear();
        for (DBNNode node : extraNodes) {
            DBNUtils.disposeNode(node, reflect);
        }
        extraNodes.clear();

        if (model.isGlobal()) {
            if (DBWorkbench.getPlatform().getWorkspace() instanceof DBPProjectManager projectManager) {
                projectManager.removeProjectListener(this);
            }
        }
    }

    @Override
    public DBNModel getModel() {
        return model;
    }

    @NotNull
    @Override
    public String getNodeType() {
        return ModelMessages.model_navigator_Root;
    }

    @Nullable
    @Override
    public Object getValueObject() {
        return this;
    }

    @NotNull
    @Override
    public String getChildrenType() {
        return ModelMessages.model_navigator_Project;
    }

    @Nullable
    @Override
    public Class<?> getChildrenClass() {
        return Object.class;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return super.getName();
    }

    @NotNull
    @Override
    public String getNodeDisplayName() {
        return "#root"; //$NON-NLS-1$
    }

    @Nullable
    @Override
    public String getNodeDescription() {
        return ModelMessages.model_navigator_Model_root;
    }

    @Nullable
    @Override
    public DBPImage getNodeIcon() {
        return null;
    }

    @Override
    public boolean allowsChildren() {
        return !projects.isEmpty() || !extraNodes.isEmpty();
    }

    @Nullable
    @Override
    public DBNNode[] getChildren(@NotNull DBRProgressMonitor monitor) {
        if (extraNodes.isEmpty()) {
            return projects.toArray(new DBNNode[0]);
        } else if (projects.isEmpty()) {
            return extraNodes.toArray(new DBNNode[0]);
        } else {
            List<DBNNode> children = new ArrayList<>(projects.size() + extraNodes.size());
            children.addAll(projects);
            children.addAll(extraNodes);
            return children.toArray(new DBNNode[0]);
        }
    }

    @NotNull
    public List<DBNProject> getProjects() {
        return projects;
    }

    @Override
    @NotNull
    public List<DBNNode> getExtraNodes() {
        return extraNodes;
    }

    @Nullable
    @Override
    public DBNNode refreshNode(@NotNull DBRProgressMonitor monitor, @Nullable Object source) throws DBException {
        if (this.getParentNode() != null) {
            return this.getParentNode().refreshNode(monitor, source);
        } else {
            for (DBNProject project : projects) {
                project.refreshNode(monitor, source);
            }
            return this;
        }
    }

    @NotNull
    @Deprecated
    @Override
    public String getNodeItemPath() {
        return "";
    }

    @Nullable
    public DBNProject getProjectNode(@Nullable DBPProject project) {
        if (project == null) {
            return null;
        }
        for (DBNProject node : projects) {
            if (node.getProject().equals(project) ||
                CommonUtils.equalObjects(node.getProject().getId(), project.getId()))
            {
                return node;
            }
        }
        return null;
    }

    public DBNProject addProject(@NotNull DBPProject project, boolean reflect) {
        DBNProject projectNode = getModel().createProjectNode(this, project);
        projects.add(projectNode);
        projects.sort((o1, o2) -> o1.getNodeDisplayName().compareToIgnoreCase(o2.getNodeDisplayName()));

        if (reflect) {
            model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, projectNode));
        }

        return projectNode;
    }

    public void removeProject(@NotNull DBPProject project) {
        for (int i = 0; i < projects.size(); i++) {
            DBNProject projectNode = projects.get(i);
            if (projectNode.getProject() == project) {
                projects.remove(i);
                model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, projectNode));
                DBNUtils.disposeNode(projectNode, true);
                break;
            }
        }
    }

    @Override
    public void addExtraNode(@NotNull DBNNode node, boolean reflect) {
        extraNodes.add(node);
        extraNodes.sort(Comparator.comparing(DBNNode::getNodeDisplayName));
        model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, node));
    }

    @Override
    public void removeExtraNode(@NotNull DBNNode node) {
        if (extraNodes.remove(node)) {
            model.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, node));
        }
    }

    @Override
    public void handleProjectAdd(@NotNull DBPProject project) {
        addProject(project, true);
    }

    @Override
    public void handleProjectRemove(@NotNull DBPProject project) {
        removeProject(project);
    }

    @Override
    public void handleActiveProjectChange(@NotNull DBPProject oldValue, @NotNull DBPProject newValue) {
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
