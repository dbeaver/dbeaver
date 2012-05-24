/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSTableForeignKey
 */
public interface DBSTableForeignKey extends DBSTableConstraint, DBSEntityAssociation
{
    @Override
    DBSTableConstraint getReferencedConstraint();

    DBSConstraintModifyRule getDeleteRule();

    DBSConstraintModifyRule getUpdateRule();

    //Collection<? extends DBSTableForeignKeyColumn> getColumns(DBRProgressMonitor monitor);

    //DBSTableForeignKeyColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn);

}
