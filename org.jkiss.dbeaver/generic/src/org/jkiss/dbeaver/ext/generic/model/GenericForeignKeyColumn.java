/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.struct.DBSForeignKeyColumn;
import org.jkiss.dbeaver.model.meta.Property;

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

    @Property(name = "Reference Column", viewable = true, order = 3)
    public GenericTableColumn getReferencedColumn()
    {
        return referencedColumn;
    }
}