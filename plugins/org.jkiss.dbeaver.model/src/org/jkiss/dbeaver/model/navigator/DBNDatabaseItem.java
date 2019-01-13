/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
