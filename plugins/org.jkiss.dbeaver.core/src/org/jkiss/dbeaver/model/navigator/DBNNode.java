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

import org.jkiss.dbeaver.core.Log;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * DBNNode
 */
public abstract class DBNNode implements DBPNamedObject, DBPPersistedObject
{
    static final Log log = Log.getLog(DBNNode.class);

    private DBNNode parentNode;

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
        this.parentNode = null;
    }

    public DBNNode getParentNode()
    {
        return parentNode;
    }

    protected void setParentNode(DBNNode parentNode)
    {
        this.parentNode = parentNode;
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

    @Override
    public String getName()
    {
        return getNodeName();
    }

    public abstract String getNodeType();

    public abstract String getNodeName();

    public abstract String getNodeDescription();

    public abstract Image getNodeIcon();

    public Image getNodeIconDefault()
    {
        Image image = getNodeIcon();
        if (image == null) {
            if (this.allowsChildren()) {
                return DBIcon.TREE_FOLDER.getImage();
            } else {
                return DBIcon.TREE_PAGE.getImage();
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

    public abstract boolean allowsChildren();

    public abstract boolean allowsNavigableChildren();
    
    public abstract List<? extends DBNNode> getChildren(DBRProgressMonitor monitor) throws DBException;

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
     * Node item path in form type1=name1/type2=name2/...[/typeX]
     * Where typeN is path element for particular database item, name is database object name.
     * @return full item node path
     */
    public String getNodeItemPath()
    {
        StringBuilder pathName = new StringBuilder(100);

//        DBXTreeItem metaChildren = this instanceof DBNDatabaseNode ? ((DBNDatabaseNode)this).getItemsMeta() : null;
//        if (metaChildren != null) {
//            pathName.append(metaChildren.getPath());
//        }

        for (DBNNode node = this; node instanceof DBNDatabaseNode; node = node.getParentNode()) {
            if (node instanceof DBNDataSource) {
                if (pathName.length() > 0) {
                    pathName.insert(0, '/');
                }
                pathName.insert(0, ((DBNDataSource) node).getDataSourceContainer().getId());
            } else if (node instanceof DBNDatabaseFolder) {
                if (pathName.length() > 0) {
                    pathName.insert(0, '/');
                }
                String type = ((DBNDatabaseFolder) node).getMeta().getType();
                if (CommonUtils.isEmpty(type)) {
                    type = node.getName();
                }
                pathName.insert(0, type);
            }
            if (!(node instanceof DBNDatabaseItem) && !(node instanceof DBNDatabaseObject)) {
                // skip folders
                continue;
            }

            if (pathName.length() > 0) {
                pathName.insert(0, '/');
            }
            pathName.insert(0, node.getNodeName().replace('/', '_'));
        }
        return pathName.toString();
    }

}
