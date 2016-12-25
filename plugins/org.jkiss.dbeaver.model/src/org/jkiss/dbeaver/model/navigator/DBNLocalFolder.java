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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * DBNLocalFolder
 */
public class DBNLocalFolder extends DBNNode implements DBNContainer
{
    private DBPDataSourceFolder folder;

    public DBNLocalFolder(DBNProjectDatabases parentNode, DBPDataSourceFolder folder)
    {
        super(parentNode);
        this.folder = folder;
    }

    @Override
    void dispose(boolean reflect)
    {
        super.dispose(reflect);
    }

    public DBPDataSourceFolder getFolder() {
        return folder;
    }

    public DBPDataSourceRegistry getDataSourceRegistry() {
        return ((DBNProjectDatabases)parentNode).getDataSourceRegistry();
    }

    @NotNull
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return getNodeName();
    }

    @Override
    public Object getValueObject()
    {
        return folder;
    }

    @Override
    public String getChildrenType()
    {
        return "connections";
    }

    @Override
    public String getNodeType()
    {
        return "folder";
    }

    @Override
    public String getNodeName()
    {
        return folder.getName();
    }

    @Override
    public String getNodeDescription()
    {
        return folder.getDescription();
    }

    @Override
    public DBPImage getNodeIcon()
    {
        DBPImage dsIcon = null;
        for (DBNDataSource ds : getDataSources()) {
            final DBPImage icon = DBValueFormatting.getObjectImage(ds.getDataSourceContainer());
            if (dsIcon == null) {
                dsIcon = icon;
            } else if (!CommonUtils.equalObjects(dsIcon, icon)) {
                dsIcon = null;
                break;
            }
        }
        return DBIcon.TREE_DATABASE_CATEGORY;
/*
        if (dsIcon == null) {
            return DBIcon.TREE_DATABASE_CATEGORY;
        } else {
            // All datasources have the same icon.
            // Make it a folder
            return new DBIconComposite(
                dsIcon,
                false,
                null,
                null,
                null,
                DBIcon.OVER_FOLDER);
        }
*/
    }

    @Override
    public String getNodeItemPath() {
        return NodePathType.folder.getPrefix() + getParentNode().getDataSourceRegistry().getProject().getName() + "/" + folder.getFolderPath();
    }

    @Override
    public DBNProjectDatabases getParentNode() {
        return (DBNProjectDatabases)super.getParentNode();
    }

    @Override
    public boolean allowsChildren()
    {
        return true;
    }

    @Override
    public boolean hasChildren(boolean navigableOnly) {
        if (!ArrayUtils.isEmpty(folder.getChildren())) {
            return true;
        }
        for (DBNDataSource dataSource : getParentNode().getDataSources()) {
            if (folder == dataSource.getDataSourceContainer().getFolder()) {
                return true;
            }
        }
        return false;
    }

    public DBNNode getLogicalParent() {
        if (folder.getParent() == null) {
            return getParentNode();
        } else {
            return getParentNode().getFolderNode(folder.getParent());
        }
    }

    @Override
    public DBNNode[] getChildren(DBRProgressMonitor monitor) throws DBException
    {
        if (ArrayUtils.isEmpty(folder.getChildren())) {
            return ArrayUtils.toArray(DBNDataSource.class, getDataSources());
        }
        final List<DBNNode> nodes = new ArrayList<>();
        for (DBPDataSourceFolder childFolder : folder.getChildren()) {
            nodes.add(getParentNode().getFolderNode(childFolder));
        }
        nodes.addAll(getDataSources());
        sortNodes(nodes);
        return ArrayUtils.toArray(DBNNode.class, nodes);
    }

    public List<DBNDataSource> getDataSources()
    {
        List<DBNDataSource> children = new ArrayList<>();
        DBNProjectDatabases parent = getParentNode();
        for (DBNDataSource dataSource : parent.getDataSources()) {
            if (folder == dataSource.getDataSourceContainer().getFolder()) {
                children.add(dataSource);
            }
        }
        return children;
    }

    @Override
    public Class<? extends DBSObject> getChildrenClass()
    {
        return DBPDataSourceContainer.class;
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode)
    {
        return otherNode == null || otherNode instanceof DBNDataSource;
    }

    @Override
    public void dropNodes(Collection<DBNNode> nodes) throws DBException
    {
        for (DBNNode node : nodes) {
            if (node instanceof DBNDataSource) {
                ((DBNDataSource) node).setFolder(folder);
            }
        }
        DBNModel.updateConfigAndRefreshDatabases(this);
    }

    @Override
    public boolean supportsRename()
    {
        return true;
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        folder.setName(newName);
        DBNModel.updateConfigAndRefreshDatabases(this);
    }

    @Override
    public String toString() {
        return folder.getFolderPath();
    }
}
