/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeObject;
import org.jkiss.utils.CommonUtils;

/**
 * DBNDatabaseObject
 */
public class DBNDatabaseObject extends DBNDatabaseNode implements DBSObject
{
    private DBXTreeObject meta;

    DBNDatabaseObject(DBNNode parent, DBXTreeObject meta)
    {
        super(parent);
        this.meta = meta;
        registerNode();
    }

    @Override
    protected void dispose(boolean reflect)
    {
        unregisterNode(reflect);
        super.dispose(reflect);
    }

    @Override
    public DBXTreeObject getMeta()
    {
        return meta;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {
        // do nothing
    }

    @Override
    public DBSObject getObject()
    {
        return this;
    }

    @Override
    public Object getValueObject()
    {
        return this;
    }

    @Override
    public String getNodeFullName()
    {
        StringBuilder pathName = new StringBuilder();
        for (DBNNode parent = getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (parent instanceof DBNDatabaseFolder) {
                // skip folders
                continue;
            }
            String parentName = parent.getNodeName();
            if (!CommonUtils.isEmpty(parentName)) {
                if (pathName.length() > 0) {
                    pathName.insert(0, '.');
                }
                pathName.insert(0, parentName);
            }
        }
        pathName.insert(0, getNodeName() + " (");
        pathName.append(")");
        return pathName.toString();
    }


    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return meta.getNodeType(getDataSource());
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return meta.getDescription();
    }

    @Override
    public DBSObject getParentObject()
    {
        return getParentNode() instanceof DBNDatabaseNode ? ((DBSWrapper)getParentNode()).getObject() : null;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        DBSObject parentObject = getParentObject();
        return parentObject == null ? null : parentObject.getDataSource();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

}
