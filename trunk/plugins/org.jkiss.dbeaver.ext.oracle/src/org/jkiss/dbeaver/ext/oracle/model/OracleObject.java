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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract oracle object
 */
public abstract class OracleObject<PARENT extends DBSObject> implements DBSObject, DBPSaveableObject
{
    static final Log log = Log.getLog(OracleObject.class);


    protected final PARENT parent;
    protected String name;
    private boolean persisted;
    private long objectId;

    protected OracleObject(
        PARENT parent,
        String name,
        long objectId,
        boolean persisted)
    {
        this.parent = parent;
        this.name = name;
        this.objectId = objectId;
        this.persisted = persisted;
    }

    protected OracleObject(
        PARENT parent,
        String name,
        boolean persisted)
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
    public OracleDataSource getDataSource()
    {
        return (OracleDataSource) parent.getDataSource();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
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
