/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraintColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * GenericTableConstraintColumn
 */
public class GenericTableConstraintColumn extends AbstractTableConstraintColumn
{
    private JDBCTableConstraint constraint;
    private GenericTableColumn tableColumn;
    private int ordinalPosition;

    public GenericTableConstraintColumn(JDBCTableConstraint constraint, GenericTableColumn tableColumn, int ordinalPosition)
    {
        this.constraint = constraint;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
    }

    public GenericTableConstraintColumn(GenericTableConstraint constraint, GenericTableConstraintColumn column)
    {
        this.constraint = constraint;
        this.tableColumn = column.tableColumn;
        this.ordinalPosition = column.ordinalPosition;
    }

    @Override
    public String getName()
    {
        return tableColumn.getName();
    }

    @Override
    @Property(id = "name", name = "Column", viewable = true, order = 1)
    public GenericTableColumn getAttribute()
    {
        return tableColumn;
    }

    @Override
    @Property(name = "Position", viewable = false, order = 2)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

//    @Property(name = "Description", viewable = true, order = 100)
    @Override
    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    @Override
    public JDBCTableConstraint getParentObject()
    {
        return constraint;
    }

    @Override
    public GenericDataSource getDataSource()
    {
        return (GenericDataSource) constraint.getDataSource();
    }

}
