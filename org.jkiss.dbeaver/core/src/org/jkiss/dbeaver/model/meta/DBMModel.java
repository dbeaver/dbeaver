/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.meta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSListener;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectAction;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.event.DataSourceEvent;
import org.jkiss.dbeaver.registry.event.IDataSourceListener;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.util.*;

/**
 * DBMModel.
 * Contains all objects which are shown in navigator tree.
 * Also ties DBSObjects to thee model (DBMNode).
 *
 * It's strongly recommended to not put the same DBSObject in tree model multiple times.
 * It will work but some actions will not work well
 * (e.g. TreeViewer sometimes update only first TreeItem corresponding to model certain model object).
 */
public class DBMModel implements IDataSourceListener, DBSListener {
    static final Log log = LogFactory.getLog(DBMModel.class);

    private DataSourceRegistry registry;
    private DBMRoot root;
    private List<IDBMListener> listeners = new ArrayList<IDBMListener>();
    private Map<DBSObject, Object> nodeMap = new HashMap<DBSObject, Object>();

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
        Object obj = nodeMap.get(object);
        if (obj == null) {
            return null;
        } else if (obj instanceof DBMNode) {
            return (DBMNode)obj;
        } else if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            List<DBMNode> nodeList = (List<DBMNode>) obj;
            if (nodeList.isEmpty()) {
                return null;
            }
            if (nodeList.size() > 1) {
                for (DBMNode node : nodeList) {
                    if (node instanceof DBMTreeItem && !((DBMTreeItem)node).getMeta().isVirtual()) {
                        return node;
                    }
                }
            }
            // Get just first one
            return nodeList.get(0);
        } else {
            // Never be here
           throw new IllegalStateException();
        }
/*
        if (node == null) {
            log.warn("Can't find tree node for object " + object.getName() + " (" + object.getClass().getName() + ")");
        }
        return node;
*/
    }

    public DBMNode getNodeByObject(DBRProgressMonitor monitor, DBSObject object, boolean load)
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
            node = getNodeByObject(item);
            if (node == null) {
                log.warn("Can't find tree node for object " + item.getName() + " (" + item.getClass().getName() + ")");
                return null;
            }
            try {
                List<? extends DBMNode> children = node.getChildren(monitor);
                for (DBMNode child : children) {
                    if (child instanceof DBMTreeFolder) {
                        Class<?> itemsClass = ((DBMTreeFolder) child).getItemsClass();
                        if (itemsClass != null && itemsClass.isAssignableFrom(nextItem.getClass())) {
                            child.getChildren(monitor);
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

    void addNode(DBMNode node)
    {
        Object obj = nodeMap.get(node.getObject());
        if (obj == null) {
            // New node
            nodeMap.put(node.getObject(), node);
        } else if (obj instanceof DBMNode) {
            // Second node - make a list
            List<DBMNode> nodeList = new ArrayList<DBMNode>(2);
            nodeList.add((DBMNode)obj);
            nodeList.add(node);
            nodeMap.put(node.getObject(), nodeList);
        } else if (obj instanceof List) {
            // Multiple nodes
            @SuppressWarnings("unchecked")
            List<DBMNode> nodeList = (List<DBMNode>) obj;
            nodeList.add(node);
        }
        this.fireNodeEvent(new DBMEvent(this, DBMEvent.Action.ADD, node));
    }

    void removeNode(DBMNode node)
    {
        Object obj = nodeMap.get(node.getObject());
        boolean badNode = false;
        if (obj == null) {
            // No found
            badNode = true;
        } else if (obj instanceof DBMNode) {
            // Just remove it
            if (nodeMap.remove(node.getObject()) != node) {
                badNode = true;
            }
        } else if (obj instanceof List) {
            // Multiple nodes
            @SuppressWarnings("unchecked")
            List<DBMNode> nodeList = (List<DBMNode>) obj;
            if (!nodeList.remove(node)) {
                badNode = true;
            }
            if (nodeList.isEmpty()) {
                nodeMap.remove(node.getObject());
            }
        }
        if (badNode) {
            log.warn("Remove unregistered meta node object " + node.getObject().getName() + " (" + node.getObject().getClass().getName() + ")");
        } else {
            this.fireNodeEvent(new DBMEvent(this, DBMEvent.Action.REMOVE, node));
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

    public void handleDataSourceEvent(DataSourceEvent event)
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
                final DBMNode dbmNode = getNodeByObject(event.getDataSource());
                if (dbmNode != null) {
                    try {
                        // Refresh with void monitor - this is disconnect event so
                        // no database reoundtrips will occure, just UI refresh
                        dbmNode.refreshNode(VoidProgressMonitor.INSTANCE);
                    } catch (DBException e) {
                        log.error(e);
                    }
                    fireNodeRefresh(event.getSource(), dbmNode, DBMEvent.NodeChange.UNLOADED);
                }
                event.getDataSource().removeListener(this);
                break;
            }
            case CONNECT:
                event.getDataSource().addListener(this);
            case CHANGE:
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

    public void handleObjectEvent(DBSObjectAction action, DBSObject object)
    {
        switch (action) {
            case CREATED:
                // TODO: Add new node to tree
                break;
            case DROPED:
                // TODO: Remove node from tree
                break;
            case ALTERED:
            case CHANGED:
            case REFRESHED:
            {
                DBMNode dbmNode = getNodeByObject(object);
                if (dbmNode != null) {
                    DBMEvent.NodeChange nodeChange = DBMEvent.NodeChange.CHANGED;
                    if (action == DBSObjectAction.REFRESHED) {
                        nodeChange = DBMEvent.NodeChange.REFRESH;
                    }
                    fireNodeRefresh(this, dbmNode, nodeChange);
                }
                break;
            }
        }
    }
}
