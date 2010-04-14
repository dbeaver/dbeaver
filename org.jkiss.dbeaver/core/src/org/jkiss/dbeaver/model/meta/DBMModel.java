/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.event.DataSourceEvent;
import org.jkiss.dbeaver.registry.event.IDataSourceListener;
import org.jkiss.dbeaver.runtime.NullProgressMonitor;
import org.jkiss.dbeaver.runtime.load.ILoadService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DBMModel
 */
public class DBMModel implements IDataSourceListener
{
    static Log log = LogFactory.getLog(DBMModel.class);

    private DataSourceRegistry registry;
    private DBMRoot root;
    private List<IDBMListener> listeners = new ArrayList<IDBMListener>();
    private Map<DBSObject, DBMNode> nodeMap = new HashMap<DBSObject, DBMNode>();

    //private static Map<DataSourceRegistry, DBMModel> modelMap = new HashMap<DataSourceRegistry, DBMModel>();

    public DBMModel(DataSourceRegistry registry)
    {
        this.registry = registry;
        this.root = new DBMRoot(this);
        for (DataSourceDescriptor dataSource : registry.getDataSources()) {
            root.addDataSource(dataSource);
        }

        this.registry.addDataSourceListener(this);
    }

    public void dispose()
    {
        this.registry.removeDataSourceListener(this);
        this.root.dispose();
        this.nodeMap.clear();
        if (!listeners.isEmpty()) {
            for (IDBMListener listener : listeners) {
                log.warn("Listener '" + listener + "' is not unregistered from DBM model");
            }
        }
        this.listeners.clear();
    }

    public DBeaverCore getApplication()
    {
        return registry.getCore();
    }

    public DataSourceRegistry getRegistry()
    {
        return registry;
    }

    public DBMRoot getRoot()
    {
        return root;
    }

    public DBMNode findNode(Object object)
    {
        if (object instanceof DBMNode) {
            return (DBMNode)object;
        } else if (object instanceof DBSObject) {
            return this.getNodeByObject((DBSObject)object);
        } else {
            return null;
        }
    }

    public DBMNode getNodeByObject(DBSObject object)
    {
        if (object instanceof DBMNode) {
            return (DBMNode)object;
        }
        return nodeMap.get(object);
/*
        if (node == null) {
            log.warn("Can't find tree node for object " + object.getName() + " (" + object.getClass().getName() + ")");
        }
        return node;
*/
    }

    public DBMNode getNodeByObject(DBSObject object, boolean load, ILoadService loadService)
    {
        DBMNode node = getNodeByObject(object);
        if (node != null || !load) {
            return node;
        }
        List<DBSObject> path = new ArrayList<DBSObject>();
        for (DBSObject item = object; item != null; item = item.getParentObject()) {
            path.add(0, item);
        }
        for (int i = 0; i < path.size() - 1; i++) {
            DBSObject item = path.get(i);
            DBSObject nextItem = path.get(i + 1);
            node = nodeMap.get(item);
            if (node == null) {
                log.warn("Can't find tree node for object " + item.getName() + " (" + item.getClass().getName() + ")");
                return null;
            }
            try {
                List<? extends DBMNode> children = node.getChildren(loadService);
                for (DBMNode child : children) {
                    if (child instanceof DBMTreeFolder) {
                        Class<?> itemsClass = ((DBMTreeFolder) child).getItemsClass();
                        if (itemsClass != null && itemsClass.isAssignableFrom(nextItem.getClass())) {
                            child.getChildren(loadService);
                        }
                    }
                }
            } catch (DBException e) {
                log.error(e.getMessage());
                return null;
            }
        }
        return getNodeByObject(object);
    }

    void addNode(DBSObject object, DBMNode node)
    {
        DBMNode oldNode = nodeMap.put(object, node);
        if (oldNode != null) {
            log.warn("Overwrite meta node object " + object.getName() + " (" + object.getClass().getName() + ")");
        }
        this.fireNodeEvent(new DBMEvent(this, DBMEvent.Action.ADD, node));
    }

    void removeNode(DBSObject object)
    {
        DBMNode removed = nodeMap.remove(object);
        if (removed == null) {
            log.warn("Remove unregistered meta node object " + object.getName() + " (" + object.getClass().getName() + ")");
        } else {
            this.fireNodeEvent(new DBMEvent(this, DBMEvent.Action.REMOVE, removed));
        }
    }

    public void addListener(IDBMListener listener)
    {
        if (this.listeners.contains(listener)) {
            log.warn("Listener " + listener + " already registered in model");
        } else {
            this.listeners.add(listener);
        }
    }

    public void removeListener(IDBMListener listener)
    {
        if (!this.listeners.remove(listener)) {
            log.warn("Listener " + listener + " wasn't registered in model");
        }
    }

    public void fireNodeRefresh(Object source, DBMNode node, DBMEvent.NodeChange nodeChange)
    {
        this.fireNodeEvent(new DBMEvent(source, DBMEvent.Action.REFRESH, nodeChange, node));
    }

    void fireNodeEvent(final DBMEvent event)
    {
        for (IDBMListener listener :  new ArrayList<IDBMListener>(listeners)) {
            listener.nodeChanged(event);
        }
    }

    public void dataSourceChanged(DataSourceEvent event)
    {
        switch (event.getAction()) {
            case ADD:
                root.addDataSource(event.getDataSource());
                break;
            case REMOVE:
                root.removeDataSource(event.getDataSource());
                break;
            case DISCONNECT:
            {
                DBMNode dbmNode = getNodeByObject(event.getDataSource());
                if (dbmNode != null) {
                    try {
                        dbmNode.refreshNode(NullProgressMonitor.INSTANCE);
                    }
                    catch (DBException e) {
                        log.error("Error refreshing datasource tree node");
                    }
                    fireNodeRefresh(event.getSource(), dbmNode, DBMEvent.NodeChange.UNLOADED);
                }
                break;
            }
            case CHANGE:
            case CONNECT:
            case CONNECT_FAIL:
            {
                DBMNode dbmNode = getNodeByObject(event.getDataSource());
                if (dbmNode != null) {
                    DBMEvent.NodeChange nodeChange = DBMEvent.NodeChange.CHANGED;
                    switch (event.getAction()) {
                    case CONNECT: nodeChange = DBMEvent.NodeChange.LOADED; break;
                    case CONNECT_FAIL: nodeChange = DBMEvent.NodeChange.UNLOADED; break;
                    }
                    fireNodeRefresh(
                        event.getSource(),
                        dbmNode, 
                        nodeChange);
                }
                break;
            }
        }
    }

}
