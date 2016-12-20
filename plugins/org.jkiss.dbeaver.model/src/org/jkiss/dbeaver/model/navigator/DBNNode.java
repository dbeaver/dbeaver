/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DBNNode
 */
public abstract class DBNNode implements DBPNamedObject, DBPPersistedObject, IAdaptable
{
    static final Log log = Log.getLog(DBNNode.class);

    public enum NodePathType {
        resource,
        folder,
        database;

        public String getPrefix() {
            return name() + "://";
        }


    }

    protected final DBNNode parentNode;

    protected DBNNode()
    {
        this.parentNode = null;
    }

    protected DBNNode(DBNNode parentNode)
    {
        this.parentNode = parentNode;
    }

    public boolean isDisposed()
    {
        return false;
    }

    void dispose(boolean reflect)
    {
    }

    public DBNModel getModel() {
        return parentNode == null ? null : parentNode.getModel();
    }

    public DBNNode getParentNode()
    {
        return parentNode;
    }

    public boolean isLocked()
    {
        return getParentNode() != null && getParentNode().isLocked();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    public boolean isManagable()
    {
        return false;
    }

    @NotNull
    @Override
    public String getName()
    {
        return getNodeName();
    }

    public abstract String getNodeType();

    public abstract String getNodeName();

    public abstract String getNodeDescription();

    public abstract DBPImage getNodeIcon();

    @NotNull
    public DBPImage getNodeIconDefault()
    {
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

    public String getNodeFullName()
    {
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

    public boolean hasChildren(boolean navigableOnly) {
        return navigableOnly ? allowsNavigableChildren() : allowsChildren();
    }

    protected abstract boolean allowsChildren();

    protected boolean allowsNavigableChildren() {
        return allowsChildren();
    }

    public abstract DBNNode[] getChildren(DBRProgressMonitor monitor) throws DBException;

    void clearNode(boolean reflect)
    {

    }

    public boolean supportsRename()
    {
        return false;
    }

    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        throw new DBException("Rename is not supported");
    }

    public boolean supportsDrop(DBNNode otherNode)
    {
        return false;
    }

    public void dropNodes(Collection<DBNNode> nodes) throws DBException
    {
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
     * @param source event source
     * @return real refreshed node or null if nothing was refreshed
     * @throws DBException on any internal exception
     */
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException
    {
        if (this.getParentNode() != null) {
            return this.getParentNode().refreshNode(monitor, source);
        } else {
            return null;
        }
    }
    
    public boolean allowsOpen()
    {
        return true;
    }

    public boolean isChildOf(DBNNode node)
    {
        for (DBNNode parent = getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (parent == node) {
                return true;
            }
        }
        return false;
    }

    public boolean isFiltered()
    {
        return false;
    }

    /**
     * Node item path in form [nodeType://]<path>
     * nodeType can be 'resource', 'folder' or 'database'.
     * If missing then 'database' will be used (backward compatibility).
     *
     * For resources and folders path is just a hierarchy path divided with / (slash).
     *
     * For database nodes path has form: type1=name1/type2=name2/...[/typeX]
     * Where typeN is path element for particular database item, name is database object name.
     * @return full item node path
     */
    public abstract String getNodeItemPath();

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }


    static void sortNodes(List<? extends DBNNode> nodes) {
        Collections.sort(nodes, new Comparator<DBNNode>() {
            @Override
            public int compare(DBNNode o1, DBNNode o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
    }

}
