/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * DBNProjectDatabases
 */
public class DBNProjectDatabases extends DBNNode implements DBNContainer, DBPEventListener
{
    private IProject project;
    private List<DBNDataSource> dataSources = new ArrayList<DBNDataSource>();
    private DataSourceRegistry dataSourceRegistry;

    public DBNProjectDatabases(DBNNode parentNode, IProject project)
    {
        super(parentNode);
        this.project = project;
        dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(project);
        dataSourceRegistry.addDataSourceListener(this);

        List<DataSourceDescriptor> projectDataSources = dataSourceRegistry.getDataSources();
        for (DataSourceDescriptor ds : projectDataSources) {
            addDataSource(ds);
        }
    }

    void dispose(boolean reflect)
    {
        for (DBNDataSource dataSource : dataSources) {
            dataSource.dispose(reflect);
        }
        dataSources.clear();
        if (dataSourceRegistry != null) {
            dataSourceRegistry.removeDataSourceListener(this);
            dataSourceRegistry = null;
        }
        super.dispose(reflect);
    }

    public Object getValueObject()
    {
        return project;
    }

    public String getItemsLabel()
    {
        return "Connection";
    }

    public Class<DataSourceDescriptor> getItemsClass()
    {
        return DataSourceDescriptor.class;
    }

    public DBNNode addChildItem(DBRProgressMonitor monitor, Object childObject) throws DBException
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
        return "Databases";
    }

    public String getNodeDescription()
    {
        return project.getName() + " databases";
    }

    public Image getNodeIcon()
    {
        return DBIcon.DATABASES.getImage();
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

    public void handleDataSourceEvent(DBPEvent event)
    {
        switch (event.getAction()) {
            case OBJECT_ADD:
                if (event.getObject() instanceof DataSourceDescriptor) {
                    addDataSource((DataSourceDescriptor) event.getObject());
                }
                break;
            case OBJECT_REMOVE:
                if (event.getObject() instanceof DataSourceDescriptor) {
                    removeDataSource((DataSourceDescriptor) event.getObject());
                }
                break;
            case OBJECT_UPDATE:
            {
                DBNNode dbmNode = getModel().getNodeByObject(event.getObject());
                if (dbmNode != null) {
                    DBNEvent.NodeChange nodeChange;
                    Boolean enabled = event.getEnabled();
                    if (enabled != null) {
                        if (enabled) {
                            nodeChange = DBNEvent.NodeChange.LOAD;
                        } else {
                            nodeChange = DBNEvent.NodeChange.UNLOAD;
                        }
                    } else {
                        nodeChange = DBNEvent.NodeChange.REFRESH;
                    }
                    getModel().fireNodeUpdate(
                        this,
                        dbmNode,
                        nodeChange);

                    if (enabled != null && !enabled) {
                        // Clear disabled node
                        dbmNode.clearNode(false);
                    }
                }
                break;
            }
        }
    }

}
