/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;

/**
 * AbstractProcedure
 */
public abstract class AbstractProcedure<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSObjectContainer>
    implements DBSProcedure, DBPSaveableObject
{
    protected CONTAINER container;
    protected String name;
    protected String description;
    protected boolean persisted;

    protected AbstractProcedure(CONTAINER container, boolean persisted)
    {
        this.container = container;
        this.persisted = persisted;
    }

    protected AbstractProcedure(CONTAINER container, boolean persisted, String name, String description)
    {
        this(container, persisted);
        this.name = name;
        this.description = description;
    }

    @Override
    public CONTAINER getContainer()
    {
        return container;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String tableName)
    {
        this.name = tableName;
    }

    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public DATASOURCE getDataSource()
    {
        return (DATASOURCE) container.getDataSource();
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

    @Override
    public DBSObject getParentObject()
    {
        return container;
    }

}
