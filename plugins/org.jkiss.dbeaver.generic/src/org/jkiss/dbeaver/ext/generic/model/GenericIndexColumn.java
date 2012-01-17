/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.impl.struct.AbstractIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * GenericIndexColumn
 */
public class GenericIndexColumn extends AbstractIndexColumn
{
    private GenericIndex index;
    private GenericTableColumn tableColumn;
    private int ordinalPosition;
    private boolean ascending;

    public GenericIndexColumn(GenericIndex index, GenericTableColumn tableColumn,
        int ordinalPosition,
        boolean ascending)
    {
        this.index = index;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
        this.ascending = ascending;
    }

    GenericIndexColumn(GenericIndex toIndex, GenericIndexColumn source)
    {
        this.index = toIndex;
        this.tableColumn = source.tableColumn;
        this.ordinalPosition = source.ordinalPosition;
        this.ascending = source.ascending;
    }

    public GenericIndex getIndex()
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

    public GenericIndex getParentObject()
    {
        return index;
    }

    public GenericDataSource getDataSource()
    {
        return index.getDataSource();
    }

}
