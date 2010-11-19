/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

/**
 * Column order
 */
public class DBDColumnOrder {

    private final String columnName;
    private final int columnIndex;
    private boolean descending;

    public DBDColumnOrder(String columnName, int columnIndex, boolean descending)
    {
        this.columnName = columnName;
        this.columnIndex = columnIndex;
        this.descending = descending;
    }

    public DBDColumnOrder(DBDColumnOrder source)
    {
        this.columnName = source.columnName;
        this.columnIndex = source.columnIndex;
        this.descending = source.descending;
    }

    public String getColumnName()
    {
        return columnName;
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
