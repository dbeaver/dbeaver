package org.jkiss.dbeaver.model.meta;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.load.ILoadService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DBMModel
 */
public class DBMModel
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
        this.root = new DBMRoot(this, registry);
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
                        Class itemsClass = ((DBMTreeFolder) child).getItemsClass();
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

    public void fireNodeRefresh(Object source, DBMNode node)
    {
        this.fireNodeEvent(new DBMEvent(source, DBMEvent.Action.REFRESH, node));
    }

    void fireNodeEvent(DBMEvent event)
    {
        for (IDBMListener listener :  new ArrayList<IDBMListener>(listeners)) {
            listener.nodeChanged(event);
        }
    }

    public void dispose()
    {
        root.dispose();
    }
}
