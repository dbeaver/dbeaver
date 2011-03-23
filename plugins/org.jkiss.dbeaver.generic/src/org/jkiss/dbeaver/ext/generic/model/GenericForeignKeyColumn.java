/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSForeignKeyColumn;

/**
 * GenericConstraintColumn
 */
public class GenericForeignKeyColumn extends GenericConstraintColumn implements DBSForeignKeyColumn
{
    private GenericTableColumn referencedColumn;

    public GenericForeignKeyColumn(
        GenericForeignKey constraint,
        GenericTableColumn tableColumn,
        int ordinalPosition,
        GenericTableColumn referencedColumn)
    {
        super(constraint, tableColumn, ordinalPosition);
        this.referencedColumn = referencedColumn;
    }

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