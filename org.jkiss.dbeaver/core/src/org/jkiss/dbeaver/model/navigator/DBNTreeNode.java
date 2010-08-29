/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IObjectImageProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeFolder;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.registry.tree.DBXTreeObject;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;

import java.util.*;

/**
 * DBNTreeNode
 */
public abstract class DBNTreeNode extends DBNNode {
    static final Log log = LogFactory.getLog(DBNTreeNode.class);

    private List<DBNTreeNode> childNodes;

    protected DBNTreeNode(DBNNode parentNode)
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
        String objectName = getObject().getName();
        if (objectName == null) {
            objectName = "?";
        }
        return objectName;
    }

    public String getNodeDescription()
    {
        return getObject().getDescription();
    }

    public Image getNodeIcon()
    {
        if (getObject() instanceof IObjectImageProvider) {
            Image image = ((IObjectImageProvider) getObject()).getObjectImage();
            if (image != null) {
                return image;
            }
        }
        DBXTreeNode meta = getMeta();
        if (meta != null) {
            return meta.getIcon(getObject());
        }
        return null;
    }

    public boolean hasChildren()
    {
        return this.getMeta().hasChildren();
    }

    public boolean hasNavigableChildren()
    {
        if (!this.getMeta().hasChildren()) {
            return false;
        } else {
            for (DBXTreeNode child : this.getMeta().getChildren()) {
                if (child.isNavigable()) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean hasChildren(DBRProgressMonitor monitor, DBXTreeNode childType)
        throws DBException
    {
        for (DBNTreeNode child : getChildren(monitor)) {
            if (child.getMeta() == childType) {
                return true;
            }
        }
        return false;
    }

    public List<DBNTreeNode> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.hasChildren() && childNodes == null) {
            if (this.initializeNode(monitor)) {
                this.childNodes = loadChildren(monitor, getMeta());
            }
        }
        return childNodes;
    }

    public boolean isLazyNode()
    {
        return childNodes == null;
    }

    protected boolean initializeNode(DBRProgressMonitor monitor)
        throws DBException
    {
        return true;
    }

    protected void clearChildren()
    {
        if (childNodes != null) {
            for (DBNNode child : childNodes) {
                child.dispose();
            }
            childNodes.clear();
            childNodes = null;
        }
    }

    private List<DBNTreeNode> loadChildren(DBRProgressMonitor monitor, final DBXTreeNode meta
    )
        throws DBException
    {
        final List<DBNTreeNode> tmpList = new ArrayList<DBNTreeNode>();
        loadChildren(monitor, meta, tmpList);
        return tmpList;
    }

    private void loadChildren(
        DBRProgressMonitor monitor,
        final DBXTreeNode meta,
        final List<DBNTreeNode> toList)
        throws DBException
    {
        if (!meta.hasChildren()) {
            return;
        }
        List<DBXTreeNode> childMetas = meta.getChildren();
        monitor.beginTask("Load items ...", childMetas.size());

        for (DBXTreeNode child : meta.getChildren()) {
            if (monitor.isCanceled()) {
                break;
            }
            monitor.subTask("Load " + child.getLabel());
            if (child instanceof DBXTreeItem) {
                final DBXTreeItem item = (DBXTreeItem) child;
                boolean isLoaded = loadTreeItems(monitor, item, toList);
                if (!isLoaded && item.isOptional()) {
                    loadChildren(monitor, item, toList);
                }
            } else if (child instanceof DBXTreeFolder) {
                toList.add(
                    new DBNTreeFolder(DBNTreeNode.this, (DBXTreeFolder) child));
            } else if (child instanceof DBXTreeObject) {
                toList.add(
                    new DBNTreeObject(DBNTreeNode.this, (DBXTreeObject) child));
            } else {
                log.warn("Unsupported meta node type: " + child);
            }
            monitor.worked(1);
        }
        monitor.done();
    }

    /**
     * Extract items using reflect api
     * @param monitor
     *@param meta items meta info
     * @param toList list ot add new items   @return true on success
     * @throws DBException on any DB error
     */
    private boolean loadTreeItems(DBRProgressMonitor monitor, DBXTreeItem meta, List<DBNTreeNode> toList)
        throws DBException
    {
        // Read property using reflection
        Object object = getValueObject();
        if (object == null) {
            return false;
        }
        String propertyName = meta.getPropertyName();
        Object propertyValue = LoadingUtils.extractPropertyValue(monitor, object, propertyName);
        if (propertyValue == null) {
            return false;
        }
        if (!(propertyValue instanceof Collection<?>)) {
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
            DBNTreeItem treeItem = new DBNTreeItem(
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
