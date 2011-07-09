/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data.query;

import org.jkiss.utils.CommonUtils;

/**
 * Column order
 */
public class DBQOrderColumn {

    private final String columnName;
    private boolean descending;

    public DBQOrderColumn(String columnName, boolean descending)
    {
        this.columnName = columnName;
        this.descending = descending;
    }

    public DBQOrderColumn(DBQOrderColumn source)
    {
        this.columnName = source.columnName;
        this.descending = source.descending;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public boolean isDescending()
    {
        return descending;
    }

    public void setDescending(boolean descending)
    {
        this.descending = descending;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DBQOrderColumn)) {
            return false;
        }
        DBQOrderColumn source = (DBQOrderColumn)obj;
        return CommonUtils.equalObjects(this.columnName, source.columnName) &&
            this.descending == source.descending;
    }
}
