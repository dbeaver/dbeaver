/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.data.query;

import org.jkiss.utils.CommonUtils;

/**
 * Column filter
 */
public class DBQCondition {

    private final String columnName;
    //private DBQCriterion criterion;
    private String condition;
    private Object columnValue;

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

    public String getCondition()
    {
        return condition;
    }

    public void setCondition(String condition)
    {
        this.condition = condition;
    }

    public Object getColumnValue()
    {
        return columnValue;
    }

    public void setColumnValue(Object columnValue)
    {
        this.columnValue = columnValue;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DBQCondition)) {
            return false;
        }
        DBQCondition source = (DBQCondition)obj;
        return CommonUtils.equalObjects(this.columnName, source.columnName) &&
            CommonUtils.equalObjects(this.condition, source.condition) &&
            CommonUtils.equalObjects(this.columnValue, source.columnValue);
    }
}
