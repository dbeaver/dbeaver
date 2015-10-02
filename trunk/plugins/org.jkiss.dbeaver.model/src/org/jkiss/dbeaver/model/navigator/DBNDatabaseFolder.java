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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
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
        registerNode();
    }

    @Override
    protected void dispose(boolean reflect)
    {
        unregisterNode(reflect);
        super.dispose(reflect);
    }

    @Override
    public DBXTreeFolder getMeta()
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
        return getParentNode() instanceof DBNDatabaseNode ? ((DBNDatabaseNode)getParentNode()).getValueObject() : null;
    }

    @Override
    public String getChildrenType()
    {
        final List<DBXTreeNode> metaChildren = meta.getChildren(this);
        if (CommonUtils.isEmpty(metaChildren)) {
            return "?";
        } else {
            return metaChildren.get(0).getChildrenType(getDataSource());
        }
    }

    @NotNull
    @Override
    @Property(viewable = true)
    public String getName()
    {
        return meta.getChildrenType(getDataSource());
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
        return getParentNode() instanceof DBSWrapper ? ((DBSWrapper)getParentNode()).getObject() : null;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return getParentObject() == null ? null : getParentObject().getDataSource();
    }

    @Override
    public boolean isPersisted()
    {
        return getParentNode() != null && getParentNode().isPersisted();
    }

    @Override
    public Class<? extends DBSObject> getChildrenClass()
    {
        String itemsType = CommonUtils.toString(meta.getType());
        if (CommonUtils.isEmpty(itemsType)) {
            return null;
        }
        Class<DBSObject> aClass = meta.getSource().getObjectClass(itemsType, DBSObject.class);
        if (aClass == null) {
            log.error("Items class '" + itemsType + "' not found");
            return null;
        }
        if (!DBSObject.class.isAssignableFrom(aClass)) {
            log.error("Class '" + aClass.getName() + "' doesn't extend DBSObject");
            return null;
        }
        return aClass ;
    }

    @Override
    public Collection<DBSObject> getChildrenObjects(DBRProgressMonitor monitor) throws DBException
    {
        List<DBNDatabaseNode> children = getChildren(monitor);
        List<DBSObject> childObjects = new ArrayList<>();
        if (!CommonUtils.isEmpty(children)) {
            for (DBNDatabaseNode child : children) {
                childObjects.add(child.getObject());
            }
        }
        return childObjects;
    }

    @Override
    public String toString() {
        return meta.getChildrenType(getDataSource());
    }
}
