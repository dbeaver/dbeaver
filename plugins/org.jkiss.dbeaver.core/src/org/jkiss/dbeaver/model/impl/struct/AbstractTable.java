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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;

/**
 * AbstractTable
 */
public abstract class AbstractTable<
    DATASOURCE extends DBPDataSource,
    CONTAINER extends DBSObjectContainer>
    implements DBSTable
{
    private CONTAINER container;
    private String tableName;

    protected AbstractTable(CONTAINER container)
    {
        this.container = container;
    }

    protected AbstractTable(CONTAINER container, String tableName)
    {
        this(container);
        this.tableName = tableName;
    }

    public CONTAINER getContainer()
    {
        return container;
    }

    @Override
    public DBSEntityType getEntityType()
    {
        return isView() ? DBSEntityType.VIEW : DBSEntityType.TABLE;
    }

    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return tableName;
    }

    public void setName(String tableName)
    {
        this.tableName = tableName;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DATASOURCE getDataSource()
    {
        return (DATASOURCE) container.getDataSource();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public DBSObject getParentObject()
    {
        return container;
    }

    public String toString()
    {
        return getFullQualifiedName();
    }

}
