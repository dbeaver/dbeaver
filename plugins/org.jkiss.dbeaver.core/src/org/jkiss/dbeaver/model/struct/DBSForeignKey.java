/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSForeignKey
 */
public interface DBSForeignKey extends DBSConstraint, DBSEntityAssociation
{
    DBSConstraint getReferencedKey();

    DBSConstraintCascade getDeleteRule();

    DBSConstraintCascade getUpdateRule();

    DBSConstraintDefferability getDefferability();

    Collection<? extends DBSForeignKeyColumn> getColumns(DBRProgressMonitor monitor);

    DBSForeignKeyColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn);

}
