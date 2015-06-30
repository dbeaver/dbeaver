/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * DBNLocalFolder
 */
public class DBNLocalFolder extends DBNNode implements DBNContainer
{
    private String name;

    public DBNLocalFolder(DBNProjectDatabases parentNode, String name)
    {
        super(parentNode);
        this.name = name;
    }

    @Override
    void dispose(boolean reflect)
    {
        super.dispose(reflect);
    }

    @Override
    public Object getValueObject()
    {
        return null;
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
        return name;
    }

    @Override
    public String getNodeDescription()
    {
        return null;
    }

    @Override
    public DBPImage getNodeIcon()
    {
        return DBIcon.TREE_DATABASE_CATEGORY;
    }

    @Override
    public boolean allowsChildren()
    {
        return true;
    }

    @Override
    public boolean allowsNavigableChildren()
    {
        return true;
    }

    @Override
    public List<DBNDataSource> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return getDataSources();
    }

    public List<DBNDataSource> getDataSources()
    {
        List<DBNDataSource> children = new ArrayList<DBNDataSource>();
        DBNProjectDatabases parent = (DBNProjectDatabases) getParentNode();
        for (DBNDataSource dataSource : parent.getDataSources()) {
            if (getName().equals(dataSource.getDataSourceContainer().getFolderPath())) {
                children.add(dataSource);
            }
        }
        return children;
    }

    @Override
    public Class<? extends DBSObject> getChildrenClass()
    {
        return DBSDataSourceContainer.class;
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
                ((DBNDataSource) node).setFolderPath(getName());
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
        if (CommonUtils.isEmpty(newName)) {
            return;
        }
        List<DBNDataSource> dataSources = getDataSources();
        for (DBNDataSource dataSource : dataSources) {
            dataSource.setFolderPath(newName);
        }
        name = newName;
        DBNModel.updateConfigAndRefreshDatabases(this);
    }
}
