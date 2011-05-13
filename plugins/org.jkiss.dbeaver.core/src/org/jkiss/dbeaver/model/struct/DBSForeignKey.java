/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSForeignKey
 */
public interface DBSForeignKey extends DBSConstraint, DBSEntityAssociation
{
    DBSConstraint getReferencedKey();

    DBSConstraintModifyRule getDeleteRule();

    DBSConstraintModifyRule getUpdateRule();

    //Collection<? extends DBSForeignKeyColumn> getColumns(DBRProgressMonitor monitor);

    //DBSForeignKeyColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn);

}
