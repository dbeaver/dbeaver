/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * DBNNode
 */
public abstract class DBNNode implements DBPNamedObject, DBPNamedObjectLocalized, DBPPersistedObject, IAdaptable {
    static final Log log = Log.getLog(DBNNode.class);

    public enum NodePathType {
        resource,
        folder,
        database,
        ext,
        other;

        public String getPrefix() {
            return name() + "://";
        }
    }

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

    public boolean isManagable() {
        return false;
    }

    @NotNull
    @Override
    public String getName() {
        return getNodeName();
    }

    @Override
    public String getLocalizedName(String locale) {
        return getName();
    }

    public abstract String getNodeType();

    public abstract String getNodeName();

    @Nullable
    public String getNodeBriefInfo() {
        return null;
    }

    public abstract String getNodeDescription();

    public abstract DBPImage getNodeIcon();

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

    public String getNodeFullName() {
        StringBuilder pathName = new StringBuilder();
        pathName.append(getNodeName());

        for (DBNNode parent = getParentNode(); parent != null && !(parent instanceof DBNDataSource); parent = parent.getParentNode()) {
            if (parent instanceof DBNDatabaseFolder) {
                // skip folders
                continue;
            }
            String parentName = parent.getNodeName();
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
    public String getNodeTargetName() {
        return getNodeName();
    }

    public boolean hasChildren(boolean navigableOnly) {
        return navigableOnly ? allowsNavigableChildren() : allowsChildren();
    }

    protected abstract boolean allowsChildren();

    protected boolean allowsNavigableChildren() {
        return allowsChildren();
    }

    public abstract DBNNode[] getChildren(DBRProgressMonitor monitor) throws DBException;

    void clearNode(boolean reflect) {

    }

    public boolean supportsRename() {
        return false;
    }

    public void rename(DBRProgressMonitor monitor, String newName) throws DBException {
        throw new DBException("Rename is not supported");
    }

    public boolean supportsDrop(DBNNode otherNode) {
        return false;
    }

    public void dropNodes(Collection<DBNNode> nodes) throws DBException {
        throw new DBException("Drop is not supported");
    }

    /**
     * Refreshes node.
     * If refresh cannot be done in this level then refreshes parent node.
     * Do not actually changes navigation tree. If some underlying object is refreshed it must fire DB model
     * event which will cause actual tree nodes refresh. Underlying object could present multiple times in
     * navigation model - each occurrence will be refreshed then.
     *
     * @param monitor progress monitor
     * @param source  event source
     * @return real refreshed node or null if nothing was refreshed
     * @throws DBException on any internal exception
     */
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException {
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
     * Node item path in form [nodeType://]<path>
     * nodeType can be 'resource', 'folder' or 'database'.
     * If missing then 'database' will be used (backward compatibility).
     * <p>
     * For resources and folders path is just a hierarchy path divided with / (slash).
     * <p>
     * For database nodes path has form: type1=name1/type2=name2/...[/typeX]
     * Where typeN is path element for particular database item, name is database object name.
     *
     * @return full item node path
     */
    public abstract String getNodeItemPath();

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (IProject.class.isAssignableFrom(adapter)) {
            // Do not adapt to IProject.
            // It brings a lot of Eclipse preferences/props to link to navigator nodes. We don't need them.
            //return adapter.cast(getOwnerProject().getEclipseProject());
        }

        return null;
    }

    public DBPProject getOwnerProject() {
        for (DBNNode node = getParentNode(); node != null; node = node.getParentNode()) {
            if (node instanceof DBNProject) {
                return ((DBNProject) node).getProject();
            }
        }
        return null;
    }

    static void sortNodes(List<? extends DBNNode> nodes) {
        nodes.sort((Comparator<DBNNode>) (o1, o2) -> {
            boolean isFolder1 = o1 instanceof DBNLocalFolder;
            boolean isFolder2 = o2 instanceof DBNLocalFolder;
            if (isFolder1 && !isFolder2) {
                return -1;
            } else if (!isFolder1 && isFolder2) {
                return 1;
            }
            return o1.getName().compareToIgnoreCase(o2.getName());
        });
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


}
