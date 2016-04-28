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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * DBNProjectDatabases
 */
public class DBNProjectDatabases extends DBNNode implements DBNContainer, DBPEventListener
{
    private static final Log log = Log.getLog(DBNProjectDatabases.class);

    private List<DBNDataSource> dataSources = new ArrayList<>();
    private DBPDataSourceRegistry dataSourceRegistry;
    private volatile DBNNode[] children;
    private final List<DBNLocalFolder> folders = new ArrayList<>();

    public DBNProjectDatabases(DBNProject parentNode, DBPDataSourceRegistry dataSourceRegistry)
    {
        super(parentNode);
        this.dataSourceRegistry = dataSourceRegistry;
        this.dataSourceRegistry.addDataSourceListener(this);

        List<? extends DBPDataSourceContainer> projectDataSources = this.dataSourceRegistry.getDataSources();
        for (DBPDataSourceContainer ds : projectDataSources) {
            addDataSource(ds, false);
        }
    }

    @Override
    protected void dispose(boolean reflect)
    {
        for (DBNDataSource dataSource : dataSources) {
            dataSource.dispose(reflect);
        }
        dataSources.clear();
        folders.clear();
        children = null;
        if (dataSourceRegistry != null) {
            dataSourceRegistry.removeDataSourceListener(this);
            dataSourceRegistry = null;
        }
        super.dispose(reflect);
    }

    @Override
    public String getNodeType()
    {
        return "connections";
    }

    public DBPDataSourceRegistry getDataSourceRegistry()
    {
        return dataSourceRegistry;
    }

    @Override
    public Object getValueObject()
    {
        return dataSourceRegistry;
    }

    @Override
    public String getChildrenType()
    {
        return ModelMessages.model_navigator_Connection;
    }

    @Override
    public Class<DBPDataSourceContainer> getChildrenClass()
    {
        return DBPDataSourceContainer.class;
    }

    @Override
    public String getNodeName()
    {
        return "Connections";
    }

    @Override
    public String getNodeDescription()
    {
        return ((DBNProject)getParentNode()).getProject().getName() + ModelMessages.model_navigator__connections;
    }

    @Override
    public DBPImage getNodeIcon()
    {
        return DBIcon.CONNECTIONS;
    }

    @Override
    public boolean allowsChildren()
    {
        return !dataSources.isEmpty();
    }

    @Override
    public boolean allowsNavigableChildren()
    {
        return allowsChildren();
    }

    @Override
    public DBNNode[] getChildren(DBRProgressMonitor monitor)
    {
        if (children == null) {
            List<DBNNode> childNodes = new ArrayList<>();
            for (DBNDataSource dataSource : dataSources) {
                String folderPath = dataSource.getDataSourceContainer().getFolderPath();
                if (CommonUtils.isEmpty(folderPath)) {
                    childNodes.add(dataSource);
                } else {
                    DBNLocalFolder folder = getLocalFolder(folderPath);
                    if (folder == null) {
                        folder = new DBNLocalFolder(this, folderPath);
                        folders.add(folder);
                    }
                    if (!childNodes.contains(folder)) {
                        childNodes.add(folder);
                    }
                }
            }
            Collections.sort(childNodes, new Comparator<DBNNode>() {
                @Override
                public int compare(DBNNode o1, DBNNode o2)
                {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
            this.children = childNodes.toArray(new DBNNode[childNodes.size()]);
        }
        return children;
    }

    public void refreshChildren()
    {
        this.children = null;
        getModel().fireNodeUpdate(this, this, DBNEvent.NodeChange.STRUCT_REFRESH);
    }

    @Override
    public boolean allowsOpen()
    {
        return false;
    }

    public DBNLocalFolder getLocalFolder(String name)
    {
        synchronized (folders) {
            for (DBNLocalFolder folder : folders) {
                if (folder.getName().equals(name)) {
                    return folder;
                }
            }
        }
        return null;
    }

    public List<DBNDataSource> getDataSources()
    {
        return dataSources;
    }

    public DBNDataSource getDataSource(String id)
    {
        for (DBNDataSource dataSource : dataSources) {
            if (dataSource.getDataSourceContainer().getId().equals(id)) {
                return dataSource;
            }
        }
        return null;
    }

    private DBNDataSource addDataSource(DBPDataSourceContainer descriptor, boolean reflect)
    {
        DBNDataSource newNode = new DBNDataSource(this, descriptor);
        dataSources.add(newNode);
        children = null;
        if (reflect) {
            getModel().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, newNode));
        }
        return newNode;
    }

    void removeDataSource(DBPDataSourceContainer descriptor)
    {
        DBNDataSource removedNode = null;
        for (Iterator<DBNDataSource> iter = dataSources.iterator(); iter.hasNext(); ) {
            DBNDataSource dataSource = iter.next();
            if (dataSource.getObject() == descriptor) {
                iter.remove();
                removedNode = dataSource;
                break;
            }
        }
        if (removedNode != null) {
            children = null;
            removedNode.dispose(true);
        }
    }

    @Override
    public void handleDataSourceEvent(DBPEvent event)
    {
        DBNModel model = getModel();
        switch (event.getAction()) {
            case OBJECT_ADD:
                if (event.getObject() instanceof DBPDataSourceContainer) {
                    addDataSource((DBPDataSourceContainer) event.getObject(), true);
                } else if (model.getNodeByObject(event.getObject()) == null) {
                    final DBNDatabaseNode parentNode = model.getParentNode(event.getObject());

                    if (parentNode != null) {
                        if (parentNode.getChildNodes() == null && parentNode.allowsChildren()) {
                            // We have to load children here
                            try {
                                model.getApplication().getRunnableContext().run(true, true, new DBRRunnableWithProgress() {
                                    @Override
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
                        if (parentNode.getChildNodes() != null && !parentNode.hasChildItem(event.getObject())) {
                            // Add only if object wasn't yet added (e.g. by create new object command)
                            parentNode.addChildItem(event.getObject());
                        }
                    }
                }
                break;
            case OBJECT_REMOVE:
                if (event.getObject() instanceof DBPDataSourceContainer) {
                    removeDataSource((DBPDataSourceContainer) event.getObject());
                } else {
                    final DBNDatabaseNode node = model.getNodeByObject(event.getObject());
                    if (node != null && node.getParentNode() instanceof DBNDatabaseNode) {
                        ((DBNDatabaseNode)node.getParentNode()).removeChildItem(event.getObject());
                    }
                }
                break;
            case OBJECT_UPDATE:
            case OBJECT_SELECT:
            {
                DBNNode dbmNode = model.getNodeByObject(event.getObject());
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
                    model.fireNodeUpdate(
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
