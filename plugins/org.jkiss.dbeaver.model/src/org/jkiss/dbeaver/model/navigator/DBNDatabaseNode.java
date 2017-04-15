/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

/**
 * DBNDatabaseNode
 */
public abstract class DBNDatabaseNode extends DBNNode implements DBSWrapper, DBPContextProvider, IDataSourceContainerProvider {

    private static final DBNDatabaseNode[] EMPTY_NODES = new DBNDatabaseNode[0];

    private volatile boolean locked;
    protected volatile DBNDatabaseNode[] childNodes;
    private boolean filtered;

    protected DBNDatabaseNode(DBNNode parentNode)
    {
        super(parentNode);
    }

    protected void registerNode() {
        DBNModel model = getModel();
        if (model != null) {
            model.addNode(this);
        }
    }

    protected void unregisterNode(boolean reflect) {
        DBNModel model = getModel();
        if (model != null) {
            model.removeNode(this, reflect);
        }
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
        DBSObject object = getObject();
        if (object == null) {
            return DBConstants.NULL_VALUE_LABEL;
        }
        String objectName;
        if (object instanceof DBPOverloadedObject) {
            objectName = ((DBPOverloadedObject) object).getOverloadedName();
        } else {
            objectName = object.getName();
        }
        if (CommonUtils.isEmpty(objectName)) {
            objectName = object.toString();
            if (CommonUtils.isEmpty(objectName)) {
                objectName = object.getClass().getName() + "@" + object.hashCode(); //$NON-NLS-1$
            }
        }
/*
        if (object instanceof DBPUniqueObject) {
            String uniqueName = ((DBPUniqueObject) object).getUniqueName();
            if (!uniqueName.equals(objectName)) {
                if (uniqueName.startsWith(objectName)) {
                    uniqueName = uniqueName.substring(objectName.length());
                }
                objectName += " (" + uniqueName + ")";
            }
        }
*/
        return objectName;
    }

