/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSTableForeignKeyColumn;

/**
 * GenericTableConstraintColumn
 */
public class GenericTableForeignKeyColumnTable extends GenericTableConstraintColumn implements DBSTableForeignKeyColumn
{
    private GenericTableColumn referencedColumn;

    public GenericTableForeignKeyColumnTable(
        GenericTableForeignKey constraint,
        GenericTableColumn tableColumn,
        int ordinalPosition,
        GenericTableColumn referencedColumn)
    {
        super(constraint, tableColumn, ordinalPosition);
        this.referencedColumn = referencedColumn;
    }

    @Override
    @Property(id = "reference", name = "Reference Column", viewable = true, order = 4)
    public GenericTableColumn getReferencedColumn()
    {
        return referencedColumn;
    }

    @Override
    public String toString()
    {
        return getName();
    }

}