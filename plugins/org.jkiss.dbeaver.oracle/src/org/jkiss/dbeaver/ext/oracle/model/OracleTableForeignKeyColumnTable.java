/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSTableForeignKeyColumn;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

/**
 * GenericConstraintColumn
 */
public class OracleTableForeignKeyColumnTable extends OracleTableConstraintColumn implements DBSTableForeignKeyColumn
{

    public OracleTableForeignKeyColumnTable(
        OracleTableForeignKey constraint,
        OracleTableColumn tableColumn,
        int ordinalPosition)
    {
        super(constraint, tableColumn, ordinalPosition);
    }

    @Property(id = "reference", name = "Reference Column", viewable = true, order = 4)
    public OracleTableColumn getReferencedColumn()
    {
        return ((OracleTableForeignKey)getConstraint()).getReferencedConstraint().getColumns(VoidProgressMonitor.INSTANCE).get(getOrdinalPosition() - 1).getTableColumn();
    }

}