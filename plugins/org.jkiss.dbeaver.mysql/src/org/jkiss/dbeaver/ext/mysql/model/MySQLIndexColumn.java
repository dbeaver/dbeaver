/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.impl.struct.AbstractIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * GenericIndexColumn
 */
public class MySQLIndexColumn extends AbstractIndexColumn
{
    private MySQLIndex index;
    private MySQLTableColumn tableColumn;
    private int ordinalPosition;
    private boolean ascending;
    private boolean nullable;

    public MySQLIndexColumn(
        MySQLIndex index,
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

    MySQLIndexColumn(MySQLIndex toIndex, MySQLIndexColumn source)
    {
        this.index = toIndex;
        this.tableColumn = source.tableColumn;
        this.ordinalPosition = source.ordinalPosition;
        this.ascending = source.ascending;
        this.nullable = source.nullable;
    }

    public MySQLIndex getIndex()
    {
        return index;
    }

    //@Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return tableColumn.getName();
    }

    @Property(id = "name", name = "Column", viewable = true, order = 1)
    public MySQLTableColumn getTableColumn()
    {
        return tableColumn;
    }

    @Property(name = "Position", viewable = false, order = 2)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    @Property(name = "Ascending", viewable = true, order = 3)
    public boolean isAscending()
    {
        return ascending;
    }

    @Property(name = "Nullable", viewable = true, order = 4)
    public boolean isNullable()
    {
        return nullable;
    }

    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    public MySQLIndex getParentObject()
    {
        return index;
    }

    public MySQLDataSource getDataSource()
    {
        return index.getDataSource();
    }

}
