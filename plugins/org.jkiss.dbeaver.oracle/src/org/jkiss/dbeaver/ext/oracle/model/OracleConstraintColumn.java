/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.impl.struct.AbstractConstraint;
import org.jkiss.dbeaver.model.impl.struct.AbstractConstraintColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * GenericConstraintColumn
 */
public class OracleConstraintColumn extends AbstractConstraintColumn
{
    private AbstractConstraint<OracleTable> constraint;
    private OracleTableColumn tableColumn;
    private int ordinalPosition;

    public OracleConstraintColumn(AbstractConstraint<OracleTable> constraint, OracleTableColumn tableColumn, int ordinalPosition)
    {
        this.constraint = constraint;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
    }

    public AbstractConstraint<OracleTable> getConstraint()
    {
        return constraint;
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

    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    public DBSObject getParentObject()
    {
        return constraint;
    }

    public OracleDataSource getDataSource()
    {
        return constraint.getTable().getDataSource();
    }

}
