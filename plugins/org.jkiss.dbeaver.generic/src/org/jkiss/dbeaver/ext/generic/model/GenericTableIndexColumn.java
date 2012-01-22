/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * GenericTableIndexColumn
 */
public class GenericTableIndexColumn extends AbstractTableIndexColumn
{
    private GenericTableIndex index;
    private GenericTableColumn tableColumn;
    private int ordinalPosition;
    private boolean ascending;

    public GenericTableIndexColumn(GenericTableIndex index, GenericTableColumn tableColumn,
                                   int ordinalPosition,
                                   boolean ascending)
    {
        this.index = index;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
        this.ascending = ascending;
    }

    GenericTableIndexColumn(GenericTableIndex toIndex, GenericTableIndexColumn source)
    {
        this.index = toIndex;
        this.tableColumn = source.tableColumn;
        this.ordinalPosition = source.ordinalPosition;
        this.ascending = source.ascending;
    }

    public GenericTableIndex getIndex()
    {
        return index;
    }

    //@Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return tableColumn.getName();
    }

    @Property(id = "name", name = "Column", viewable = true, order = 1)
    public GenericTableColumn getTableColumn()
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

    public GenericTableIndex getParentObject()
    {
        return index;
    }

    public GenericDataSource getDataSource()
    {
        return index.getDataSource();
    }

}
