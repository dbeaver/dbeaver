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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * GenericIndexColumn
 */
public class MySQLTableIndexColumn extends AbstractTableIndexColumn
{
    private MySQLTableIndex index;
    private MySQLTableColumn tableColumn;
    private int ordinalPosition;
    private boolean ascending;
    private boolean nullable;

    public MySQLTableIndexColumn(
        MySQLTableIndex index,
        MySQLTableColumn tableColumn,
        int ordinalPosition,
        boolean ascending,
        boolean nullable)
    {
        this.index = index;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
        this.ascending = ascending;
        this.nullable = nullable;
    }

    MySQLTableIndexColumn(MySQLTableIndex toIndex, MySQLTableIndexColumn source)
    {
        this.index = toIndex;
        this.tableColumn = source.tableColumn;
        this.ordinalPosition = source.ordinalPosition;
        this.ascending = source.ascending;
        this.nullable = source.nullable;
    }

    @Override
    public MySQLTableIndex getIndex()
    {
        return index;
    }

    //@Property(name = "Name", viewable = true, order = 1)
    @Override
    public String getName()
    {
        return tableColumn.getName();
    }

    @Override
    @Property(id = "name", viewable = true, order = 1)
    public MySQLTableColumn getTableColumn()
    {
        return tableColumn;
    }

    @Override
    @Property(viewable = false, order = 2)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    @Override
    @Property(viewable = true, order = 3)
    public boolean isAscending()
    {
        return ascending;
    }

    @Property(viewable = true, order = 4)
    public boolean isNullable()
    {
        return nullable;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    @Override
    public MySQLTableIndex getParentObject()
    {
        return index;
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource()
    {
        return index.getDataSource();
    }

}
