/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSIndex
 */
public interface DBSIndexColumn extends DBSObject
{
    DBSIndex getIndex();

    int getOrdinalPosition();

    boolean isAscending();

    DBSTableColumn getTableColumn();

}