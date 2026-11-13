/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import java.util.List;

/**
 * DBNProject
 */
public class DBNProject extends DBNNode implements DBNNodeWithCache, DBNNodeExtendable {
    private static final Log log = Log.getLog(DBNProject.class);

    private final DBPProject project;
    private List<DBNNode> extraNodes;
    private DBNNode[] children;
    private boolean disposed;

    public DBNProject(DBNNode parentNode, DBPProject project) {
        super(parentNode);
        this.project = project;
        DBNRegistry.getInstance().extendNode(this, false);
    }

    @NotNull
    public DBPProject getProject() {
        return project;
    }

    public DBNProjectDatabases getDatabases() {
        try {
            DBNNode[] dbNodes = getChildren(new VoidProgressMonitor());
            if (dbNodes != null) {
                for (DBNNode db : dbNodes) {
                    if (db instanceof DBNProjectDatabases databases) {
                        return databases;
                    }
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

    @NotNull
    @Override
    public String getNodeDisplayName() {
        return project.getDisplayName();
    }

    @Nullable
    @Override
    public String getNodeDescription() {
        return null;
    }

    @NotNull
    @Override
    public String getLocalizedName(@NotNull String locale) {
        return getNodeDisplayName();
    }

    @NotNull
    @Override
    public String getNodeType() {
        return "project";
    }

    @Nullable
    @Override
    public DBPImage getNodeIcon() {
        DBPImage image = DBIcon.PROJECT;
        if (DBWorkbench.isDistributed() && getProject().isPrivateProject()) {
            image = new DBIconComposite(image, false, null, null, null, DBIcon.OVER_LAMP);
        } else if (!getProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT)) {
            image = new DBIconComposite(image, false, null, null, null, DBIcon.OVER_LOCK);
        }

        return image;
    }

    @Override
    public <T> T getAdapter(@NotNull Class<T> adapter) {
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

    @Nullable
    @Override
    public Throwable getLastLoadError() {
        return getProject().getDataSourceRegistry().getLastError();
    }

    @Override
    public boolean supportsRename() {
        return DBWorkbench.isDistributed() || !project.isVirtual();
    }

    @Override
    public void rename(@NotNull DBRProgressMonitor monitor, @NotNull String newName) throws DBException {
        throw new DBCFeatureNotSupportedException("Project rename is not supported");
    }

    @NotNull
    protected DBNNode[] readChildNodes(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (getModel().isGlobal() && !project.isOpen()) {
            project.ensureOpen();
        }

        List<DBNNode> children = new ArrayList<>();
        try {
            DBPDataSourceRegistry dataSourceRegistry = project.getDataSourceRegistry();

            dataSourceRegistry.checkForErrors();

            children.add(new DBNProjectDatabases(this, dataSourceRegistry));
            addProjectNodes(monitor, children);

            filterChildren(children);
            return children.toArray(DBNNode[]::new);
        } catch (Throwable e) {
            // These nodes have not been published in the cache yet.
            for (DBNNode child : children) {
                try {
                    DBNUtils.disposeNode(child, false);
                } catch (Throwable disposeError) {
                    e.addSuppressed(disposeError);
                }
            }
            project.dispose();
            if (e instanceof DBException) {
                throw e;
            } else {
                throw new DBException("Internal error", e);
            }
        }
    }

    protected void addProjectNodes(DBRProgressMonitor monitor, List<DBNNode> children) throws DBException {

    }

    protected void filterChildren(List<DBNNode> children) {
    }

    @Nullable
    @Override
    public DBNNode refreshNode(@NotNull DBRProgressMonitor monitor, @Nullable Object source) throws DBException {
        project.getDataSourceRegistry().refreshConfig();
        return this;
    }

    @Nullable
    public DBNNode findNodeByRelativePath(@NotNull DBRProgressMonitor monitor, @NotNull String path) throws DBException {
        var model = getModel();
        var projectPath = model.toProjectPath(project, path);
        if (projectPath != null) {
            return model.getNodeByPath(monitor, project, projectPath);
        }
        return null;
    }

    @Nullable
    public DBNNode findResource(@NotNull DBRProgressMonitor monitor, @NotNull Path path) throws DBException {
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

    @Nullable
    public <T> T getExtraNode(@NotNull Class<T> nodeType) {
        if (extraNodes != null) {
            for (DBNNode node : extraNodes) {
                if (nodeType.isAssignableFrom(node.getClass())) {
                    return nodeType.cast(node);
                } else if (node instanceof DBNNodeExtension nodeExtension && nodeExtension.matchesType(nodeType)) {
                    return nodeType.cast(nodeExtension.resolveRealNode());
                }
            }
        }
        log.error("Cannot determine model extender for type '" + nodeType + "'");
        return null;
    }

    @Override
    public void addExtraNode(@NotNull DBNNodeExtension node, boolean reflect) {
        if (extraNodes == null) {
            extraNodes = new ArrayList<>();
        }
        extraNodes.add(node);
        if (DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            node.resolveRealNode();
        }
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

    @NotNull
    @Override
    public DBNNode resolveTargetNode(@NotNull DBNNodeExtension sourceNode, @NotNull DBNNode targetNode) {
        int index = extraNodes.indexOf(sourceNode);
        if (index < 0) {
            throw new IndexOutOfBoundsException("Source extension node '" + sourceNode + "' not found in '" + this + "'");
        }
        extraNodes.set(index, targetNode);
        return targetNode;
    }

    @Override
    protected synchronized void dispose(boolean reflect) {
        if (disposed) {
            return;
        }
        disposed = true;
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

    @Override
    public synchronized boolean isDisposed() {
        return disposed;
    }

    @NotNull
    @Override
    public String getNodeId() {
        return project.getId();
    }

    @Override
    public boolean hasChildren(boolean navigableOnly) {
        return true;
    }

    @Override
    protected boolean allowsChildren() {
        return true;
    }

    @Nullable
    @Override
    public DBNNode[] getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        DBNNode[] childNodes = getRegularChildNodes(monitor);
        if (!CommonUtils.isEmpty(extraNodes)) {
            DBNNode[] result = new DBNNode[childNodes.length + extraNodes.size()];
            System.arraycopy(childNodes, 0, result, 0, childNodes.length);
            for (int i = 0; i < extraNodes.size(); i++) {
                result[childNodes.length + i] = extraNodes.get(i);
            }
            return result;
        }
        return childNodes;
    }

    @NotNull
    private DBNNode[] getRegularChildNodes(@NotNull DBRProgressMonitor monitor) throws DBException {
        synchronized (this) {
            if (disposed) {
                throw new DBException("Project navigator node is disposed");
            }
            if (children != null) {
                return children;
            }
        }

        // Registry initialization can synchronously request a password on the UI thread.
        // Do not hold the navigator lock: the UI may query this node while loading.
        try {
            if (getModel().isGlobal() && !project.isOpen()) {
                project.ensureOpen();
            }
            project.getDataSourceRegistry();
        } catch (Throwable e) {
            project.dispose();
            throw new DBException("Internal error", e);
        }

        synchronized (this) {
            if (disposed) {
                throw new DBException("Project navigator node is disposed");
            }
            if (children != null) {
                return children;
            }
            DBNNode[] childNodes = readChildNodes(monitor);
            // Loading can invoke callbacks which dispose this node reentrantly.
            if (disposed) {
                for (DBNNode child : childNodes) {
                    DBNUtils.disposeNode(child, false);
                }
                throw new DBException("Project navigator node is disposed");
            }
            children = childNodes;
            return children;
        }
    }

    @Override
    public synchronized boolean needsInitialization() {
        return children == null;
    }

    @Override
    @Nullable
    public synchronized DBNNode[] getCachedChildren() {
        return children;
    }

    @Override
    public synchronized void setCachedChildren(@Nullable DBNNode[] children) {
        if (!disposed) {
            this.children = children;
        }
    }
}
