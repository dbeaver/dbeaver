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
