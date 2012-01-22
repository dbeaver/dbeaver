/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraintColumn;
import org.jkiss.dbeaver.model.meta.Property;

/**
 * GenericConstraintColumn
 */
public class OracleTableConstraintColumn extends AbstractTableConstraintColumn
{
    private AbstractTableConstraint<OracleTableBase> constraint;
    private OracleTableColumn tableColumn;
    private int ordinalPosition;

    public OracleTableConstraintColumn(AbstractTableConstraint<OracleTableBase> constraint, OracleTableColumn tableColumn, int ordinalPosition)
    {
        this.constraint = constraint;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
    }

    //@Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return tableColumn.getName();
    }

    @Property(id = "name", name = "Column", viewable = true, order = 1)
    public OracleTableColumn getAttribute()
    {
        return tableColumn;
    }

    @Property(name = "Position", viewable = false, order = 2)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    public AbstractTableConstraint<OracleTableBase> getParentObject()
    {
        return constraint;
    }

    public OracleDataSource getDataSource()
    {
        return constraint.getTable().getDataSource();
    }

}
