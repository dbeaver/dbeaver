/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.navigator.registry.DBNRegistry;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DBNProject
 */
public class DBNProject extends DBNNode implements DBNNodeWithCache, DBNNodeExtendable {
    private static final Log log = Log.getLog(DBNProject.class);

    private final DBPProject project;
    private List<DBNNode> extraNodes;
    private DBNNode[] children;

    public DBNProject(DBNNode parentNode, DBPProject project) {
        super(parentNode);
        this.project = project;
        if (DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            DBNRegistry.getInstance().extendNode(this, false);
        }
    }

    @NotNull
    public DBPProject getProject() {
        return project;
    }

    public DBNProjectDatabases getDatabases() {
        try {
            for (DBNNode db : getChildren(new VoidProgressMonitor())) {
                if (db instanceof DBNProjectDatabases databases) {
                    return databases;
                }
            }
        } catch (DBException e) {
            throw new IllegalStateException("Can't read project contents", e);
        }
        throw new IllegalStateException("No databases resource in project");
    }

    @NotNull
    @Override
    public String getName() {
        return project.getId();
    }

    @Override
    public String getNodeDisplayName() {
        return project.getDisplayName();
    }

    @Override
    public String getNodeDescription() {
        return null;
    }

    @Override
    public String getLocalizedName(String locale) {
        return getNodeDisplayName();
    }

    @Override
    public String getNodeType() {
        return "project";
    }

    @NotNull
    @Override
    public DBPImage getNodeIcon() {
        DBPImage image = DBIcon.PROJECT;
        if (getProject().isPrivateProject()) {
            image = new DBIconComposite(image, false, null, null, null, DBIcon.OVER_LAMP);
        } else if (!getProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT)) {
            image = new DBIconComposite(image, false, null, null, null, DBIcon.OVER_LOCK);
        }

        return image;
    }

    @Override
    public boolean allowsOpen() {
        return true;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBNProject.class) {
            return adapter.cast(this);
        }
        return super.getAdapter(adapter);
    }

    @Nullable
    @Override
    public DBPProject getOwnerProjectOrNull() {
        return project;
    }

    @Override
    public Throwable getLastLoadError() {
        return getProject().getDataSourceRegistry().getLastError();
    }

    @Override
    public boolean supportsRename() {
        return !project.isVirtual();
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException {
        throw new DBCFeatureNotSupportedException("Project rename is not supported");
    }

    protected DBNNode[] readChildNodes(DBRProgressMonitor monitor) throws DBException {
        if (getModel().isGlobal() && !project.isOpen()) {
            project.ensureOpen();
        }

        final DBPDataSourceRegistry dataSourceRegistry = project.getDataSourceRegistry();

        try {
            dataSourceRegistry.checkForErrors();
        } catch (Throwable e) {
            project.dispose();
            throw e;
        }

        final List<DBNNode> children = new ArrayList<>();

        children.add(new DBNProjectDatabases(this, dataSourceRegistry));
        addProjectNodes(monitor, children);

        if (!CommonUtils.isEmpty(extraNodes)) {
            children.addAll(extraNodes);
        }
        filterChildren(children);
        return children.toArray(DBNNode[]::new);
    }

    protected void addProjectNodes(DBRProgressMonitor monitor, List<DBNNode> children) throws DBException {

    }

    protected void filterChildren(List<DBNNode> children) {
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException {
        project.getDataSourceRegistry().refreshConfig();
        return this;
    }

    public DBNNode findResource(DBRProgressMonitor monitor, Path path) throws DBException {
        Path relativePath = getProject().getAbsolutePath().relativize(path);

        DBNNode resNode = this;
        for (Path fileName : relativePath) {
            resNode = DBUtils.findObject(resNode.getChildren(monitor), fileName.toString());
            if (resNode == null) {
                break;
            }
        }
        return resNode;
    }

    @NotNull
    @Override
    public List<DBNNode> getExtraNodes() {
        if (extraNodes == null) {
            return Collections.emptyList();
        }
        return extraNodes;
    }

    public <T> T getExtraNode(Class<T> nodeType) {
        if (extraNodes == null) {
            DBNRegistry.getInstance().extendNode(this, false);
        }
        if (extraNodes != null) {
            for (DBNNode node : extraNodes) {
                if (nodeType.isAssignableFrom(node.getClass())) {
                    return nodeType.cast(node);
                }
            }
        }
        return null;
    }

    @Override
    public void addExtraNode(@NotNull DBNNode node, boolean reflect) {
        if (extraNodes == null) {
            extraNodes = new ArrayList<>();
        }
        extraNodes.add(node);
        extraNodes.sort(Comparator.comparing(DBNNode::getNodeDisplayName));
        if (reflect) {
            getModel().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, node));
        }
    }

    @Override
    public void removeExtraNode(@NotNull DBNNode node) {
        if (extraNodes != null && extraNodes.remove(node)) {
            getModel().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, node));
        }
    }

    @Override
    protected void dispose(boolean reflect) {
        if (extraNodes != null) {
            for (DBNNode node : extraNodes) {
                DBNUtils.disposeNode(node, reflect);
            }
            extraNodes.clear();
        }
        if (children != null) {
            for (DBNNode child : children) {
                DBNUtils.disposeNode(child, reflect);
            }
            children = null;
        }
        if (reflect) {
            getModel().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, this));
        }
        super.dispose(reflect);
    }

    @NotNull
    @Override
    public String getNodeId() {
        return project.getId();
    }

    @Deprecated
    @Override
    public String getNodeItemPath() {
        return NodePathType.resource.getPrefix() + project.getId();
    }

    @Override
    public boolean hasChildren(boolean navigableOnly) {
        return true;
    }

    @Override
    protected boolean allowsChildren() {
        return true;
    }

    @Override
    public DBNNode[] getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (children != null) {
            return children;
        }
        children = readChildNodes(monitor);
        return children;
    }

    @Override
    public boolean needsInitialization() {
        return children == null;
    }

    @Override
    public DBNNode[] getCachedChildren() {
        return children;
    }

    @Override
    public void setCachedChildren(DBNNode[] children) {
        this.children = children;
    }
}
