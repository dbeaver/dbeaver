/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import net.sf.jkiss.utils.BeanUtils;
import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionFilter;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.tree.DBXTreeFolder;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.registry.tree.DBXTreeObject;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

/**
 * DBNDatabaseNode
 */
public abstract class DBNDatabaseNode extends DBNNode implements IActionFilter, DBSWrapper {
    static final Log log = LogFactory.getLog(DBNDatabaseNode.class);

    private boolean locked;
    protected List<DBNDatabaseNode> childNodes;

    protected DBNDatabaseNode(DBNNode parentNode)
    {
        super(parentNode);
    }

    void dispose(boolean reflect)
    {
        if (childNodes != null) {
            clearChildren(reflect);
            childNodes = null;
        }
        super.dispose(reflect);
    }

    @Override
    public String getNodeType()
    {
        return getMeta().getLabel();
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
        List<DBNDatabaseNode> children = getChildren(monitor);
        if (!CommonUtils.isEmpty(children)) {
            for (DBNDatabaseNode child : children) {
                if (child.getMeta() == childType) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<DBNDatabaseNode> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.hasChildren() && childNodes == null) {
            if (this.initializeNode(null)) {
                final List<DBNDatabaseNode> tmpList = new ArrayList<DBNDatabaseNode>();
                loadChildren(monitor, getMeta(), null, tmpList);
                this.childNodes = tmpList;
            }
        }
        return childNodes;
    }

    List<DBNDatabaseNode> getChildNodes()
    {
        return childNodes;
    }

    void addChildItem(DBSObject object)
    {
        final List<DBXTreeNode> metaChildren = getMeta().getChildren();
        if (!CommonUtils.isEmpty(metaChildren) && metaChildren.size() == 1 && metaChildren.get(0) instanceof DBXTreeItem) {
            final DBNDatabaseItem newChild = new DBNDatabaseItem(this, (DBXTreeItem) metaChildren.get(0), object, false);
            childNodes.add(newChild);
            getModel().addNode(newChild, true);
        } else {
            log.error("Cannot add child item to " + getNodeName() + ". Conditions doesn't met");
        }
    }

    void removeChildItem(DBSObject object)
    {
        if (!CommonUtils.isEmpty(childNodes)) {
            for (Iterator<DBNDatabaseNode> iter = childNodes.iterator(); iter.hasNext(); ) {
                final DBNDatabaseNode child = iter.next();
                if (child.getObject() == object) {
                    iter.remove();
                    child.dispose(true);
                }
            }
        }
    }

    @Override
    void clearNode(boolean reflect) {
        clearChildren(reflect);
    }

    public boolean isLazyNode()
    {
        return childNodes == null;
    }

    public boolean isLocked()
    {
        return locked || super.isLocked();
    }

    public boolean initializeNode(Runnable onFinish)
    {
        return true;
    }

    /**
     * Refreshes node.
     * If refresh cannot be done in this level then refreshes parent node.
     * Do not actually changes navigation tree. If some underlying object is refreshed it must fire DB model
     * event which will cause actual tree nodes refresh. Underlying object could present multiple times in
     * navigation model - each occurrence will be refreshed then.
     * @param monitor progress monitor
     * @return real refreshed node or null if nothing was refreshed
     * @throws DBException on any internal exception
     */
    public DBNNode refreshNode(DBRProgressMonitor monitor) throws DBException
    {
        if (isLocked()) {
            log.warn("Attempt to refresh locked node '" + getNodeName() + "'");
            return null;
        }
        if (getObject() instanceof DBSEntity && ((DBSEntity)getObject()).refreshEntity(monitor)) {
            refreshNodeContent(monitor);
            return this;
        } else {
            return super.refreshNode(monitor);
        }
    }

    private void refreshNodeContent(final DBRProgressMonitor monitor)
        throws DBException
    {
        if (isDisposed()) {
            return;
        }
        this.locked = true;
        try {
            this.getModel().fireNodeUpdate(this, this, DBNEvent.NodeChange.LOCK);

            try {
                this.reloadChildren(monitor);
            } catch (DBException e) {
                log.error(e);
            }

            this.getModel().fireNodeUpdate(this, this, DBNEvent.NodeChange.REFRESH);
        } finally {
            this.locked = false;

            // Unlock node
            this.getModel().fireNodeUpdate(this, this, DBNEvent.NodeChange.UNLOCK);
        }
        //new RefreshJob("Refresh node " + getNodeName()).schedule();
    }

    protected void clearChildren(boolean reflect)
    {
        if (childNodes != null) {
            for (DBNNode child : childNodes) {
                child.dispose(reflect);
            }
            childNodes.clear();
            childNodes = null;
        }
    }

    private void loadChildren(
        DBRProgressMonitor monitor,
        final DBXTreeNode meta,
        final List<DBNDatabaseNode> oldList,
        final List<DBNDatabaseNode> toList)
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
                        new DBNDatabaseFolder(this, (DBXTreeFolder) child));
                } else {
                    for (DBNDatabaseNode oldFolder : oldList) {
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
                        new DBNDatabaseObject(this, (DBXTreeObject) child));
                } else {
                    for (DBNDatabaseNode oldObject : oldList) {
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
        final List<DBNDatabaseNode> oldList,
        final List<DBNDatabaseNode> toList)
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
                for (DBNDatabaseNode oldChild : oldList) {
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
                DBNDatabaseItem treeItem = new DBNDatabaseItem(this, meta, object, oldList != null);
                toList.add(treeItem);
            }
        }

        if (oldList != null) {
            // Now remove all non-existing items
            for (DBNDatabaseNode oldChild : oldList) {
                boolean found = false;
                for (Object childItem : itemList) {
                    if (childItem instanceof DBSObject && equalObjects(oldChild.getObject(), (DBSObject) childItem)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Remove old child object
                    oldChild.dispose(true);
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
        List<DBNDatabaseNode> newChildren = new ArrayList<DBNDatabaseNode>();
        loadChildren(monitor, getMeta(), childNodes, newChildren);
        childNodes = newChildren;
    }

    public boolean testAttribute(Object target, String name, String value) {
        if (name.equals("targetType")) {
            try {
                Class<?> targetClass = Class.forName(value);
                return targetClass.isAssignableFrom(getObject().getClass());
            } catch (ClassNotFoundException e) {
                log.warn("Unknown target type: " + value);
            }
        }
        return false;
    }

    private static boolean equalObjects(DBSObject object1, DBSObject object2) {
        while (object1 != null && object2 != null) {
            if (object1.getClass() != object2.getClass() || !CommonUtils.equalObjects(object1.getName(), object2.getName())) {
                return false;
            }
            object1 = object1.getParentObject();
            object2 = object2.getParentObject();
        }
        return true;
    }

    public abstract Object getValueObject();

    public abstract DBXTreeNode getMeta();

    protected abstract void reloadObject(DBRProgressMonitor monitor, DBSObject object);

    public List<Class<?>> getChildrenTypes()
    {
        List<DBXTreeNode> childMetas = getMeta().getChildren();
        if (CommonUtils.isEmpty(childMetas)) {
            return null;
        } else {
            List<Class<?>> result = new ArrayList<Class<?>>();
            for (DBXTreeNode childMeta : childMetas) {
                if (childMeta instanceof DBXTreeItem) {
                    Class<?> childrenType = getChildrenType((DBXTreeItem) childMeta);
                    if (childrenType != null) {
                        result.add(childrenType);
                    }
                }
            }
            return result;
        }
    }

    private Class<?> getChildrenType(DBXTreeItem childMeta)
    {
        Object valueObject = getValueObject();
        if (valueObject == null) {
            return null;
        }
        String propertyName = childMeta.getPropertyName();
        Method getter = LoadingUtils.findPropertyReadMethod(valueObject.getClass(), propertyName);
        if (getter == null) {
            return null;
        }
        Type propType = getter.getGenericReturnType();
        return BeanUtils.getCollectionType(propType);
    }

}
