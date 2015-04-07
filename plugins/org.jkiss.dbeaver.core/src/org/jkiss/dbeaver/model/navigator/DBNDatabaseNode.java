/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionFilter;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSObjectStateful;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.tree.DBXTreeFolder;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.registry.tree.DBXTreeObject;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

/**
 * DBNDatabaseNode
 */
public abstract class DBNDatabaseNode extends DBNNode implements IActionFilter, DBSWrapper, IDataSourceContainerProvider {

    private volatile boolean locked;
    protected volatile List<DBNDatabaseNode> childNodes;
    private boolean filtered;

    protected DBNDatabaseNode(DBNNode parentNode)
    {
        super(parentNode);
    }

    @Override
    void dispose(boolean reflect)
    {
        clearChildren(reflect);
        super.dispose(reflect);
    }

    @Override
    public String getNodeType()
    {
        return getObject() == null ? "" : getMeta().getNodeType(getObject().getDataSource()); //$NON-NLS-1$
    }

    @Override
    public String getNodeName()
    {
        if (getObject() == null) {
            return DBConstants.NULL_VALUE_LABEL;
        }
        String objectName = getObject().getName();
        if (CommonUtils.isEmpty(objectName)) {
            objectName = "?"; //$NON-NLS-1$
        }
        return objectName;
    }

    @Override
    public String getNodeFullName()
    {
        if (getObject() instanceof DBPQualifiedObject) {
            return ((DBPQualifiedObject)getObject()).getFullQualifiedName();
        } else {
            return super.getNodeFullName();
        }
    }

    @Override
    public String getNodeDescription()
    {
        return getObject() == null ? null : getObject().getDescription();
    }

    @Override
    public Image getNodeIcon()
    {
        Image image = null;
        final DBSObject object = getObject();
        if (object instanceof IObjectImageProvider) {
            image = ((IObjectImageProvider) object).getObjectImage();
        }
        if (image == null) {
            DBXTreeNode meta = getMeta();
            if (meta != null) {
                image = meta.getIcon(this);
            }
        }
        if (image != null && object instanceof DBSObjectStateful) {
            image = DBNModel.getStateOverlayImage(image, ((DBSObjectStateful) object).getObjectState());
        }
        return image;
    }

    @Override
    public boolean allowsChildren()
    {
        return !isDisposed() && this.getMeta().hasChildren(this);
    }

    @Override
    public boolean allowsNavigableChildren()
    {
        return !isDisposed() && this.getMeta().hasChildren(this, true);
    }

    public boolean hasChildren(DBRProgressMonitor monitor, DBXTreeNode childType)
        throws DBException
    {
        if (isDisposed()) {
            return false;
        }
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

    @Override
    public List<DBNDatabaseNode> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        if (childNodes == null && allowsChildren()) {
            if (this.initializeNode(monitor, null)) {
                final List<DBNDatabaseNode> tmpList = new ArrayList<DBNDatabaseNode>();
                loadChildren(monitor, getMeta(), null, tmpList);
                if (!monitor.isCanceled()) {
                    this.childNodes = tmpList;
                    this.afterChildRead();
                }
            }
        }
        return childNodes;
    }

    protected void afterChildRead()
    {
        // Do nothing
    }

    List<DBNDatabaseNode> getChildNodes()
    {
        return childNodes;
    }

