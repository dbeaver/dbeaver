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
import org.jkiss.utils.ArrayUtils;
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

    @Nullable
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
        DBNDatabaseNode[] children = getChildren(monitor);
        List<DBSObject> childObjects = new ArrayList<>();
        if (!ArrayUtils.isEmpty(children)) {
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
