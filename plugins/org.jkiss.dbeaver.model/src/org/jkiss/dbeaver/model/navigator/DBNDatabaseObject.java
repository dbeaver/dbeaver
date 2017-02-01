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
