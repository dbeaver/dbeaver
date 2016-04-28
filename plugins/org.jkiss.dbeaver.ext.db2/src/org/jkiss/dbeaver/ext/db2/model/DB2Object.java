/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract DB2 object
 */
public abstract class DB2Object<PARENT extends DBSObject> implements DBSObject, DBPSaveableObject {
    private static final Log log = Log.getLog(DB2Object.class);

    protected final PARENT parent;
    protected String name;
    private boolean persisted;
    private long objectId;

    protected DB2Object(PARENT parent, String name, long objectId, boolean persisted)
    {
        this.parent = parent;
        this.name = name;
        this.objectId = objectId;
        this.persisted = persisted;
    }

    protected DB2Object(PARENT parent, String name, boolean persisted)
    {
        this.parent = parent;
        this.name = name;
        this.persisted = persisted;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public PARENT getParentObject()
    {
        return parent;
    }

    @NotNull
    @Override
    public DB2DataSource getDataSource()
    {
        return (DB2DataSource) parent.getDataSource();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public long getObjectId()
    {
        return objectId;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }
}
