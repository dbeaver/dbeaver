/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import net.sf.jkiss.utils.BeanUtils;
import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.tree.DBXTreeFolder;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.ICommandIds;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * DBNDatabaseFolder
 */
public class DBNDatabaseFolder extends DBNDatabaseNode implements DBNContainer, DBSFolder
{
    private DBXTreeFolder meta;

    DBNDatabaseFolder(DBNNode parent, DBXTreeFolder meta)
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
        return getParentNode() instanceof DBNDatabaseNode ? ((DBNDatabaseNode)getParentNode()).getValueObject() : null;
    }

    public String getItemsLabel()
    {
        if (CommonUtils.isEmpty(meta.getChildren())) {
            return "?";
        } else {
            return meta.getChildren().get(0).getLabel();
        }
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
        return getParentNode() instanceof DBSWrapper ? ((DBSWrapper)getParentNode()).getObject() : null;
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

    public DBNDatabaseItem addChildItem(DBRProgressMonitor monitor, Object childObject) throws DBException
    {
        List<DBXTreeNode> childMetas = getMeta().getChildren();
        if (childMetas.size() != 1 || !(childMetas.get(0) instanceof DBXTreeItem)) {
            throw new DBException("It's not allowed to add child items to node '" + getNodeName() + "'");
        }
        if (!(childObject instanceof DBSObject)) {
            throw new DBException("Only database structure objects could be added to database folder");
        }
        DBXTreeItem childMeta = (DBXTreeItem)childMetas.get(0);
        // Ensure that children are loaded
        getChildren(monitor);
        // Add new child item
        DBNDatabaseItem childItem = new DBNDatabaseItem(this, childMeta, (DBSObject)childObject, true);
        this.childNodes.add(childItem);

        return childItem;
    }

    public void removeChildItem(DBNNode item) throws DBException
    {
        if (!(item instanceof DBNDatabaseNode) || CommonUtils.isEmpty(childNodes) || !childNodes.contains(item)) {
            throw new DBException("Item '" + item.getNodeName() + "' do not belongs to node '" + getNodeName() + "' and can't be removed from it");
        }
        childNodes.remove(item);
        item.dispose(true);
    }

}
