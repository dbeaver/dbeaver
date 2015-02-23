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

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeItem;

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

    @Override
    public String toString()
    {
        return object == null ? super.toString() : object.toString();
    }
}
