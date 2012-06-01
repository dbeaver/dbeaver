/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.utils.CommonUtils;

/**
 * DBNDatabaseItem
 */
public class DBNDatabaseItem extends DBNDatabaseNode
{
    private DBXTreeItem meta;
    private DBSObject object;

    DBNDatabaseItem(DBNNode parent, DBXTreeItem meta, DBSObject object, boolean reflect)
    {
        super(parent);
        this.meta = meta;
        this.object = object;
        DBNModel.getInstance().addNode(this, reflect);
    }

    @Override
    public boolean isDisposed()
    {
        return object == null || super.isDisposed();
    }

    @Override
    protected void dispose(boolean reflect)
    {
        // Notify model
        // Reflect changes only if underlying object is not persisted
        DBNModel.getInstance().removeNode(this, reflect);
        this.object = null;
        super.dispose(reflect);
    }

    @Override
    public DBXTreeItem getMeta()
    {
        return meta;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {
        DBNModel.getInstance().removeNode(this, false);
        this.object = object;
        DBNModel.getInstance().addNode(this, false);
    }

    @Override
    public DBSObject getObject()
    {
        return object;
    }

    @Override
    public Object getValueObject()
    {
        getNodeItemPath();
        return object;
    }

    @Override
    public boolean isPersisted()
    {
        return object != null && object.isPersisted();
    }

    @Override
    public final boolean isManagable()
    {
        return true;
    }

}
