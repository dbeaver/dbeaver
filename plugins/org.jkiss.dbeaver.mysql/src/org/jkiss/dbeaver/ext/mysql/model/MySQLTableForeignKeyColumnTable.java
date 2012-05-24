/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSTableForeignKeyColumn;

/**
 * GenericConstraintColumn
 */
public class MySQLTableForeignKeyColumnTable extends MySQLTableConstraintColumn implements DBSTableForeignKeyColumn
{
    private MySQLTableColumn referencedColumn;

    public MySQLTableForeignKeyColumnTable(
        MySQLTableForeignKey constraint,
        MySQLTableColumn tableColumn,
        int ordinalPosition,
        MySQLTableColumn referencedColumn)
    {
        super(constraint, tableColumn, ordinalPosition);
        this.referencedColumn = referencedColumn;
    }

    @Override
    @Property(id = "reference", name = "Reference Column", viewable = true, order = 4)
    public MySQLTableColumn getReferencedColumn()
    {
        return referencedColumn;
    }

}