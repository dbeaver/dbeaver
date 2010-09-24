/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import net.sf.jkiss.utils.CommonUtils;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        List<DBNTreeNode> children = getChildren(monitor);
        if (!CommonUtils.isEmpty(children)) {
            for (DBNTreeNode child : children) {
                if (child.getMeta() == childType) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<DBNTreeNode> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.hasChildren() && childNodes == null) {
            if (this.initializeNode(monitor)) {
                final List<DBNTreeNode> tmpList = new ArrayList<DBNTreeNode>();
                loadChildren(monitor, getMeta(), null, tmpList);
                this.childNodes = tmpList;
            }
        }
        return childNodes;
    }

    @Override
    void clearNode() {
        clearChildren();
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

    private void loadChildren(
        DBRProgressMonitor monitor,
        final DBXTreeNode meta,
        final List<DBNTreeNode> oldList,
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
                boolean isLoaded = loadTreeItems(monitor, item, oldList, toList);
                if (!isLoaded && item.isOptional()) {
                    // This may occur only if no child nodes was read
                    // Then we try to go on next DBX level
                    loadChildren(monitor, item, oldList, toList);
                }
            } else if (child instanceof DBXTreeFolder) {
                if (oldList == null) {
                    // Load new folders only if there are no old ones
                    toList.add(
                        new DBNTreeFolder(this, (DBXTreeFolder) child));
                } else {
                    for (DBNTreeNode oldFolder : oldList) {
                        if (oldFolder.getMeta() == child) {
                            oldFolder.reloadChildren(monitor);
                            toList.add(oldFolder);
                            break;
                        }
                    }
                }
            } else if (child instanceof DBXTreeObject) {
                if (oldList == null) {
                    // Load new objects only if there are no old ones
                    toList.add(
                        new DBNTreeObject(this, (DBXTreeObject) child));
                } else {
                    for (DBNTreeNode oldObject : oldList) {
                        if (oldObject.getMeta() == child) {
                            oldObject.reloadChildren(monitor);
                            toList.add(oldObject);
                            break;
                        }
                    }
                }
            } else {
                log.warn("Unsupported meta node type: " + child);
            }
            monitor.worked(1);
        }
        monitor.done();
    }

    /**
     * Extract items using reflect api
     * @param monitor progress monitor
     * @param meta items meta info
     * @param toList list ot add new items   @return true on success
     * @throws DBException on any DB error
     */
    private boolean loadTreeItems(
        DBRProgressMonitor monitor,
        DBXTreeItem meta,
        final List<DBNTreeNode> oldList,
        final List<DBNTreeNode> toList)
        throws DBException
    {
        // Read property using reflection
        Object valueObject = getValueObject();
        if (valueObject == null) {
            return false;
        }
        String propertyName = meta.getPropertyName();
        Object propertyValue = LoadingUtils.extractPropertyValue(monitor, valueObject, propertyName);
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
            // Property reading can take really long time so this node can be disposed at this moment -
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
            DBSObject object = (DBSObject)childItem;
            boolean added = false;
            if (oldList != null) {
                // Check that new object is a replacement of old one
                for (DBNTreeNode oldChild : oldList) {
                    if (equalObjects(oldChild.getObject(), object)) {
                        oldChild.reloadObject(monitor, object);

                        if (oldChild.hasChildren() && !oldChild.isLazyNode()) {
                            // Refresh children recursive
                            oldChild.reloadChildren(monitor);
                        }
                        getModel().fireNodeUpdate(this, oldChild, DBNEvent.NodeChange.REFRESH);

                        toList.add(oldChild);
                        added = true;
                        break;
                    }
                }
            }
            if (!added) {
                // Simply add new item
                DBNTreeItem treeItem = new DBNTreeItem(this, meta, object);
                toList.add(treeItem);
            }
        }

        if (oldList != null) {
            // Now remove all non-existing items
            for (DBNTreeNode oldChild : oldList) {
                boolean found = false;
                for (Object childItem : itemList) {
                    if (childItem instanceof DBSObject && equalObjects(oldChild.getObject(), (DBSObject) childItem)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Remove old child object
                    oldChild.dispose();
                }
            }
        }
        return true;
    }

    protected void reloadChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        if (childNodes == null) {
            // Nothing to reload
            return;
        }
        List<DBNTreeNode> newChildren = new ArrayList<DBNTreeNode>();
        loadChildren(monitor, getMeta(), childNodes, newChildren);
        childNodes = newChildren;
    }

    private static boolean equalObjects(DBSObject object1, DBSObject object2) {
        return object1 != null && object2 != null &&
            CommonUtils.equalObjects(object1.getObjectId(), object2.getObjectId());
    }

    public abstract DBXTreeNode getMeta();

    protected abstract void reloadObject(DBRProgressMonitor monitor, DBSObject object);
}
