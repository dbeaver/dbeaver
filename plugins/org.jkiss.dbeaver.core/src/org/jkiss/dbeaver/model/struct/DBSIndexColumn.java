/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSIndex
 */
public interface DBSIndexColumn extends DBSEntityAttribute
{
    DBSIndex getIndex();

    int getOrdinalPosition();

    boolean isAscending();

    DBSTableColumn getTableColumn();

}