/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

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
 * DBNModel.
 * Contains all objects which are shown in navigator tree.
 * Also ties DBSObjects to thee model (DBNNode).
 *
 * It's strongly recommended to not put the same DBSObject in tree model multiple times.
 * It will work but some actions will not work well
 * (e.g. TreeViewer sometimes update only first TreeItem corresponding to model certain model object).
 */
public class DBNModel implements IDataSourceListener, DBSListener {
    static final Log log = LogFactory.getLog(DBNModel.class);

    private DataSourceRegistry registry;
    private DBNRoot root;
    private List<IDBNListener> listeners = new ArrayList<IDBNListener>();
    private Map<DBSObject, Object> nodeMap = new HashMap<DBSObject, Object>();

    //private static Map<DataSourceRegistry, DBNModel> modelMap = new HashMap<DataSourceRegistry, DBNModel>();

    public DBNModel(DataSourceRegistry registry)
    {
        this.registry = registry;
        this.root = new DBNRoot(this);
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
            for (IDBNListener listener : listeners) {
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

    public DBNRoot getRoot()
    {
        return root;
    }

    public DBNNode findNode(Object object)
    {
        if (object instanceof DBNNode) {
            return (DBNNode)object;
        } else if (object instanceof DBSObject) {
            return this.getNodeByObject((DBSObject)object);
        } else {
            return null;
        }
    }

    public DBNNode getNodeByObject(DBSObject object)
    {
        if (object instanceof DBNNode) {
            return (DBNNode)object;
        }
        Object obj = nodeMap.get(object);
        if (obj == null) {
            return null;
        } else if (obj instanceof DBNNode) {
            return (DBNNode)obj;
        } else if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            List<DBNNode> nodeList = (List<DBNNode>) obj;
            if (nodeList.isEmpty()) {
                return null;
            }
            if (nodeList.size() > 1) {
                for (DBNNode node : nodeList) {
                    if (node instanceof DBNTreeItem && !((DBNTreeItem)node).getMeta().isVirtual()) {
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

    public DBNNode getNodeByObject(DBRProgressMonitor monitor, DBSObject object, boolean load)
    {
        DBNNode node = getNodeByObject(object);
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
                List<? extends DBNNode> children = node.getChildren(monitor);
                for (DBNNode child : children) {
                    if (child instanceof DBNTreeFolder) {
                        Class<?> itemsClass = ((DBNTreeFolder) child).getItemsClass();
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

    void addNode(DBNNode node)
    {
        addNode(node, true);
    }

    void addNode(DBNNode node, boolean reflect)
    {
        Object obj = nodeMap.get(node.getObject());
        if (obj == null) {
            // New node
            nodeMap.put(node.getObject(), node);
        } else if (obj instanceof DBNNode) {
            // Second node - make a list
            List<DBNNode> nodeList = new ArrayList<DBNNode>(2);
            nodeList.add((DBNNode)obj);
            nodeList.add(node);
            nodeMap.put(node.getObject(), nodeList);
        } else if (obj instanceof List) {
            // Multiple nodes
            @SuppressWarnings("unchecked")
            List<DBNNode> nodeList = (List<DBNNode>) obj;
            nodeList.add(node);
        }
        if (reflect) {
            this.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, DBNEvent.NodeChange.LOAD, node));
        }
    }

    void removeNode(DBNNode node)
    {
        removeNode(node, true);
    }

    void removeNode(DBNNode node, boolean reflect)
    {
        Object obj = nodeMap.get(node.getObject());
        boolean badNode = false;
        if (obj == null) {
            // No found
            badNode = true;
        } else if (obj instanceof DBNNode) {
            // Just remove it
            if (nodeMap.remove(node.getObject()) != node) {
                badNode = true;
            }
        } else if (obj instanceof List) {
            // Multiple nodes
            @SuppressWarnings("unchecked")
            List<DBNNode> nodeList = (List<DBNNode>) obj;
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
            if (reflect) {
                this.fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, DBNEvent.NodeChange.UNLOAD, node));
            }
        }
    }

    public void addListener(IDBNListener listener)
    {
        if (this.listeners.contains(listener)) {
            log.warn("Listener " + listener + " already registered in model");
        } else {
            this.listeners.add(listener);
        }
    }

    public void removeListener(IDBNListener listener)
    {
        if (!this.listeners.remove(listener)) {
            log.warn("Listener " + listener + " wasn't registered in model");
        }
    }

    void fireNodeUpdate(Object source, DBNNode node, DBNEvent.NodeChange nodeChange)
    {
        this.fireNodeEvent(new DBNEvent(source, DBNEvent.Action.UPDATE, nodeChange, node));
    }

    void fireNodeEvent(final DBNEvent event)
    {
        for (IDBNListener listener :  new ArrayList<IDBNListener>(listeners)) {
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
                final DBNNode dbmNode = getNodeByObject(event.getDataSource());
                if (dbmNode != null) {
                    dbmNode.clearNode();
                    fireNodeUpdate(event.getSource(), dbmNode, DBNEvent.NodeChange.UNLOAD);
                }
                event.getDataSource().removeListener(this);
                break;
            }
            case CONNECT:
                event.getDataSource().addListener(this);
            case CHANGE:
            case CONNECT_FAIL:
            {
                DBNNode dbmNode = getNodeByObject(event.getDataSource());
                if (dbmNode != null) {
                    DBNEvent.NodeChange nodeChange = DBNEvent.NodeChange.REFRESH;
                    switch (event.getAction()) {
                    case CONNECT: nodeChange = DBNEvent.NodeChange.LOAD; break;
                    case CONNECT_FAIL: nodeChange = DBNEvent.NodeChange.UNLOAD; break;
                    }
                    fireNodeUpdate(
                        event.getSource(),
                        dbmNode, 
                        nodeChange);
                }
                break;
            }
            case INVALIDATE:
            {
                DBNNode dbmNode = getNodeByObject(event.getDataSource());
                fireNodeUpdate(
                    event.getSource(),
                    dbmNode,
                    DBNEvent.NodeChange.REFRESH);
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
                DBNNode dbmNode = getNodeByObject(object);
                if (dbmNode != null) {
                    DBNEvent.NodeChange nodeChange = DBNEvent.NodeChange.REFRESH;
                    if (action == DBSObjectAction.REFRESHED) {
                        nodeChange = DBNEvent.NodeChange.REFRESH;
                    }
                    fireNodeUpdate(this, dbmNode, nodeChange);
                }
                break;
            }
        }
    }

}
