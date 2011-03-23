/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.impl.struct.AbstractConstraintColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * GenericConstraintColumn
 */
public class GenericConstraintColumn extends AbstractConstraintColumn
{
    private GenericConstraint constraint;
    private GenericTableColumn tableColumn;
    private int ordinalPosition;

    public GenericConstraintColumn(GenericConstraint constraint, GenericTableColumn tableColumn, int ordinalPosition)
    {
        this.constraint = constraint;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
    }

    public GenericConstraintColumn(GenericConstraint constraint, GenericConstraintColumn column)
    {
        this.constraint = constraint;
        this.tableColumn = column.tableColumn;
        this.ordinalPosition = column.ordinalPosition;
    }

    public DBSConstraint getConstraint()
    {
        return constraint;
    }

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

    @Property(name = "Description", viewable = true, order = 100)
    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    public DBSObject getParentObject()
    {
        return constraint;
    }

    public GenericDataSource getDataSource()
    {
        return constraint.getDataSource();
    }

}
