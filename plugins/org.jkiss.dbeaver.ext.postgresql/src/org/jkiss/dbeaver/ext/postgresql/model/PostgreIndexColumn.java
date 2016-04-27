/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * GenericIndexColumn
 */
public class PostgreIndexColumn extends AbstractTableIndexColumn
{
    private PostgreIndex index;
    private PostgreAttribute tableColumn;
    private String expression;
    private int ordinalPosition;
    private boolean ascending;
    private boolean nullable;

    public PostgreIndexColumn(
        PostgreIndex index,
        PostgreAttribute tableColumn,
        String expression,
        int ordinalPosition,
        boolean ascending,
        boolean nullable)
    {
        this.index = index;
        this.tableColumn = tableColumn;
        this.expression = expression;
        this.ordinalPosition = ordinalPosition;
        this.ascending = ascending;
        this.nullable = nullable;
    }

    @NotNull
    @Override
    public PostgreIndex getIndex()
    {
        return index;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return tableColumn == null ? expression : tableColumn.getName();
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 1)
    public PostgreAttribute getTableColumn()
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
        return tableColumn == null ? null : tableColumn.getDescription();
    }

    @Override
    public PostgreIndex getParentObject()
    {
        return index;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return index.getDataSource();
    }

}
