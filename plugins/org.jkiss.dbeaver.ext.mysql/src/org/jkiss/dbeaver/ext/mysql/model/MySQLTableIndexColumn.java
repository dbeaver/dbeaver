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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;

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
    private String subPart;

    public MySQLTableIndexColumn(
        MySQLTableIndex index,
        MySQLTableColumn tableColumn,
        int ordinalPosition,
        boolean ascending,
        boolean nullable,
        String subPart)
    {
        this.index = index;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
        this.ascending = ascending;
        this.nullable = nullable;
        this.subPart = subPart;
    }

    MySQLTableIndexColumn(DBRProgressMonitor monitor, MySQLTableIndex toIndex, DBSTableIndexColumn source) throws DBException {
        this.index = toIndex;
        if (source.getTableColumn() != null) {
            this.tableColumn = toIndex.getTable().getAttribute(monitor, source.getTableColumn().getName());
        }
        this.ordinalPosition = source.getOrdinalPosition();
        this.ascending = source.isAscending();
        if (source instanceof MySQLTableIndexColumn) {
            this.nullable = ((MySQLTableIndexColumn)source).nullable;
            this.subPart = ((MySQLTableIndexColumn)source).subPart;
        }
    }

    @NotNull
    @Override
    public MySQLTableIndex getIndex()
    {
        return index;
    }

    //@Property(name = "Name", viewable = true, order = 1)
    @NotNull
    @Override
    public String getName()
    {
        return tableColumn.getName();
    }

    @Nullable
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

    @Property(viewable = true, order = 5)
    public String getSubPart() {
        return subPart;
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
