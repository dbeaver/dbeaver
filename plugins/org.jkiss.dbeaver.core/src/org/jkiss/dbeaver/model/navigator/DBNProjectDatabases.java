/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.DBIcon;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * DBNProjectDatabases
 */
public class DBNProjectDatabases extends DBNResource implements DBNContainer, DBPEventListener
{
    private List<DBNDataSource> dataSources = new ArrayList<DBNDataSource>();
    private DataSourceRegistry dataSourceRegistry;

    public DBNProjectDatabases(DBNNode parentNode, IResource resource, DBPResourceHandler handler)
    {
        super(parentNode, resource, handler);
        dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(resource.getProject());
        dataSourceRegistry.addDataSourceListener(this);

        List<DataSourceDescriptor> projectDataSources = dataSourceRegistry.getDataSources();
        for (DataSourceDescriptor ds : projectDataSources) {
            addDataSource(ds, false);
        }
    }

    protected void dispose(boolean reflect)
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

    public DataSourceRegistry getDataSourceRegistry()
    {
        return dataSourceRegistry;
    }

    public Object getValueObject()
    {
        return dataSourceRegistry;
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
            return addDataSource((DataSourceDescriptor)childObject, true);
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
        return "Connections";
    }

    public String getNodeDescription()
    {
        return getResource().getProject().getName() + " connections";
    }

    public Image getNodeIcon()
    {
        return DBIcon.CONNECTIONS.getImage();
    }

    public boolean allowsChildren()
    {
        return !dataSources.isEmpty();
    }

    @Override
    public boolean allowsNavigableChildren()
    {
        return allowsChildren();
    }

    public List<? extends DBNNode> getChildren(DBRProgressMonitor monitor)
    {
        return dataSources;
    }

    public String getDefaultCommandId()
    {
        return null;
    }

    private DBNDataSource addDataSource(DataSourceDescriptor descriptor, boolean reflect)
    {
        DBNDataSource newNode = new DBNDataSource(this, descriptor);
        dataSources.add(newNode);
        this.getModel().addNode(newNode, reflect);
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
                    addDataSource((DataSourceDescriptor) event.getObject(), true);
                } else if (getModel().getNodeByObject(event.getObject()) == null) {
                    final DBNDatabaseNode parentNode = getModel().getParentNode(event.getObject());

                    if (parentNode != null) {
                        if (parentNode.getChildNodes() == null && parentNode.allowsChildren()) {
                            // We have to load children here
                            try {
                                DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                                    {
                                        try {
                                            parentNode.getChildren(monitor);
                                        } catch (Exception e) {
                                            throw new InvocationTargetException(e);
                                        }
                                    }
                                });
                            } catch (InvocationTargetException e) {
                                log.error(e.getTargetException());
                            } catch (InterruptedException e) {
                                // do nothing
                            }
                        }
                        if (parentNode.getChildNodes() != null) {
                            parentNode.addChildItem(event.getObject());
                        }
                    }
                }
                break;
            case OBJECT_REMOVE:
                if (event.getObject() instanceof DataSourceDescriptor) {
                    removeDataSource((DataSourceDescriptor) event.getObject());
                } else {
                    final DBNDatabaseNode node = getModel().getNodeByObject(event.getObject());
                    if (node != null && node.getParentNode() instanceof DBNDatabaseNode) {
                        ((DBNDatabaseNode)node.getParentNode()).removeChildItem(event.getObject());
                    }
                }
                break;
            case OBJECT_UPDATE:
            case OBJECT_SELECT:
            {
                DBNNode dbmNode = getModel().getNodeByObject(event.getObject());
                if (dbmNode != null) {
                    DBNEvent.NodeChange nodeChange;
                    Boolean enabled = null;
                    if (event.getAction() == DBPEvent.Action.OBJECT_SELECT) {
                        nodeChange = DBNEvent.NodeChange.REFRESH;
                    } else {
                        enabled = event.getEnabled();
                        if (enabled != null) {
                            if (enabled) {
                                nodeChange = DBNEvent.NodeChange.LOAD;
                            } else {
                                nodeChange = DBNEvent.NodeChange.UNLOAD;
                            }
                        } else {
                            nodeChange = DBNEvent.NodeChange.REFRESH;
                        }
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
