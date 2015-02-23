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
package org.jkiss.dbeaver.ext.nosql.cassandra.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * CassandraIndexColumn
 */
public class CassandraIndexColumn extends AbstractTableIndexColumn
{
    private CassandraIndex index;
    private CassandraColumn tableColumn;

    public CassandraIndexColumn(CassandraIndex index, CassandraColumn column)
    {
        this.index = index;
        this.tableColumn = column;
    }

    @Override
    public CassandraIndex getIndex()
    {
        return index;
    }

    @Override
    public String getName()
    {
        return tableColumn.getName();
    }

    @Override
    @Property(id = "name", viewable = true, order = 1)
    public CassandraColumn getTableColumn()
    {
        return tableColumn;
    }

    @Override
    public int getOrdinalPosition()
    {
        return 1;
    }

    @Override
    public boolean isAscending()
    {
        return true;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public CassandraIndex getParentObject()
    {
        return index;
    }

    @NotNull
    @Override
    public CassandraDataSource getDataSource()
    {
        return index.getDataSource();
    }

}
