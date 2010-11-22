/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.data.criteria.DBDCriterion;

/**
 * Column filter
 */
public class DBDColumnFilter {

    private final String columnName;
    private final int columnIndex;
    private DBDCriterion criterion;
    private String where;

    public DBDColumnFilter(String columnName, int columnIndex, String where)
    {
        this.columnName = columnName;
        this.columnIndex = columnIndex;
        this.where = where;
    }

    public DBDColumnFilter(DBDColumnFilter source)
    {
        this.columnName = source.columnName;
        this.columnIndex = source.columnIndex;
        this.where = source.where;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public int getColumnIndex()
    {
        return columnIndex;
    }

    public DBDCriterion getCriterion()
    {
        return criterion;
    }

    public void setCriterion(DBDCriterion criterion)
    {
        this.criterion = criterion;
    }

    public String getWhere()
    {
        return where;
    }

    public void setWhere(String where)
    {
        this.where = where;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DBDColumnFilter)) {
            return false;
        }
        DBDColumnFilter source = (DBDColumnFilter)obj;
        return CommonUtils.equalObjects(this.columnName, source.columnName) &&
            this.columnIndex == source.columnIndex &&
            CommonUtils.equalObjects(this.where, source.where);
    }
}
