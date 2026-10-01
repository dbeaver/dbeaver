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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * DBNNode
 */
public abstract class DBNNode implements DBPNamedObject, DBPNamedObjectLocalized, DBPObjectWithDescription, DBPPersistedObject, DBPAdaptable {
    static final Log log = Log.getLog(DBNNode.class);

    public static final String NODE_URI_PREFIX= "node://";

    protected final DBNNode parentNode;

    protected DBNNode() {
        this.parentNode = null;
    }

    protected DBNNode(DBNNode parentNode) {
        this.parentNode = parentNode;
    }

    public boolean isDisposed() {
        return false;
    }

    protected void dispose(boolean reflect) {
    }

    public DBNModel getModel() {
        return parentNode == null ? null : parentNode.getModel();
    }

    @Nullable
    public DBNNode getParentNode() {
        return parentNode;
    }

    public boolean isLocked() {
        return getParentNode() != null && getParentNode().isLocked();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    public boolean isManageable() {
        return false;
    }

    /**
     * Unique identifier of a node within its parent.
     */
    @NotNull
    public String getNodeId() {
        return getName();
    }

    @NotNull
    @Override
    public String getName() {
        return getNodeDisplayName();
    }

    /**
     * Internal node name. Usually it is the same as getName.
     */
    @NotNull
    public abstract String getNodeDisplayName();

    @NotNull
    @Override
    public String getLocalizedName(@NotNull String locale) {
        return getName();
    }

    /**
     * Node type. May be used internally.
     */
    @NotNull
    public abstract String getNodeType();

    /**
     * Node type in display format.
     */
    @NotNull
    public String getNodeTypeLabel() {
        return getNodeType();
    }

    @Nullable
    public String getNodeBriefInfo() {
        return null;
    }

    @Nullable
    public abstract String getNodeDescription();

    @Nullable
    public abstract DBPImage getNodeIcon();

    @Nullable
    @Override
    public String getDescription() {
        return getNodeDescription();
    }

    @NotNull
    public DBPImage getNodeIconDefault() {
        DBPImage image = getNodeIcon();
        if (image == null) {
            if (this.hasChildren(false)) {
                return DBIcon.TREE_FOLDER;
            } else {
                return DBIcon.TREE_PAGE;
            }
        } else {
            return image;
        }
    }

    @NotNull
    public String getNodeFullName() {
        StringBuilder pathName = new StringBuilder();
        pathName.append(getNodeDisplayName());

        for (DBNNode parent = getParentNode(); parent != null && !(parent instanceof DBNDataSource); parent = parent.getParentNode()) {
            if (parent instanceof DBNDatabaseFolder) {
                // skip folders
                continue;
            }
            String parentName = parent.getNodeDisplayName();
            if (!CommonUtils.isEmpty(parentName)) {
                pathName.insert(0, '.').insert(0, parentName);
            }
        }
        return pathName.toString();
    }

    /**
     * Used to copy target name in clipboard and in DND operations.
     * Equals to regular node name by default.
     */
    @NotNull
    public String getNodeTargetName() {
        return getNodeDisplayName();
    }

    public boolean hasChildren(boolean navigableOnly) {
        return navigableOnly ? allowsNavigableChildren() : allowsChildren();
    }

    protected abstract boolean allowsChildren();

    protected boolean allowsNavigableChildren() {
        return allowsChildren();
    }

    @Nullable
    public abstract DBNNode[] getChildren(@NotNull DBRProgressMonitor monitor) throws DBException;

    void clearNode(boolean reflect) {

    }

    public boolean supportsRename() {
        return false;
    }

    public void rename(@NotNull DBRProgressMonitor monitor, @NotNull String newName) throws DBException {
        throw new DBException("Rename is not supported");
    }

    public boolean supportsDrop(@Nullable DBNNode otherNode) {
        return false;
    }

    public void dropNodes(@NotNull DBRProgressMonitor monitor, @NotNull Collection<DBNNode> nodes) throws DBException {
        throw new DBException("Drop is not supported");
    }

    /**
     * Refreshes node.
     * If refresh cannot be done in this level then refreshes parent node.
     * Do not actually change navigation tree. If some underlying object is refreshed it must fire DB model
     * event which will cause actual tree nodes refresh. Underlying object could present multiple times in
     * navigation model - each occurrence will be refreshed then.
     *
     * @param monitor progress monitor
     * @param source  event source
     * @return real refreshed node or null if nothing was refreshed
     * @throws DBException on any internal exception
     */
    @Nullable
    public DBNNode refreshNode(@NotNull DBRProgressMonitor monitor, @Nullable Object source) throws DBException {
        if (this.getParentNode() != null) {
            return this.getParentNode().refreshNode(monitor, source);
        } else {
            return null;
        }
    }

    public boolean allowsOpen() {
        return true;
    }

    public boolean isChildOf(DBNNode node) {
        for (DBNNode parent = getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (parent == node) {
                return true;
            }
        }
        return false;
    }

    public boolean isFiltered() {
        return false;
    }

    /**
     * Node uri path in form [node://]<parentPath>/<path>
     *
     * @return a unique path to the node containing information about the reals hierarchy
     */
    @NotNull
    public final String getNodeUri() {
        var pathBuilder = new StringBuilder();
        var currentNode = this;
        while (currentNode != null && !(currentNode instanceof DBNRoot)) {
            if (!pathBuilder.isEmpty()) {
                pathBuilder.insert(0, '/');
            }
            String nodeId = currentNode.getNodeId();
            nodeId = DBNUtils.encodeNodePath(nodeId);
            pathBuilder.insert(0, nodeId);
            if (currentNode instanceof DBNLocalFolder folder) {
                // FIXME: When traversing to root, nested folders are skipped. This is a workaround so that we don't skip them.
                currentNode = folder.getLogicalParent();
            } else {
                currentNode = currentNode.getParentNode();
            }
        }

        return DBNNode.NODE_URI_PREFIX + pathBuilder;
    }

    @Override
    public <T> T getAdapter(@NotNull Class<T> adapter) {
        return null;
    }

    @Nullable
    public DBPProject getOwnerProjectOrNull() {
        for (DBNNode node = getParentNode(); node != null; node = node.getParentNode()) {
            if (node instanceof DBNProject nodeProject) {
                return nodeProject.getProject();
            }
        }
        return null;
    }

    @NotNull
    public DBPWorkspace getOwnerWorkspace() {
        DBPProject project = getOwnerProjectOrNull();
        if (project != null) {
            return project.getWorkspace();
        }
        DBPWorkspace modelWorkspace = getModel().getModelWorkspace();
        if (modelWorkspace != null) {
            return modelWorkspace;
        }
        return DBWorkbench.getPlatform().getWorkspace();
    }

    @NotNull
    public DBPProject getOwnerProject() {
        DBPProject project = getOwnerProjectOrNull();
        if (project == null) {
            throw new IllegalStateException("Node doesn't have owner project");
        }
        return project;
    }

    @Nullable
    public Throwable getLastLoadError() {
        return null;
    }

    static void sortNodes(@NotNull List<? extends DBNNode> nodes) {
        nodes.sort((Comparator<DBNNode>) (o1, o2) -> {
            boolean isFolder1 = o1 instanceof DBNLocalFolder;
            boolean isFolder2 = o2 instanceof DBNLocalFolder;
            if (isFolder1 && !isFolder2) {
                return -1;
            } else if (!isFolder1 && isFolder2) {
                return 1;
            }
            String odn1 = o1.getNodeDisplayName();
            String odn2 = o2.getNodeDisplayName();
            return CommonUtils.notEmpty(odn1).compareToIgnoreCase(CommonUtils.notEmpty(odn2));
        });
    }

    protected void fireNodeEvent(final DBNEvent event) {
        getModel().fireNodeEvent(event);
    }

    public static Class<? extends DBSObject> getFolderChildrenClass(DBXTreeFolder meta) {
        String itemsType = CommonUtils.toString(meta.getType());
        if (CommonUtils.isEmpty(itemsType)) {
            return null;
        }
        Class<DBSObject> aClass = meta.getSource().getObjectClass(itemsType, DBSObject.class);
        if (aClass == null) {
            log.error("Items class '" + itemsType + "' not found");
            return null;
        }
        if (!DBSObject.class.isAssignableFrom(aClass)) {
            log.error("Class '" + aClass.getName() + "' doesn't extend DBSObject");
            return null;
        }
        return aClass;
    }

    public static boolean nodeHasStructureContainers(@NotNull DBNNode node, @NotNull DBXTreeNode meta) {
        List<DBXTreeNode> children = meta.getChildren(node);
        if (!CommonUtils.isEmpty(children)) {
            for (DBXTreeNode child : children) {
                if (child instanceof DBXTreeFolder tf) {
                    Class<? extends DBSObject> childrenClass = DBNNode.getFolderChildrenClass(tf);
                    if (childrenClass != null && DBSObjectContainer.class.isAssignableFrom(childrenClass)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    @NotNull
    public String toString() {
        return getNodeUri();
    }
}
