/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSForeignKeyColumn;

/**
 * GenericConstraintColumn
 */
public class OracleForeignKeyColumn extends OracleConstraintColumn implements DBSForeignKeyColumn
{
    private OracleTableColumn referencedColumn;

    public OracleForeignKeyColumn(
        OracleForeignKey constraint,
        OracleTableColumn tableColumn,
        int ordinalPosition,
        OracleTableColumn referencedColumn)
    {
        super(constraint, tableColumn, ordinalPosition);
        this.referencedColumn = referencedColumn;
    }

    @Property(id = "reference", name = "Reference Column", viewable = true, order = 4)
    public OracleTableColumn getReferencedColumn()
    {
        return referencedColumn;
    }

}