    @Override
    public String getNodeFullName()
    {
        if (getObject() instanceof DBPQualifiedObject) {
            return ((DBPQualifiedObject)getObject()).getFullyQualifiedName(DBPEvaluationContext.UI);
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
    public DBPImage getNodeIcon()
    {
        final DBSObject object = getObject();
        DBPImage image = DBValueFormatting.getObjectImage(object, false);
        if (image == null) {
            DBXTreeNode meta = getMeta();
            if (meta != null) {
                image = meta.getIcon(this);
            }
        }
        if (image != null && object instanceof DBPStatefulObject) {
            image = DBNModel.getStateOverlayImage(image, ((DBPStatefulObject) object).getObjectState());
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
        DBNDatabaseNode[] children = getChildren(monitor);
        if (!ArrayUtils.isEmpty(children)) {
            for (DBNDatabaseNode child : children) {
                if (child.getMeta() == childType) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized DBNDatabaseNode[] getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        if (childNodes == null && hasChildren(false)) {
            if (this.initializeNode(monitor, null)) {
                final List<DBNDatabaseNode> tmpList = new ArrayList<>();
                loadChildren(monitor, getMeta(), null, tmpList, true);
                if (!monitor.isCanceled()) {
                    if (tmpList.isEmpty()) {
                        this.childNodes = EMPTY_NODES;
                    } else {
                        this.childNodes = tmpList.toArray(new DBNDatabaseNode[tmpList.size()]);
                    }
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

    DBNDatabaseNode[] getChildNodes()
    {
        return childNodes;
    }

    boolean hasChildItem(DBSObject object)
    {
        if (childNodes != null) {
            for (DBNDatabaseNode child : childNodes) {
                if (child.getObject() == object) {
                    return true;
                }
            }
        }
        return false;
    }

    void addChildItem(DBSObject object)
    {
        DBXTreeItem metaChildren = getItemsMeta();
        if (metaChildren != null) {
            final DBNDatabaseItem newChild = new DBNDatabaseItem(this, metaChildren, object, false);
            synchronized (this) {
                childNodes = ArrayUtils.add(DBNDatabaseNode.class, childNodes, newChild);
            }
            getModel().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, DBNEvent.NodeChange.LOAD, newChild));
        } else {
            log.error("Cannot add child item to " + getNodeName() + ". Conditions doesn't met"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    void removeChildItem(DBSObject object)
    {
        DBNNode childNode = null;
        synchronized (this) {
            if (!ArrayUtils.isEmpty(childNodes)) {
                for (int i = 0; i < childNodes.length; i++) {
                    final DBNDatabaseNode child = childNodes[i];
                    if (child.getObject() == object) {
                        childNode = child;
                        childNodes = ArrayUtils.remove(DBNDatabaseNode.class, childNodes, i);
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

    /**
     * Reorder children nodes
     */
    public void updateChildrenOrder(boolean reflect) {
        try {
            refreshNodeContent(new VoidProgressMonitor(), getObject(), this, reflect);
        } catch (DBException e) {
            log.error("Error reordering node children", e);
        }

    }

    public boolean needsInitialization()
    {
        return childNodes == null && hasChildren(false);
    }

    @Override
    public boolean isLocked()
    {
        return locked || super.isLocked();
    }

    public boolean initializeNode(DBRProgressMonitor monitor, DBRProgressListener onFinish)
    {
        if (onFinish != null) {
            onFinish.onTaskFinished(Status.OK_STATUS);
        }
        return true;
    }

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
        DBSObject object = getObject();
        if (object instanceof DBPRefreshableObject) {
            if (object.isPersisted()) {
                DBSObject newObject = ((DBPRefreshableObject) object).refreshObject(monitor);
                if (newObject == null) {
                    if (parentNode instanceof DBNDatabaseNode) {
                        ((DBNDatabaseNode) parentNode).removeChildItem(object);
                    }
                    return null;
                } else {
                    refreshNodeContent(monitor, newObject, source, true);
                    return this;
                }
            } else {
                // Not persisted node - nothing to refresh
                getModel().fireNodeUpdate(source, this, DBNEvent.NodeChange.REFRESH);
                return this;
            }
        } else {
            return super.refreshNode(monitor, source);
        }
    }

    private void refreshNodeContent(final DBRProgressMonitor monitor, DBSObject newObject, Object source, boolean reflect)
        throws DBException
    {
        if (isDisposed()) {
            return;
        }
        this.locked = true;
        DBNModel model = getModel();
        try {
            if (newObject != getObject()) {
                reloadObject(monitor, newObject);
            }
            if (reflect) model.fireNodeUpdate(source, this, DBNEvent.NodeChange.LOCK);

            this.reloadChildren(monitor, reflect);

            if (reflect) model.fireNodeUpdate(source, this, DBNEvent.NodeChange.REFRESH);
        } finally {
            this.locked = false;

            // Unlock node
            if (reflect) model.fireNodeUpdate(source, this, DBNEvent.NodeChange.UNLOCK);
        }
    }

    protected void clearChildren(boolean reflect)
    {
        DBNDatabaseNode[] childrenCopy;
        synchronized (this) {
            childrenCopy = childNodes == null ? null : Arrays.copyOf(childNodes, childNodes.length);
            childNodes = null;
        }
        if (childrenCopy != null) {
            for (DBNNode child : childrenCopy) {
                child.dispose(reflect);
            }
        }
    }

    private void loadChildren(
        DBRProgressMonitor monitor,
        final DBXTreeNode meta,
        final DBNDatabaseNode[] oldList,
        final List<DBNDatabaseNode> toList,
        boolean reflect)
        throws DBException
    {
        if (monitor.isCanceled()) {
            return;
        }
        this.filtered = false;

        List<DBXTreeNode> childMetas = meta.getChildren(this);
        if (CommonUtils.isEmpty(childMetas)) {
            return;
        }
        monitor.beginTask(ModelMessages.model_navigator_load_items_, childMetas.size());

        for (DBXTreeNode child : childMetas) {
            if (monitor.isCanceled()) {
                break;
            }
            monitor.subTask(ModelMessages.model_navigator_load_ + " " + child.getChildrenType(getObject().getDataSource()));
            if (child instanceof DBXTreeItem) {
                final DBXTreeItem item = (DBXTreeItem) child;
                boolean isLoaded = loadTreeItems(monitor, item, oldList, toList, reflect);
                if (!isLoaded && item.isOptional() && item.getRecursiveLink() == null) {
                    // This may occur only if no child nodes was read
                    // Then we try to go on next DBX level
                    loadChildren(monitor, item, oldList, toList, reflect);
                }
            } else if (child instanceof DBXTreeFolder) {
                if (oldList == null) {
                    // Load new folders only if there are no old ones
                    toList.add(
                        new DBNDatabaseFolder(this, (DBXTreeFolder) child));
                } else {
                    for (DBNDatabaseNode oldFolder : oldList) {
                        if (oldFolder.getMeta() == child) {
                            oldFolder.reloadChildren(monitor, reflect);
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
                            oldObject.reloadChildren(monitor, reflect);
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

        if (reflect && filtered) {
            getModel().fireNodeUpdate(this, this, DBNEvent.NodeChange.REFRESH);
        }
    }


    /**
     * Extract items using reflect api
     * @param monitor progress monitor
     * @param meta items meta info
     * @param oldList previous child items
     * @param toList list ot add new items   @return true on success
     * @param reflect
     * @return true on success
     * @throws DBException on any DB error
     */
    private boolean loadTreeItems(
        DBRProgressMonitor monitor,
        DBXTreeItem meta,
        final DBNDatabaseNode[] oldList,
        final List<DBNDatabaseNode> toList,
        boolean reflect)
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
        Object propertyValue = extractPropertyValue(monitor, valueObject, propertyName);
        if (propertyValue == null) {
            return false;
        }
        if (!(propertyValue instanceof Collection<?>)) {
            log.warn("Bad property '" + propertyName + "' value: " + propertyValue.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }

        DBSObjectFilter filter = getNodeFilter(meta, false);
        this.filtered = filter != null && !filter.isNotApplicable();

        Collection<?> itemList = (Collection<?>) propertyValue;
        if (itemList.isEmpty()) {
            return false;
        }
        if (this.isDisposed()) {
            // Property reading can take really long time so this node can be disposed at this moment -
            // check it
            return false;
        }

        DBPDataSourceContainer dataSourceContainer = getDataSourceContainer();
        boolean showSystem = dataSourceContainer == null || dataSourceContainer.isShowSystemObjects();
        for (Object childItem : itemList) {
            if (childItem == null) {
                continue;
            }
            if (!(childItem instanceof DBSObject)) {
                log.warn("Bad item type: " + childItem.getClass().getName()); //$NON-NLS-1$
                continue;
            }
            if (DBUtils.isHiddenObject(childItem)) {
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

                        if (oldChild.hasChildren(false) && !oldChild.needsInitialization()) {
                            // Refresh children recursive
                            oldChild.reloadChildren(monitor, reflect);
                        }
                        if (reflect) {
                            getModel().fireNodeUpdate(this, oldChild, DBNEvent.NodeChange.REFRESH);
                        }

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

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ? null : dataSource.getDefaultContext(true);
    }

    @NotNull
    public DBPDataSourceContainer getDataSourceContainer()
    {
        for (DBNNode p = getParentNode(); p != null; p = p.getParentNode()) {
            if (p instanceof DBNDataSource) {
                return ((DBNDataSource) p).getDataSourceContainer();
            }
        }
        throw new IllegalStateException("No parent datasource node");
    }

    public DBPDataSource getDataSource() {
        DBSObject object = getObject();
        return object == null ? null : object.getDataSource();
    }

    public DBSObjectFilter getNodeFilter(DBXTreeItem meta, boolean firstMatch)
    {
        DBPDataSourceContainer dataSource = getDataSourceContainer();
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
        DBPDataSourceContainer dataSource = getDataSourceContainer();
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

    @Override
    public String getNodeItemPath() {
        StringBuilder pathName = new StringBuilder(100);

        for (DBNNode node = this; node instanceof DBNDatabaseNode; node = node.getParentNode()) {
            if (node instanceof DBNDataSource) {
                if (pathName.length() > 0) {
                    pathName.insert(0, '/');
                }
                pathName.insert(0, ((DBNDataSource) node).getDataSourceContainer().getId());
            } else if (node instanceof DBNDatabaseFolder) {
                if (pathName.length() > 0) {
                    pathName.insert(0, '/');
                }
                String type = ((DBNDatabaseFolder) node).getMeta().getType();
                if (CommonUtils.isEmpty(type)) {
                    type = node.getName();
                }
                pathName.insert(0, type);
            }
            if (!(node instanceof DBNDatabaseItem) && !(node instanceof DBNDatabaseObject)) {
                // skip folders
                continue;
            }

            if (pathName.length() > 0) {
                pathName.insert(0, '/');
            }
            pathName.insert(0, node.getNodeName().replace('/', '_'));
        }
        pathName.insert(0, NodePathType.database.getPrefix());
        return pathName.toString();
    }

    protected void reloadChildren(DBRProgressMonitor monitor, boolean reflect)
        throws DBException
    {
        DBNDatabaseNode[] oldChildren;
        synchronized (this) {
            if (childNodes == null) {
                // Nothing to reload
                return;
            }
            oldChildren = Arrays.copyOf(childNodes, childNodes.length);
        }
        List<DBNDatabaseNode> newChildren = new ArrayList<>();
        loadChildren(monitor, getMeta(), oldChildren, newChildren, reflect);
        synchronized (this) {
            childNodes = newChildren.toArray(new DBNDatabaseNode[newChildren.size()]);
        }
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
            List<Class<?>> result = new ArrayList<>();
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
        Method getter = findPropertyReadMethod(valueObject.getClass(), propertyName);
        if (getter == null) {
            return null;
        }
        Type propType = getter.getGenericReturnType();
        return BeanUtils.getCollectionType(propType);
    }

    public IProject getOwnerProject() {
        for (DBNNode node = getParentNode(); node != null; node = node.getParentNode()) {
            if (node instanceof DBNProject) {
                return ((DBNProject) node).getProject();
            }
        }
        return null;
    }
    ////////////////////////////////////////////////////////////////////////////////////
    // Reflection utils

    public static Object extractPropertyValue(DBRProgressMonitor monitor, Object object, String propertyName)
        throws DBException
    {
        // Read property using reflection
        if (object == null) {
            return null;
        }
        try {
            Method getter = findPropertyReadMethod(object.getClass(), propertyName);
            if (getter == null) {
                log.warn("Can't find property '" + propertyName + "' read method in '" + object.getClass().getName() + "'");
                return null;
            }
            Class<?>[] paramTypes = getter.getParameterTypes();
            if (paramTypes.length == 0) {
                // No params - just read it
                return getter.invoke(object);
            } else if (paramTypes.length == 1 && paramTypes[0] == DBRProgressMonitor.class) {
                // Read with progress monitor
                return getter.invoke(object, monitor);
            } else {
                log.warn("Can't read property '" + propertyName + "' - bad method signature: " + getter.toString());
                return null;
            }
        }
        catch (IllegalAccessException ex) {
            log.warn("Error accessing items " + propertyName, ex);
            return null;
        }
        catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof DBException) {
                throw (DBException) ex.getTargetException();
            }
            throw new DBException("Can't read " + propertyName, ex.getTargetException());
        }
    }

    public static Method findPropertyReadMethod(Class<?> clazz, String propertyName)
    {
        String methodName = BeanUtils.propertyNameToMethodName(propertyName);
        return findPropertyGetter(clazz, "get" + methodName, "is" + methodName);
    }

    private static Method findPropertyGetter(Class<?> clazz, String getName, String isName)
    {
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            if (
                (!Modifier.isPublic(method.getModifiers())) ||
                    (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) ||
                    (method.getReturnType().equals(void.class)))
            {
                // skip
            } else if (method.getName().equals(getName) || (method.getName().equals(isName) && method.getReturnType().equals(boolean.class))) {
                // If it matches the get name, it's the right method
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0 || (parameterTypes.length == 1 && parameterTypes[0] == DBRProgressMonitor.class)) {
                    return method;
                }
            }
        }
        return clazz == Object.class ? null : findPropertyGetter(clazz.getSuperclass(), getName, isName);
    }

}
