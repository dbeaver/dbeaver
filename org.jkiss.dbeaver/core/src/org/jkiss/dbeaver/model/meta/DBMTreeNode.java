package org.jkiss.dbeaver.model.meta;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeFolder;
import org.jkiss.dbeaver.registry.tree.DBXTreeIcon;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DBMTreeNode
 */
public abstract class DBMTreeNode extends DBMNode {
    static Log log = LogFactory.getLog(DBMTreeNode.class);

    private List<DBMTreeNode> childNodes;

    protected DBMTreeNode(DBMNode parentNode)
    {
        super(parentNode);
    }

    void dispose()
    {
        if (childNodes != null) {
            clearChildren();
            childNodes = null;
        }
        super.dispose();
    }

    public String getNodeName()
    {
        return getObject().getName();
    }

    public String getNodeDescription()
    {
        return getObject().getDescription();
    }

    public Image getNodeIcon()
    {
        DBXTreeNode meta = getMeta();
        if (meta != null) {
            List<DBXTreeIcon> extIcons = meta.getIcons();
            if (!CommonUtils.isEmpty(extIcons)) {
                JexlContext exprContext = new JexlContext() {
                    Map map;
                    public void setVars(Map map)
                    {
                        this.map = map;
                    }

                    public Map getVars()
                    {
                        return map;
                    }
                };
                exprContext.setVars(Collections.singletonMap("object", getObject()));
                // Try to get some icon depending on it's condition
                for (DBXTreeIcon icon : extIcons) {
                    if (icon.getExpr() == null) {
                        continue;
                    }
                    try {
                        Object result = icon.getExpr().evaluate(exprContext);
                        if (Boolean.TRUE.equals(result)) {
                            return icon.getIcon();
                        }
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }
            return meta.getDefaultIcon();
        }
        return null;
    }

    public boolean hasChildren()
    {
        return this.getMeta().hasChildren();
    }

    public boolean hasChildren(DBXTreeNode childType, ILoadService loadService)
        throws DBException
    {
        for (DBMTreeNode child : getChildren(loadService)) {
            if (child.getMeta() == childType) {
                return true;
            }
        }
        return false;
    }

    public List<DBMTreeNode> getChildren(ILoadService loadService)
        throws DBException
    {
        if (this.hasChildren() && childNodes == null) {
            if (this.initializeNode(loadService.getProgressMonitor())) {
                this.childNodes = loadChildren(getMeta(), loadService);
            }
        }
        return childNodes;
    }

    public boolean isLazyNode()
    {
        return true;
    }

    protected boolean initializeNode(DBRProgressMonitor monitor)
        throws DBException
    {
        return true;
    }

    protected void clearChildren()
    {
        if (childNodes != null) {
            for (DBMNode child : childNodes) {
                child.dispose();
            }
            childNodes.clear();
            childNodes = null;
        }
    }

    private List<DBMTreeNode> loadChildren(final DBXTreeNode meta, ILoadService loadService)
        throws DBException
    {
        final List<DBMTreeNode> tmpList = new ArrayList<DBMTreeNode>();
        loadChildren(meta, tmpList, loadService);
        return tmpList;
    }

    private void loadChildren(
        final DBXTreeNode meta,
        final List<DBMTreeNode> toList,
        ILoadService loadService)
        throws DBException
    {
        if (!meta.hasChildren()) {
            return;
        }
        DBRProgressMonitor monitor = loadService.getProgressMonitor();
        List<DBXTreeNode> childMetas = meta.getChildren();
        monitor.beginTask("Load items ...", childMetas.size());

        for (DBXTreeNode child : meta.getChildren()) {
            if (monitor.isCanceled()) {
                break;
            }
            monitor.subTask("Load " + child.getLabel());
            if (child instanceof DBXTreeItem) {
                final DBXTreeItem item = (DBXTreeItem) child;
                boolean isLoaded = loadTreeItems(item, toList, loadService);
                if (!isLoaded && item.isOptional()) {
                    loadChildren(item, toList, loadService);
                }
            } else {
                toList.add(
                    new DBMTreeFolder(DBMTreeNode.this, (DBXTreeFolder) child));
            }
            monitor.worked(1);
        }
        monitor.done();
    }

    /**
     * Extract items using reflect api
     * @param meta items meta info
     * @param toList list ot add new items
     * @param loadService load service
     * @return true on success
     * @throws DBException on any DB error
     */
    private boolean loadTreeItems(DBXTreeItem meta, List<DBMTreeNode> toList, ILoadService loadService)
        throws DBException
    {
        // Read property using reflection
        Object object = getValueObject();
        if (object == null) {
            return false;
        }
        String propertyName = meta.getPropertyName();
        Object propertyValue = LoadingUtils.extractPropertyValue(object, propertyName, loadService);
        if (propertyValue == null) {
            return false;
        }
        if (!(propertyValue instanceof Collection)) {
            log.warn("Bad property '" + propertyName + "' value: " + propertyValue.getClass().getName());
            return false;
        }
        Collection<?> itemList = (Collection<?>) propertyValue;
        if (itemList.isEmpty()) {
            return false;
        }
        if (this.isDisposed()) {
            // Property reading can take realy long time so this node can be disposed at this moment -
            // check it
            return false;
        }
        for (Object childItem : itemList) {
            if (childItem == null) {
                continue;
            }
            if (!(childItem instanceof DBSObject)) {
                log.warn("Bad item type: " + childItem.getClass().getName());
                continue;
            }
            DBMTreeItem treeItem = new DBMTreeItem(
                this,
                meta,
                (DBSObject) childItem);
            toList.add(treeItem);
        }
        return true;
    }

    public abstract DBXTreeNode getMeta();

    public abstract DBSObject getObject();

}
