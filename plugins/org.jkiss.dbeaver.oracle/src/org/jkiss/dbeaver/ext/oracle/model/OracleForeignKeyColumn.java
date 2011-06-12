/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSForeignKeyColumn;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

/**
 * GenericConstraintColumn
 */
public class OracleForeignKeyColumn extends OracleConstraintColumn implements DBSForeignKeyColumn
{

    public OracleForeignKeyColumn(
        OracleForeignKey constraint,
        OracleTableColumn tableColumn,
        int ordinalPosition)
    {
        super(constraint, tableColumn, ordinalPosition);
    }

    @Property(id = "reference", name = "Reference Column", viewable = true, order = 4)
    public OracleTableColumn getReferencedColumn()
    {
        return ((OracleForeignKey)getConstraint()).getReferencedKey().getColumns(VoidProgressMonitor.INSTANCE).get(getOrdinalPosition()).getTableColumn();
    }

}