/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.impl.struct.AbstractConstraint;
import org.jkiss.dbeaver.model.impl.struct.AbstractConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * GenericConstraintColumn
 */
public class MySQLConstraintColumn extends AbstractConstraintColumn
{
    private AbstractConstraint<MySQLDataSource, MySQLTable> constraint;
    private MySQLTableColumn tableColumn;
    private int ordinalPosition;

    public MySQLConstraintColumn(AbstractConstraint<MySQLDataSource, MySQLTable> constraint, MySQLTableColumn tableColumn, int ordinalPosition)
    {
        this.constraint = constraint;
        this.tableColumn = tableColumn;
        this.ordinalPosition = ordinalPosition;
    }

    public AbstractConstraint<MySQLDataSource, MySQLTable> getConstraint()
    {
        return constraint;
    }

    @Property(name = "Column", viewable = true, order = 2)
    public MySQLTableColumn getTableColumn()
    {
        return tableColumn;
    }

    @Property(name = "Position", viewable = true, order = 1)
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    public String getName()
    {
        return tableColumn.getName();
    }

    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    public DBSObject getParentObject()
    {
        return constraint;
    }

    public MySQLDataSource getDataSource()
    {
        return constraint.getDataSource();
    }

}
