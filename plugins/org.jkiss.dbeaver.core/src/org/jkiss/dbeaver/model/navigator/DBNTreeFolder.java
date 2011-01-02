/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import net.sf.jkiss.utils.BeanUtils;
import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeFolder;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.ICommandIds;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * DBNTreeFolder
 */
public class DBNTreeFolder extends DBNTreeNode implements DBNContainer
{
    private DBXTreeFolder meta;

    DBNTreeFolder(DBNNode parent, DBXTreeFolder meta)
    {
        super(parent);
        this.meta = meta;
        if (this.getModel() != null) {
            this.getModel().addNode(this);
        }
    }

    protected void dispose(boolean reflect)
    {
        if (this.getModel() != null) {
            this.getModel().removeNode(this, reflect);
        }
        super.dispose(reflect);
    }

    public DBXTreeFolder getMeta()
    {
        return meta;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {
        // do nothing
    }

    public DBSObject getObject()
    {
        return this;
    }

    public Object getValueObject()
    {
        return getParentNode() == null ? null : getParentNode().getValueObject();
    }

    public String getName()
    {
        return meta.getLabel();
    }

    public String getDescription()
    {
        return meta.getDescription();
    }

    public DBSObject getParentObject()
    {
        return getParentNode() == null ? null : getParentNode().getObject();
    }

    public DBPDataSource getDataSource()
    {
        return getParentObject() == null ? null : getParentObject().getDataSource();
    }

    public boolean isPersisted()
    {
        return true;
    }

    public String getDefaultCommandId()
    {
        return ICommandIds.CMD_OBJECT_OPEN;
    }

    public Class<? extends DBSObject> getItemsClass()
    {
        String itemsType = CommonUtils.toString(meta.getType());
        if (CommonUtils.isEmpty(itemsType)) {
            return null;
        }
        Class<?> aClass = meta.getSource().getObjectClass(itemsType);
        if (aClass == null) {
            log.error("Items class '" + itemsType + "' not found");
            return null;
        }
        if (!DBSObject.class.isAssignableFrom(aClass)) {
            log.error("Class '" + aClass.getName() + "' doesn't extend DBSObject");
            return null;
        }
        return (Class<DBSObject>)aClass ;
    }

    private Class getChildrenType(DBXTreeItem childMeta)
    {
        Object valueObject = getValueObject();
        if (valueObject == null) {
            return null;
        }
        String propertyName = childMeta.getPropertyName();
        Method getter = LoadingUtils.findPropertyReadMethod(valueObject.getClass(), propertyName);
        Type propType = getter.getGenericReturnType();
        return BeanUtils.getCollectionType(propType);
    }

    public DBNTreeItem addChildItem(DBRProgressMonitor monitor, DBSObject childObject) throws DBException
    {
        List<DBXTreeNode> childMetas = getMeta().getChildren();
        if (childMetas.size() != 1 || !(childMetas.get(0) instanceof DBXTreeItem)) {
            throw new DBException("It's not allowed to add child items to node '" + getNodeName() + "'");
        }
        DBXTreeItem childMeta = (DBXTreeItem)childMetas.get(0);
        // Ensure that children are loaded
        getChildren(monitor);
        // Add new child item
        DBNTreeItem childItem = new DBNTreeItem(this, childMeta, childObject, true);
        this.childNodes.add(childItem);

        return childItem;
    }

    public void removeChildItem(DBNNode item) throws DBException
    {
        if (!(item instanceof DBNTreeNode) || CommonUtils.isEmpty(childNodes) || !childNodes.contains(item)) {
            throw new DBException("Item '" + item.getNodeName() + "' do not belongs to node '" + getNodeName() + "' and can't be removed from it");
        }
        childNodes.remove(item);
        item.dispose(true);
    }

}
