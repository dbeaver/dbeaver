/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;

/**
 * Column order
 */
public class DBDColumnOrder {

    private final DBCColumnMetaData columnMetaData;
    private final int columnIndex;
    private boolean descending;

    public DBDColumnOrder(DBCColumnMetaData columnMetaData, int columnIndex, boolean descending)
    {
        this.columnMetaData = columnMetaData;
        this.columnIndex = columnIndex;
        this.descending = descending;
    }

    public DBCColumnMetaData getColumnMetaData()
    {
        return columnMetaData;
    }

    public int getColumnIndex()
    {
        return columnIndex;
    }

    public boolean isDescending()
    {
        return descending;
    }

    public void setDescending(boolean descending)
    {
        this.descending = descending;
    }

}
