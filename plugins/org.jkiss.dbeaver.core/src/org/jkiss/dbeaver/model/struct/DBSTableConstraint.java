/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSTableConstraint
 */
public interface DBSTableConstraint extends DBSEntityConstraint, DBPQualifiedObject
{
    DBSTable getTable();

    Collection<? extends DBSTableConstraintColumn> getColumns(DBRProgressMonitor monitor);

    DBSTableConstraintColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn);

}
