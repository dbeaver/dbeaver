/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSIndex
 */
public interface DBSIndexColumn extends DBSStructureObject
{
    DBSIndex getIndex();

    int getOrdinalPosition();

    boolean isAscending();

    DBSTableColumn getTableColumn();

}