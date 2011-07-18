/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;
import org.jkiss.dbeaver.ui.ICommandIds;

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
        if (this.getModel() != null) {
            this.getModel().addNode(this, reflect);
        }
    }

    @Override
    public boolean isDisposed()
    {
        return object == null || super.isDisposed();
    }

    protected void dispose(boolean reflect)
    {
        if (this.getModel() != null) {
            // Notify model
            // Reflect changes only if underlying object is not persisted
            this.getModel().removeNode(this, reflect);
        }
        this.object = null;
        super.dispose(reflect);
    }

    public DBXTreeItem getMeta()
    {
        return meta;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {
        getModel().removeNode(this, false);
        this.object = object;
        getModel().addNode(this, false);
    }

    public DBSObject getObject()
    {
        return object;
    }

    public Object getValueObject()
    {
        return object;
    }

    @Override
    public boolean isPersisted()
    {
        return object != null && object.isPersisted();
    }

    public final boolean isManagable()
    {
        return true;
    }
}
