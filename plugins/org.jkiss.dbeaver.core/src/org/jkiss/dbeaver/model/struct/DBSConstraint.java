/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSConstraint
 */
public interface DBSConstraint extends DBSEntityQualified
{
    DBSTable getTable();

    DBSEntityConstraintType getConstraintType();

    Collection<? extends DBSConstraintColumn> getColumns(DBRProgressMonitor monitor);

    DBSConstraintColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn);

}
