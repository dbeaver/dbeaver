/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSTableIndex
 */
public interface DBSTableIndex extends DBSObject, DBPQualifiedObject
{
    DBSTable getTable();

    boolean isUnique();

    DBSIndexType getIndexType();

    Collection<? extends DBSTableIndexColumn> getColumns(DBRProgressMonitor monitor);

}
