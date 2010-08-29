/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.impl.struct.AbstractIndexColumn;
import org.jkiss.dbeaver.model.anno.Property;

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
    public GenericTableColumn getTableColumn()
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

    public GenericIndex getParentObject()
    {
        return index;
    }

    public GenericDataSource getDataSource()
    {
        return index.getDataSource();
    }

}
