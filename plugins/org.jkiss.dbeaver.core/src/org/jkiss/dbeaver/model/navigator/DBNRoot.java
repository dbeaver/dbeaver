/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * DBNRoot
 */
public class DBNRoot extends DBNNode implements DBNContainer
{
    private List<DBNDataSource> dataSources = new ArrayList<DBNDataSource>();

    public DBNRoot(DBNModel model)
    {
        super(model);
    }

    void dispose(boolean reflect)
    {
        for (DBNDataSource dataSource : dataSources) {
            dataSource.dispose(reflect);
        }
        dataSources.clear();
    }

    public DBSObject getObject()
    {
        return this;
    }

    public Object getValueObject()
    {
        return this;
    }

    public Class<DataSourceDescriptor> getItemsClass()
    {
        return DataSourceDescriptor.class;
    }

    public DBNNode addChildItem(DBRProgressMonitor monitor, DBSObject childObject) throws DBException
    {
        if (childObject instanceof DataSourceDescriptor) {
            return addDataSource((DataSourceDescriptor)childObject);
        }
        throw new IllegalArgumentException("Only data source descriptors could be added to root node");
    }

    public void removeChildItem(DBNNode item) throws DBException
    {
        if (item instanceof DBNDataSource) {
            removeDataSource(((DBNDataSource)item).getObject());
        } else {
            throw new IllegalArgumentException("Only data source descriptors could be removed from root node");
        }
    }

    public String getNodeName()
    {
        return null;
    }

    public String getNodeDescription()
    {
        return null;
    }

    public Image getNodeIcon()
    {
        return null;
    }

    public boolean hasChildren()
    {
        return !dataSources.isEmpty();
    }

    @Override
    public boolean hasNavigableChildren()
    {
        return hasChildren();
    }

    public List<? extends DBNNode> getChildren(DBRProgressMonitor monitor)
    {
        return dataSources;
    }

    public String getDefaultCommandId()
    {
        return null;
    }

    public boolean isLazyNode()
    {
        return false;
    }

    DBNDataSource addDataSource(DataSourceDescriptor descriptor)
    {
        DBNDataSource newNode = new DBNDataSource(this, descriptor);
        dataSources.add(newNode);
        return newNode;
    }

    void removeDataSource(DataSourceDescriptor descriptor)
    {
        for (Iterator<DBNDataSource> iter = dataSources.iterator(); iter.hasNext(); ) {
            DBNDataSource dataSource = iter.next();
            if (dataSource.getObject() == descriptor) {
                iter.remove();
                dataSource.dispose(true);
                break;
            }
        }
    }

    public String getName()
    {
        return "#root";
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return null;
    }

    public DBPDataSource getDataSource()
    {
        return null;
    }

    public boolean isPersisted()
    {
        return true;
    }

}
