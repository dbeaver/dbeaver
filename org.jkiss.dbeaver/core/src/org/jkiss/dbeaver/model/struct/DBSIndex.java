/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSIndex
 */
public interface DBSIndex extends DBSEntity
{
    DBSTable getTable();

    boolean isUnique();

    DBSIndexType getIndexType();

    Collection<? extends DBSIndexColumn> getColumns(DBRProgressMonitor monitor);

}
