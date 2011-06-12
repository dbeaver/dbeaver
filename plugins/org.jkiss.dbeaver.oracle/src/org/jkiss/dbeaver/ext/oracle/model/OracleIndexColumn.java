/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.struct.AbstractIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * GenericIndexColumn
 */
public class OracleIndexColumn extends AbstractIndexColumn
{
    private OracleIndex index;
    private OracleTableColumn tableColumn;
    private int ordinalPosition;
    private boolean ascending;

    public OracleIndexColumn(
        OracleIndex index,
        OracleTableColumn tableColumn,
        int ordinalPosition,
        boolean ascending)
    {
        this.index = index;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
        this.ascending = ascending;
    }

    OracleIndexColumn(OracleIndex toIndex, OracleIndexColumn source)
    {
        this.index = toIndex;
        this.tableColumn = source.tableColumn;
        this.ordinalPosition = source.ordinalPosition;
        this.ascending = source.ascending;
    }

    public OracleIndex getIndex()
    {
        return index;
    }

    //@Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return tableColumn.getName();
    }

    @Property(id = "name", name = "Column", viewable = true, order = 1)
    public OracleTableColumn getTableColumn()
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

    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    public OracleIndex getParentObject()
    {
        return index;
    }

    public OracleDataSource getDataSource()
    {
        return index.getDataSource();
    }

}
