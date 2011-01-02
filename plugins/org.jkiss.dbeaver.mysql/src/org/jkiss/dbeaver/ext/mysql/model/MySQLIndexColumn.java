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

    public MySQLIndexColumn(MySQLIndex index, MySQLTableColumn tableColumn,
        int ordinalPosition,
        boolean ascending)
    {
        this.index = index;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
        this.ascending = ascending;
    }

    MySQLIndexColumn(MySQLIndex toIndex, MySQLIndexColumn source)
    {
        this.index = toIndex;
        this.tableColumn = source.tableColumn;
        this.ordinalPosition = source.ordinalPosition;
        this.ascending = source.ascending;
    }

    public MySQLIndex getIndex()
    {
        return index;
    }

    @Property(name = "Position", viewable = true, order = 1)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    @Property(name = "Ascending", viewable = true, order = 2)
    public boolean isAscending()
    {
        return ascending;
    }

    @Property(name = "Column", viewable = true, order = 3)
    public MySQLTableColumn getTableColumn()
    {
        return tableColumn;
    }

    public String getName()
    {
        return tableColumn.getName();
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
