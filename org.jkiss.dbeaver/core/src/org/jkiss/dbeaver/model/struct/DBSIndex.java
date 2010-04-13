/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import java.util.Collection;

/**
 * DBSIndex
 */
public interface DBSIndex extends DBSStructureObject
{
    DBSTable getTable();

    boolean isUnique();

    DBSIndexType getIndexType();

    Collection<? extends DBSIndexColumn> getColumns();

}
