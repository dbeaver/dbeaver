/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;

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
        registerNode();
    }

    @Override
    public boolean isDisposed()
    {
        return object == null || super.isDisposed();
    }

    @Override
    protected void dispose(boolean reflect)
    {
        unregisterNode(reflect);
        this.object = null;
        super.dispose(reflect);
    }

    @Override
    public DBXTreeItem getMeta()
    {
        return meta;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject newObject) {
        if (this.object == newObject) {
            return;
        }
        unregisterNode(false);
        this.object = newObject;
        registerNode();
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