    void addChildItem(DBSObject object)
    {
        DBXTreeItem metaChildren = getItemsMeta();
        if (metaChildren != null) {
            final DBNDatabaseItem newChild = new DBNDatabaseItem(this, metaChildren, object, false);
            synchronized (this) {
                childNodes.add(newChild);
            }
            DBNModel.getInstance().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, DBNEvent.NodeChange.LOAD, newChild));
        } else {
            log.error("Cannot add child item to " + getNodeName() + ". Conditions doesn't met"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    void removeChildItem(DBSObject object)
    {
        DBNNode childNode = null;
        synchronized (this) {
            if (!CommonUtils.isEmpty(childNodes)) {
                for (Iterator<DBNDatabaseNode> iter = childNodes.iterator(); iter.hasNext(); ) {
                    final DBNDatabaseNode child = iter.next();
                    if (child.getObject() == object) {
                        childNode = child;
                        iter.remove();
                        break;
                    }
                }
            }
        }
        if (childNode != null) {
            childNode.dispose(true);
        }
    }

    @Override
    void clearNode(boolean reflect) {
        clearChildren(reflect);
    }

    public boolean needsInitialization()
    {
        return childNodes == null && allowsChildren();
    }

    @Override
    public boolean isLocked()
    {
        return locked || super.isLocked();
    }

    public boolean initializeNode(DBRProgressMonitor monitor, DBRProcessListener onFinish)
    {
        if (onFinish != null) {
            onFinish.onProcessFinish(Status.OK_STATUS);
        }
        return true;
    }

/*
    @Override
    public boolean supportsRename()
    {
        final DBSObject object = getObject();
        return !(object == null || !object.isPersisted()) &&
            DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectRenamer.class) != null;
    }
*/

    /**
     * Refreshes node.
     * If refresh cannot be done in this level then refreshes parent node.
     * Do not actually changes navigation tree. If some underlying object is refreshed it must fire DB model
     * event which will cause actual tree nodes refresh. Underlying object could present multiple times in
     * navigation model - each occurrence will be refreshed then.
     *
     * @param monitor progress monitor
     * @param source source object
     * @return real refreshed node or null if nothing was refreshed
     * @throws DBException on any internal exception
     */
    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException
    {
        if (isLocked()) {
            log.warn("Attempt to refresh locked node '" + getNodeName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        if (getObject() instanceof DBPRefreshableObject && ((DBPRefreshableObject)getObject()).refreshObject(monitor)) {
            refreshNodeContent(monitor, source);
            return this;
        } else {
            return super.refreshNode(monitor, source);
        }
    }

    private void refreshNodeContent(final DBRProgressMonitor monitor, Object source)
        throws DBException
    {
        if (isDisposed()) {
            return;
        }
        this.locked = true;
        DBNModel model = DBNModel.getInstance();
        try {
            model.fireNodeUpdate(source, this, DBNEvent.NodeChange.LOCK);

            try {
                this.reloadChildren(monitor);
            } catch (DBException e) {
                log.error(e);
            }

            model.fireNodeUpdate(source, this, DBNEvent.NodeChange.REFRESH);
        } finally {
            this.locked = false;

            // Unlock node
            model.fireNodeUpdate(source, this, DBNEvent.NodeChange.UNLOCK);
        }
        //new RefreshJob("Refresh node " + getNodeName()).schedule();
    }

    protected void clearChildren(boolean reflect)
    {
        List<DBNDatabaseNode> childrenCopy;
        synchronized (this) {
            childrenCopy = childNodes;
            childNodes = null;
        }
        if (childrenCopy != null) {
            for (DBNNode child : childrenCopy) {
                child.dispose(reflect);
            }
            childrenCopy.clear();
        }
    }

    private void loadChildren(
        DBRProgressMonitor monitor,
        final DBXTreeNode meta,
        final List<DBNDatabaseNode> oldList,
        final List<DBNDatabaseNode> toList)
        throws DBException
    {
        this.filtered = false;

        List<DBXTreeNode> childMetas = meta.getChildren(this);
        if (CommonUtils.isEmpty(childMetas)) {
            return;
        }
        monitor.beginTask(CoreMessages.model_navigator_load_items_, childMetas.size());

        for (DBXTreeNode child : childMetas) {
            if (monitor.isCanceled()) {
                break;
            }
            monitor.subTask(CoreMessages.model_navigator_load_ + " " + child.getChildrenType(getObject().getDataSource()));
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
                log.warn("Unsupported meta node type: " + child); //$NON-NLS-1$
            }
            monitor.worked(1);
        }
        monitor.done();
        
        if (filtered) {
            DBNModel.getInstance().fireNodeUpdate(this, this, DBNEvent.NodeChange.REFRESH);
        }
    }


    /**
     * Extract items using reflect api
     * @param monitor progress monitor
     * @param meta items meta info
     * @param oldList previous child items
     * @param toList list ot add new items   @return true on success
     * @return true on success
     * @throws DBException on any DB error
     */
    private boolean loadTreeItems(
        DBRProgressMonitor monitor,
        DBXTreeItem meta,
        final List<DBNDatabaseNode> oldList,
        final List<DBNDatabaseNode> toList)
        throws DBException
    {
        if (this.isDisposed()) {
            // Property reading can take really long time so this node can be disposed at this moment -
            // check it
            return false;
        }
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
            log.warn("Bad property '" + propertyName + "' value: " + propertyValue.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }

        DBSObjectFilter filter = getNodeFilter(meta, false);
        this.filtered = filter != null && !filter.isEmpty();

        Collection<?> itemList = (Collection<?>) propertyValue;
        if (itemList.isEmpty()) {
            return false;
        }
        if (this.isDisposed()) {
            // Property reading can take really long time so this node can be disposed at this moment -
            // check it
            return false;
        }

        DataSourceDescriptor dataSourceContainer = getDataSourceContainer();
        boolean showSystem = dataSourceContainer == null || dataSourceContainer.isShowSystemObjects();
        for (Object childItem : itemList) {
            if (childItem == null) {
                continue;
            }
            if (!(childItem instanceof DBSObject)) {
                log.warn("Bad item type: " + childItem.getClass().getName()); //$NON-NLS-1$
                continue;
            }
            if (childItem instanceof DBPHiddenObject && ((DBPHiddenObject) childItem).isHidden()) {
                // Skip hidden objects
                continue;
            }
            if (!showSystem && childItem instanceof DBPSystemObject && ((DBPSystemObject) childItem).isSystem()) {
                // Skip system objects
                continue;
            }
            if (filter != null && !filter.matches(((DBSObject)childItem).getName())) {
                // Doesn't match filter
                continue;
            }
            DBSObject object = (DBSObject)childItem;
            boolean added = false;
            if (oldList != null) {
                // Check that new object is a replacement of old one
                for (DBNDatabaseNode oldChild : oldList) {
                    if (oldChild.getMeta() == meta && equalObjects(oldChild.getObject(), object)) {
                        oldChild.reloadObject(monitor, object);

                        if (oldChild.allowsChildren() && !oldChild.needsInitialization()) {
                            // Refresh children recursive
                            oldChild.reloadChildren(monitor);
                        }
                        DBNModel.getInstance().fireNodeUpdate(this, oldChild, DBNEvent.NodeChange.REFRESH);

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
                if (oldChild.getMeta() != meta) {
                    // Wrong type
                    continue;
                }
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

    public DataSourceDescriptor getDataSourceContainer()
    {
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ? null : (DataSourceDescriptor) dataSource.getContainer();
    }

    public DBPDataSource getDataSource() {
        DBPDataSource dataSource = getObject().getDataSource();
        if(dataSource == null) {
            return null;
        }
        return dataSource;
    }

    public DBSObjectFilter getNodeFilter(DBXTreeItem meta, boolean firstMatch)
    {
        DataSourceDescriptor dataSource = getDataSourceContainer();
        if (dataSource != null && this instanceof DBNContainer) {
            Class<?> childrenClass = this.getChildrenClass(meta);
            if (childrenClass != null) {
                Object valueObject = getValueObject();
                DBSObject parentObject = null;
                if (valueObject instanceof DBSObject && !(valueObject instanceof DBPDataSource)) {
                    parentObject = (DBSObject) valueObject;
                }
                return dataSource.getObjectFilter(childrenClass, parentObject, firstMatch);
            }
        }
        return null;
    }

    public void setNodeFilter(DBXTreeItem meta, DBSObjectFilter filter)
    {
        DataSourceDescriptor dataSource = getDataSourceContainer();
        if (dataSource != null && this instanceof DBNContainer) {
            Class<?> childrenClass = this.getChildrenClass(meta);
            if (childrenClass != null) {
                Object parentObject = getValueObject();
                if (parentObject instanceof DBPDataSource) {
                    parentObject = null;
                }
                dataSource.setObjectFilter(
                    this.getChildrenClass(meta),
                    (DBSObject) parentObject,
                    filter);
                dataSource.persistConfiguration();
            }
        } else {
            log.error("No active datasource - can't save filter configuration");
        }
    }

    @Override
    public boolean isFiltered()
    {
        return filtered;
    }

    protected void reloadChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        List<DBNDatabaseNode> oldChildren;
        synchronized (this) {
            if (childNodes == null) {
                // Nothing to reload
                return;
            }
            oldChildren = new ArrayList<DBNDatabaseNode>(childNodes);
        }
        List<DBNDatabaseNode> newChildren = new ArrayList<DBNDatabaseNode>();
        loadChildren(monitor, getMeta(), oldChildren, newChildren);
        synchronized (this) {
            childNodes = newChildren;
        }
    }

    @Override
    public boolean testAttribute(Object target, String name, String value) {
        if (getObject() != null) {
            if (name.equals("targetType")) { //$NON-NLS-1$
                try {
                    Class<?> targetClass = Class.forName(value);
                    return targetClass.isAssignableFrom(getObject().getClass());
                } catch (ClassNotFoundException e) {
                    log.warn("Unknown target type: " + value); //$NON-NLS-1$
                }
            }
        }
        return false;
    }

    private static boolean equalObjects(DBSObject object1, DBSObject object2) {
        if (object1 == object2) {
            return true;
        }
        if (object1 == null || object2 == null) {
            return false;
        }
        while (object1 != null && object2 != null) {
            if (object1.getClass() != object2.getClass() ||
                !CommonUtils.equalObjects(DBUtils.getObjectUniqueName(object1), DBUtils.getObjectUniqueName(object2)))
            {
                return false;
            }
            object1 = object1.getParentObject();
            object2 = object2.getParentObject();
        }
        return true;
    }

    public abstract Object getValueObject();

    public abstract DBXTreeNode getMeta();

    public DBXTreeItem getItemsMeta()
    {
        List<DBXTreeNode> metaChildren = getMeta().getChildren(this);
        if (metaChildren != null && metaChildren.size() == 1 && metaChildren.get(0) instanceof DBXTreeItem) {
            return (DBXTreeItem)metaChildren.get(0);
        } else {
            return null;
        }
    }

    protected abstract void reloadObject(DBRProgressMonitor monitor, DBSObject object);

    public List<Class<?>> getChildrenTypes(DBXTreeNode useMeta)
    {
        List<DBXTreeNode> childMetas = useMeta == null ? getMeta().getChildren(this) : Collections.singletonList(useMeta);
        if (CommonUtils.isEmpty(childMetas)) {
            return null;
        } else {
            List<Class<?>> result = new ArrayList<Class<?>>();
            for (DBXTreeNode childMeta : childMetas) {
                if (childMeta instanceof DBXTreeItem) {
                    Class<?> childrenType = getChildrenClass((DBXTreeItem) childMeta);
                    if (childrenType != null) {
                        result.add(childrenType);
                    }
                }
            }
            return result;
        }
    }

    private Class<?> getChildrenClass(DBXTreeItem childMeta)
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
