/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data.query;

import org.jkiss.utils.CommonUtils;

/**
 * Column filter
 */
public class DBQCondition {

    private final String columnName;
    private DBQCriterion criterion;
    private String condition;

    public DBQCondition(String columnName, String condition)
    {
        this.columnName = columnName;
        this.condition = condition;
    }

    public DBQCondition(DBQCondition source)
    {
        this.columnName = source.columnName;
        this.condition = source.condition;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public DBQCriterion getCriterion()
    {
        return criterion;
    }

    public void setCriterion(DBQCriterion criterion)
    {
        this.criterion = criterion;
    }

    public String getCondition()
    {
        return condition;
    }

    public void setCondition(String condition)
    {
        this.condition = condition;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DBQCondition)) {
            return false;
        }
        DBQCondition source = (DBQCondition)obj;
        return CommonUtils.equalObjects(this.columnName, source.columnName) &&
            CommonUtils.equalObjects(this.condition, source.condition);
    }
}
