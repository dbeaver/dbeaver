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

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.NavigatorUtils;
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
    public Image getNodeIcon()
    {
        return DBIcon.TREE_DATABASE_CATEGORY.getImage();
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
        return DataSourceDescriptor.class;
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
        NavigatorUtils.updateConfigAndRefreshDatabases(this);
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
        NavigatorUtils.updateConfigAndRefreshDatabases(this);
    }
}
