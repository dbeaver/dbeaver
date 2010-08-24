/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionDelegate;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * DBMRoot
 */
public class DBMRoot extends DBMNode implements DBSObject
{
    private List<DBMDataSource> dataSources = new ArrayList<DBMDataSource>();

    public DBMRoot(DBMModel model)
    {
        super(model);
    }

    void dispose()
    {
        for (DBMDataSource dataSource : dataSources) {
            dataSource.dispose();
        }
        dataSources.clear();
    }

    public DBSObject getObject()
    {
        return this;
    }

    public Object getValueObject()
    {
        return null;
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

    public List<? extends DBMNode> getChildren(DBRProgressMonitor monitor)
    {
        return dataSources;
    }

    public DBMNode refreshNode(DBRProgressMonitor monitor)
        throws DBException
    {
        // Nothing to do
        return null;
    }

    public Class<IActionDelegate> getDefaultAction()
    {
        return null;
    }

    public boolean isLazyNode()
    {
        return false;
    }

    void addDataSource(DataSourceDescriptor descriptor)
    {
        dataSources.add(
            new DBMDataSource(this, descriptor));
    }

    void removeDataSource(DataSourceDescriptor descriptor)
    {
        for (Iterator<DBMDataSource> iter = dataSources.iterator(); iter.hasNext(); ) {
            DBMDataSource dataSource = iter.next();
            if (dataSource.getObject() == descriptor) {
                iter.remove();
                dataSource.dispose();
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

}
